"""Auditable research-agent pipeline: code computes, an LLM may narrate.

Market/news acquisition and indicators stay deterministic and provenance
tracked.  A configured gateway model performs the final synthesis; when no
provider is available the same response shape is returned with an explicit
``deterministic_fallback`` mode instead of pretending that rules are an LLM.
"""
from __future__ import annotations

import asyncio
import json
import re
import time
import uuid
from collections.abc import AsyncIterator
from typing import Any

from .tools import execute_tool_async
from .ta_agents import run_debate
from .gateway import gateway
from . import store
from .insight import build_insight

_CHAT_SYSTEM_PROMPT = (
    "你是知牛（ZhiNiu）的股票研究助手，面向 A 股个人投资者，用中文回答。规则：\n"
    "1. 概念/方法类问题（如指标含义、术语、分析方法）给出专业、简洁、结构化的解释，可用短列表。\n"
    "2. 严禁编造具体价格、财报数字或新闻；若上下文提供了标的快照，数字只能引用该快照。\n"
    "3. 涉及实时行情的问题，提醒用户在行情页查看最新数据。\n"
    "4. 不构成投资建议，涉及决策时提示风险。\n"
    "5. 回答不超过 300 字。\n"
    "安全约束：提到的证券代码必须来自本轮工具返回结果，禁止编造、猜测或引用旧对话代码；"
    "行情/财务/K线数值只能来自工具返回数据，禁止自行填写或修改任何数字；"
    "工具失败时明确说明失败项，禁止用未经成功查询的数据补全。"
)

# ---------- AI 工具指令协议（AI 操作 App：加自选 / 拉起对比 / 设价格预警） ----------
# 模型在回复末尾以独立行输出 ⟦TOOL⟧{json} 指令（对标课题组 KuiklyStock ⟦TOOL⟧ 协议）；
# 服务端解析、白名单校验并做「名称→代码必须搜索解析」防幻觉，客户端执行。
_CHAT_TOOLS_PROMPT = (
    "\n6. 工具指令（App 操作）：用户明确要求「加自选 / 对比 / 设预警」时，在回复最后另起一行输出指令，"
    "格式固定（一行一条，不要放进正文，不要输出代码块围栏）：\n"
    "⟦TOOL⟧{\"name\":\"add_watchlist\",\"args\":{\"symbol\":\"sz300750\"}}\n"
    "可用指令：\n"
    "- add_watchlist：加入自选，args={\"symbol\":\"sz300750\"}（代码必须来自上下文快照；不确定代码时用 {\"name\":\"宁德时代\"} 交由服务端搜索解析）\n"
    "- open_compare：拉起双股对比页，args={\"symbol_a\":\"sh600519\",\"symbol_b\":\"sz300750\"}\n"
    "- set_price_alert：价格预警，args={\"symbol\":\"sz300750\",\"operator\":\"above\"|\"below\",\"price\":320.5}（price 必须是具体数字；operator above=突破上方价、below=跌破下方价）\n"
    "- research_stock：转入个股多 Agent 研究（行情/技术面/财务/资讯/多空/风控），args={\"symbol\":\"sz300750\"}（不确定代码时用 {\"name\":\"隆基绿能\"} 由服务端搜索解析）。当用户就某只具体股票寻求分析/行情/趋势/买卖参考，而上下文快照中没有该股数据时，必须输出此指令让 App 取真实数据，而不是凭记忆作答。\n"
    "- set_appearance：切换 App 外观，args={\"mode\":\"light\"|\"dark\"|\"system\"}（light=浅色、dark=深色/夜间、system=跟随系统；用户说「换成深色/夜间模式」时输出）\n"
    "- set_color_mode：切换涨跌配色，args={\"mode\":\"red_up\"|\"green_up\"}（red_up=红涨绿跌·A 股习惯；green_up=绿涨红跌·海外习惯；用户说「换成绿涨红跌/欧美配色」时输出）\n"
    "仅当用户明确表达操作意图时输出一条指令，并用一句话在正文说明已为其执行；不明确时不要输出指令。\n"
    "7. 结论徽章（可选）：当且仅当本次回答给出了明确的风险/操作倾向时，在回答最末尾另起一行、严格按格式输出：\n"
    "【AI观点】风险：低｜操作建议：观望\n"
    "（风险只能取 低/中/高；操作建议只能取 买入/持有/卖出/观望；科普、闲聊、纯概念解释类回答不要输出这一行。）\n"
    "8. 走势卡片（可选）：需要展示某只股票近期走势时，在回答最末尾另起一行输出 [KCHART:sh600519]（代码必须来自本轮工具结果或上下文快照，最多 1 个；App 会渲染真实日 K 卡；闲聊不要输出）。"
)

_TOOL_MARKERS = ("⟦TOOL⟧", "【TOOL】", "[TOOL]", "「TOOL」")
# 流式阶段一并抑制的正文标记：工具指令 + [KCHART:] 走势卡（协议约定置于回复末尾）
_STREAM_MARKERS = _TOOL_MARKERS + ("[KCHART:", "【KCHART】")
_TOOL_NAME_ALIASES = {
    "add_watchlist": "add_watchlist", "addwatchlist": "add_watchlist", "add_to_watchlist": "add_watchlist",
    "watch": "add_watchlist", "add_favorite": "add_watchlist", "add_favourite": "add_watchlist",
    "open_compare": "open_compare", "compare": "open_compare", "compare_stocks": "open_compare",
    "open_stock_compare": "open_compare", "stock_compare": "open_compare",
    "set_price_alert": "set_price_alert", "price_alert": "set_price_alert", "set_alert": "set_price_alert",
    "alert": "set_price_alert", "add_alert": "set_price_alert", "set_price_warning": "set_price_alert",
    "research_stock": "research_stock", "research": "research_stock", "analyze_stock": "research_stock",
    "analyse_stock": "research_stock", "stock_research": "research_stock", "deep_research": "research_stock",
    "set_appearance": "set_appearance", "set_theme": "set_appearance", "theme_mode": "set_appearance",
    "set_theme_mode": "set_appearance", "set_mode": "set_appearance", "appearance": "set_appearance",
    "set_dark_mode": "set_appearance", "dark_mode": "set_appearance", "switch_theme": "set_appearance",
    "set_color_mode": "set_color_mode", "setcolormode": "set_color_mode", "color_mode": "set_color_mode",
    "set_updown": "set_color_mode", "updown_color": "set_color_mode", "up_down_mode": "set_color_mode",
    "change_color_mode": "set_color_mode", "switch_color_mode": "set_color_mode",
}
_SYMBOL_RE = re.compile(r"^(sh|sz|bj)\d{6}$", re.IGNORECASE)


def _parse_tool_json(payload: str) -> dict | None:
    """宽容解析指令 JSON：剥代码围栏/首尾杂字符；失败时用正则兜底提 name 与顶层字段。"""
    text = payload.strip().strip("`").strip()
    start, end = text.find("{"), text.rfind("}")
    if start >= 0 and end > start:
        try:
            parsed = json.loads(text[start : end + 1])
            if isinstance(parsed, dict):
                return parsed
        except Exception:
            pass
    name_match = re.search(r'["\']?name["\']?\s*[:：]\s*["\']([\w-]+)["\']', text)
    if not name_match:
        return None
    return {"name": name_match.group(1), "_raw": text}


def _extract_tool_directives(content: str) -> tuple[str, list[dict]]:
    """从模型回复中拆出指令行：返回 (无指令正文, [原始指令 dict])。

    兼容多种标记（⟦TOOL⟧/【TOOL】/[TOOL]）与围栏；指令行不进正文展示。
    """
    text_lines: list[str] = []
    directives: list[dict] = []
    for line in content.splitlines():
        stripped = line.strip()
        lowered = stripped.lower().replace(" ", "")
        hit = next((m for m in _TOOL_MARKERS if m.lower().replace(" ", "") in lowered), None)
        if hit is None:
            text_lines.append(line)
            continue
        payload = stripped[stripped.find(hit) + len(hit):].strip()
        parsed = _parse_tool_json(payload)
        if parsed is not None:
            directives.append(parsed)
    return "\n".join(text_lines).strip(), directives


async def _resolve_symbol_arg(arg: dict, keys: tuple[str, ...], context_symbol: str) -> tuple[str, str] | None:
    """把 args 中的标的解析为 (symbol, name)。防幻觉：名称必须经 quote_search 真实解析；
    直接代码必须符合 sh/sz/bj+6 位格式（提示词约束其来自上下文快照）。"""
    from .quote import quote_search

    for key in keys:
        value = str(arg.get(key) or "").strip()
        if not value:
            continue
        if _SYMBOL_RE.match(value):
            return value.lower(), str(arg.get("name") or value)
        if len(value) >= 2 and not value.isdigit():
            result = await quote_search(value, 1)
            items = result.get("items") or []
            if items:
                first = items[0]
                return first["symbol"], first["name"]
        return None
    if context_symbol and _SYMBOL_RE.match(context_symbol):
        return context_symbol.lower(), ""
    return None


async def _validate_tool_directive(
    raw: dict, context_symbol: str
) -> tuple[dict | None, str]:
    """白名单 + 参数校验 + 名称搜索解析。返回 (合法指令|None, 追加到正文的人话说明)。"""
    name = str(raw.get("name") or "").strip()
    canonical = _TOOL_NAME_ALIASES.get(name.lower().replace("-", "_"))
    if canonical is None:
        return None, ""
    args = raw.get("args") if isinstance(raw.get("args"), dict) else raw
    if canonical == "add_watchlist":
        resolved = await _resolve_symbol_arg(args, ("symbol", "code", "stock", "name"), context_symbol)
        if resolved is None:
            return None, "（未能解析标的代码，本次未执行加自选；可在个股详情页手动添加）"
        symbol, stock_name = resolved
        return (
            {"name": "add_watchlist", "args": {"symbol": symbol, "name": stock_name}, "display": f"加入自选 {stock_name or symbol}"},
            "",
        )
    if canonical == "research_stock":
        resolved = await _resolve_symbol_arg(args, ("symbol", "code", "stock", "name"), context_symbol)
        if resolved is None:
            return None, "（未能解析研究标的，本次未转入研究；可在行情页搜索该股后进入详情）"
        symbol, stock_name = resolved
        return (
            {"name": "research_stock", "args": {"symbol": symbol, "name": stock_name}, "display": f"转入个股研究 {stock_name or symbol}"},
            "",
        )
    if canonical == "open_compare":
        first = await _resolve_symbol_arg(args, ("symbol_a", "symbolA", "a", "symbol"), "")
        second = await _resolve_symbol_arg(args, ("symbol_b", "symbolB", "b"), "")
        if first is None or second is None or first[0] == second[0]:
            return None, "（对比标的不完整或重复，本次未打开对比页；可指明两只股票后重试）"
        return (
            {
                "name": "open_compare",
                "args": {"symbolA": first[0], "symbolB": second[0], "nameA": first[1], "nameB": second[1]},
                "display": f"打开对比 {first[1] or first[0]} × {second[1] or second[0]}",
            },
            "",
        )
    if canonical == "set_price_alert":
        resolved = await _resolve_symbol_arg(args, ("symbol", "code", "stock", "name"), context_symbol)
        if resolved is None:
            return None, "（未能解析预警标的，本次未设置）"
        operator = str(args.get("operator") or args.get("direction") or "").strip().lower()
        if operator in ("up", "above", "超过", "突破", "上方", "高于", ">", ">=", "cross_above"):
            operator = "above"
        elif operator in ("down", "below", "跌破", "下方", "低于", "<", "<=", "cross_below"):
            operator = "below"
        else:
            return None, "（预警方向不明确（需 above/below），本次未设置）"
        price = _num_or_none(args.get("price") or args.get("target") or args.get("value"))
        if price is None or price <= 0:
            return None, "（预警价格缺失或非数字，本次未设置；请给出具体价格，如「跌破 300 设预警」）"
        symbol, stock_name = resolved
        return (
            {
                "name": "set_price_alert",
                "args": {"symbol": symbol, "name": stock_name, "operator": operator, "price": price},
                "display": f"设置预警 {stock_name or symbol} {'突破' if operator == 'above' else '跌破'} {price:g}",
            },
            "",
        )
    if canonical == "set_appearance":
        mode = _norm_appearance(args.get("mode") or args.get("theme") or args.get("value"))
        if mode is None:
            return None, "（外观取值不明确（浅色/深色/跟随系统），本次未切换）"
        return (
            {"name": "set_appearance", "args": {"mode": mode}, "display": f"外观切换为{_APPEARANCE_LABELS[mode]}"},
            "",
        )
    if canonical == "set_color_mode":
        mode = _norm_color_mode(args.get("mode") or args.get("colorMode") or args.get("value"))
        if mode is None:
            return None, "（涨跌配色取值不明确（红涨绿跌/绿涨红跌），本次未切换）"
        label = "红涨绿跌（A 股）" if mode == "red_up" else "绿涨红跌（海外）"
        return (
            {"name": "set_color_mode", "args": {"mode": mode}, "display": f"涨跌配色切换为{label}"},
            "",
        )
    return None, ""


async def _process_chat_tools(content: str, context_symbol: str = "") -> tuple[str, list[dict]]:
    """拆指令 → 校验解析 → (干净正文, 合法指令列表)；失败说明追加入正文（诚实降级）。"""
    text, raw_directives = _extract_tool_directives(content)
    tools: list[dict] = []
    notes: list[str] = []
    for raw in raw_directives[:3]:
        tool, note = await _validate_tool_directive(raw, context_symbol)
        if tool is not None:
            tools.append(tool)
        elif note:
            notes.append(note)
    if notes:
        text = (text + "\n" + "\n".join(notes)).strip()
    return text, tools


_TOOL_NUDGE = (
    "你说明了操作，但没有按协议输出指令行。请只补一行 ⟦TOOL⟧{json} 指令"
    "（name/args 按协议；不确定代码就用 name），不要输出其他文字。"
)


def _looks_like_tool_intent(question: str) -> bool:
    """服务端操作意图识别（与前端路由同一套口径；用于「无指令强重试」判定）。"""
    watchlist_intent = "自选" in question and any(w in question for w in ("加", "收藏", "添加"))
    alert_intent = any(w in question for w in ("预警", "提醒我", "报警"))
    compare_intent = any(w in question for w in ("对比", "比较")) and any(
        w in question.lower() for w in ("和", "与", "vs", "×")
    )
    appearance_intent = any(w in question for w in ("深色", "浅色", "夜间", "暗黑", "亮色", "主题")) and any(
        w in question for w in ("切", "换", "改", "调", "设")
    )
    colormode_intent = any(w in question for w in ("红涨绿跌", "绿涨红跌", "涨跌配色", "配色")) and any(
        w in question for w in ("切", "换", "改", "调", "设")
    )
    return watchlist_intent or alert_intent or compare_intent or appearance_intent or colormode_intent


async def _chat_once(messages: list[dict]) -> tuple[str, str, str]:
    """单次非流式调用；返回 (content, provider, model)。全失败返回空串。"""
    response: dict | None = None
    try:
        async for chunk in gateway.chat_completions(
            model="zhiniu/quick", messages=messages, stream=False,
            temperature=0.4, max_tokens=600,
        ):
            response = chunk
    except Exception:  # noqa: BLE001
        return "", "", ""
    if not response or response.get("id") == "mock-llm":
        return "", "", ""
    content = ((response.get("choices") or [{}])[0].get("message") or {}).get("content", "")
    return content.strip(), response.get("provider", ""), response.get("model", "")

# 个股诊股（/agent/insight）独立缓存键：与用户研究 keyword 空间隔离，复用 store 的缓存/并发去重。
_INSIGHT_CACHE_KEY = "insight:v1"

_INSIGHT_SYSTEM_PROMPT = (
    "你是知牛（ZhiNiu）的个股诊股分析师。输入为服务端已获取的真实证据（行情/技术面/估值财报/关键位），"
    "你只做归纳判断，输出严格 JSON（不要输出 JSON 以外的任何文字）。规则：\n"
    "1. verdict 只能取 偏强 / 中性 / 偏弱 三值之一。\n"
    "2. summary 不超过 120 字，数字只能引用输入证据。\n"
    "3. trend/valuation/earnings/volume/risk 各不超过 80 字；估值与业绩在证据缺失时写「证据不足，暂不判断」。\n"
    "4. signals 给 3~5 条，每条必须引用输入中的具体数值（如 RSI、PE、涨跌幅、支撑/压力）。\n"
    "5. buyZone/sellZone 为 [低, 高] 两个数字，边界只能取输入证据给出的支撑/压力/现价附近的值，"
    "禁止自造价格；判断不了就填 null。\n"
    "6. riskLevel 只能取 low/medium_low/medium/medium_high/high 五值之一。\n"
    "7. followUps 给 4 条用户可能想追问的问题。\n"
    "输出字段：{\"verdict\",\"summary\",\"trend\",\"valuation\",\"earnings\",\"volume\",\"risk\","
    "\"signals\":[],\"buyZone\":[lo,hi]|null,\"sellZone\":[lo,hi]|null,\"riskLevel\",\"followUps\":[]}"
)

_INSIGHT_VERDICTS = ("偏强", "中性", "偏弱")
_INSIGHT_RISK_LEVELS = ("low", "medium_low", "medium", "medium_high", "high")


def _technical_summary(kline: dict) -> dict:
    bars = kline.get("data") or []
    closes = [float(bar.get("close", 0)) for bar in bars if float(bar.get("close", 0)) > 0]
    if len(closes) < 2:
        return {"direction": "insufficient", "changePct": 0.0, "sampleSize": len(closes), "rsi14": 0.0, "ma20": 0.0}
    first, last = closes[0], closes[-1]
    change = (last - first) / first * 100 if first else 0.0
    direction = "up" if change > 0.5 else "down" if change < -0.5 else "sideways"
    window = closes[-15:]
    gains = [max(window[i] - window[i - 1], 0.0) for i in range(1, len(window))]
    losses = [max(window[i - 1] - window[i], 0.0) for i in range(1, len(window))]
    avg_gain = sum(gains) / len(gains) if gains else 0.0
    avg_loss = sum(losses) / len(losses) if losses else 0.0
    rsi = 100.0 if avg_loss == 0 and avg_gain > 0 else 50.0 if avg_loss == 0 else 100.0 - 100.0 / (1.0 + avg_gain / avg_loss)
    ma_window = closes[-20:]
    return {
        "direction": direction,
        "changePct": round(change, 2),
        "sampleSize": len(closes),
        "latest": round(last, 3),
        "ma20": round(sum(ma_window) / len(ma_window), 3),
        "rsi14": round(rsi, 1),
    }


def _risk_flags(quote: dict, technical: dict, news: dict, financials: dict) -> list[str]:
    flags: list[str] = []
    if quote.get("isStale"):
        flags.append("行情命中陈旧缓存")
    if news.get("isStale"):
        flags.append("资讯为陈旧缓存或离线快照")
    if not financials.get("available"):
        flags.append("财务与估值数据暂不可用")
    elif financials.get("isStale"):
        flags.append("财务与估值数据来自陈旧缓存")
    if technical.get("sampleSize", 0) < 20:
        flags.append("K 线样本不足 20 根")
    if abs(float(technical.get("changePct", 0))) >= 10:
        flags.append("区间价格波动较大")
    return flags


def _fallback_synthesis(technical: dict, risks: list[str], news: dict) -> dict:
    direction = technical.get("direction", "insufficient")
    stance = {"up": "偏强", "down": "偏弱", "sideways": "震荡"}.get(direction, "数据不足")
    risk_text = "；".join(risks) if risks else "未触发规则风险项，仍需关注波动与数据时效"
    return {
        "mode": "deterministic_fallback",
        "provider": "rule-engine",
        "model": "none",
        "stance": stance,
        "confidence": 0.45 if direction == "insufficient" else 0.62,
        "summary": f"技术结构{stance}，区间涨跌 {technical.get('changePct', 0):.2f}%，RSI(14) {technical.get('rsi14', 0):.1f}。",
        "catalysts": [f"已检索 {int(news.get('total', 0))} 条相关资讯"],
        "risks": risks or [risk_text],
    }


def _levels_from_kline(kline: dict) -> dict:
    """确定性压力/支撑：近 60 根 K 线区间高低点（不依赖模型，杜绝编造价位）。"""
    bars = (kline.get("data") or [])[-60:]
    highs = [float(b.get("high", 0)) for b in bars if float(b.get("high", 0)) > 0]
    lows = [float(b.get("low", 0)) for b in bars if float(b.get("low", 0)) > 0]
    if not highs or not lows:
        return {}
    return {
        "pressure": round(max(highs), 3),
        "support": round(min(lows), 3),
        "window": len(bars),
    }


async def _synthesize(
    symbol: str,
    keyword: str,
    quote: dict,
    technical: dict,
    financials: dict,
    news: dict,
    risks: list[str],
    kline: dict,
    progress=None,
) -> dict:
    """TradingAgents 结构辩论归纳；无模型/超时在 ta_agents 内显式降级 rule-engine。"""
    evidence = {
        "symbol": symbol,
        "keyword": keyword,
        "quote": {k: quote.get(k) for k in ("name", "price", "prevClose", "date", "time", "source", "isStale")},
        "technical": technical,
        "financials": financials,
        "news": news,
        "riskFlags": risks,
        "levels": _levels_from_kline(kline),
    }
    return await run_debate(evidence, progress)


def _build_report(
    symbol: str,
    quote: dict,
    kline: dict,
    financials: dict,
    news: dict,
    synthesis: dict,
    started: float,
) -> dict:
    technical = _technical_summary(kline)
    risks = _risk_flags(quote, technical, news, financials)
    llm_mode = synthesis.get("mode") == "llm"
    debate_source = synthesis.get("provider", "rule-engine")
    debate_stages: list[dict[str, Any]] = (
        [
            {"id": "bull", "label": "多头研究员", "status": "done", "source": debate_source},
            {"id": "bear", "label": "空头研究员", "status": "done", "source": debate_source},
            {"id": "manager", "label": "研究经理", "status": "done", "source": debate_source},
            {"id": "trader", "label": "交易员", "status": "done", "source": debate_source},
            {"id": "risk_debate", "label": "风控辩论", "status": "done", "source": debate_source},
        ]
        if llm_mode
        else [{"id": "debate", "label": "多空辩论", "status": "degraded", "source": "rule-engine"}]
    )
    stages: list[dict[str, Any]] = [
        {"id": "market", "label": "行情 Agent", "status": "done", "source": quote.get("source", "market-gateway")},
        {"id": "technical", "label": "技术面 Agent", "status": "done", "source": kline.get("source", "market-gateway")},
        {
            "id": "financial", "label": "财务 Agent",
            "status": "done" if financials.get("available") else "degraded",
            "source": financials.get("source") or financials.get("provider", "unavailable"),
        },
        {"id": "news", "label": "资讯 Agent", "status": "done", "source": news.get("source", "unknown")},
        {"id": "risk", "label": "风险 Agent", "status": "done", "source": "rule-engine"},
        *debate_stages,
        {
            "id": "synthesis", "label": "归纳 Agent",
            "status": "done" if llm_mode else "degraded",
            "source": synthesis.get("provider", "rule-engine"),
        },
    ]
    report = {
        "symbol": symbol,
        "status": "complete",
        "elapsedMs": round((time.perf_counter() - started) * 1000, 1),
        "stages": stages,
        "evidence": {
            "quote": quote,
            "technical": technical,
            "financials": financials,
            "news": news,
            "riskFlags": risks,
            "levels": _levels_from_kline(kline),
        },
        "synthesis": synthesis,
        "disclaimer": "研究结果仅供信息分析，不构成投资建议；请核对来源时间与陈旧标记。",
    }
    return report


def _chat_messages(question: str, history: list[dict] | None, symbol: str, quote: dict | None) -> list[dict]:
    """run_chat / stream_chat 共用的消息组装：system（含可选标的快照锚定 + 工具指令协议）+ 最近 8 轮 + 本轮问题。"""
    system = _CHAT_SYSTEM_PROMPT + _CHAT_TOOLS_PROMPT
    if symbol and quote:
        name = quote.get("name", "")
        price = quote.get("price")
        change = quote.get("changePct")
        if change is None and price and quote.get("prevClose"):
            # 快照未带涨跌幅（如离线快照）→ 由现价/昨收现算，避免模型拿到空值
            change = round((price - quote["prevClose"]) / quote["prevClose"] * 100, 2)
        system += (
            f"\n当前上下文标的：{name}（{symbol}），最新价 {price}，涨跌幅 {change}%。"
            "回答中引用该标的数字时只能使用此快照。"
        )
    messages: list[dict] = [{"role": "system", "content": system}]
    for item in (history or [])[-8:]:
        role = "assistant" if item.get("role") == "ai" else "user"
        content = str(item.get("content", ""))[:500]
        if content:
            messages.append({"role": role, "content": content})
    messages.append({"role": "user", "content": question})
    return messages


_CHAT_FALLBACK_TEXT = (
    "模型网关当前不可用，已切换规则模式。你可以：\n"
    "1) 输入股票名称（如「分析宁德时代」）触发多 Agent 研究；\n"
    "2) 稍后重试通用提问。"
)


async def run_chat(question: str, history: list[dict] | None = None, symbol: str = "", quote: dict | None = None) -> dict:
    """通用问答（非个股研究链路）：走 LLM 网关，无模型时显式规则降级。

    history 为 [{role: "user"|"ai", content: str}]，最多取最近 8 轮；
    symbol/quote 提供时把最新快照注入 system 上下文（防模型编造价格）。
    """
    messages = _chat_messages(question, history, symbol, quote)

    response: dict | None = None
    try:
        async for chunk in gateway.chat_completions(
            model="zhiniu/quick", messages=messages, stream=False,
            temperature=0.4, max_tokens=600,
        ):
            response = chunk
    except Exception:  # noqa: BLE001 - 网关异常走降级文案，通用问答永不 500
        response = None
    if response and response.get("provider") and response.get("id") != "mock-llm":
        content = ((response.get("choices") or [{}])[0].get("message") or {}).get("content", "").strip()
        if content:
            clean_text, tools = await _process_chat_tools(content, symbol)
            clean_text, charts = _extract_charts(clean_text)
            clean_text, verdict = _extract_verdict(clean_text)
            # 无指令强重试（对标 KuiklyStock）：用户明确要操作、模型只说不发指令 → 补一轮只要指令
            if not tools and _looks_like_tool_intent(question):
                retry_messages = messages + [
                    {"role": "assistant", "content": content},
                    {"role": "user", "content": _TOOL_NUDGE},
                ]
                retry_content, _, _ = await _chat_once(retry_messages)
                if retry_content:
                    _, retry_tools = await _process_chat_tools(retry_content, symbol)
                    tools = retry_tools
            return {
                "mode": "llm",
                "provider": response.get("provider", ""),
                "model": response.get("model", ""),
                "content": clean_text[:1200],
                "tools": tools,
                "verdict": verdict,
                "charts": charts,
            }
    return {
        "mode": "deterministic_fallback",
        "provider": "rule-engine",
        "model": "none",
        "content": _CHAT_FALLBACK_TEXT,
        "tools": [],
        "verdict": None,
        "charts": [],
    }


async def stream_chat(
    question: str,
    history: list[dict] | None = None,
    symbol: str = "",
    quote: dict | None = None,
) -> AsyncIterator[dict[str, Any]]:
    """通用问答流式版（/agent/chat/stream）：逐 delta 转发模型输出。

    帧协议：chat_started → delta* → tool*（App 操作指令，客户端执行）→ chat_finished（终帧含完整正文与 mode）。
    Mock LLM（id=mock-llm）不透传占位文本，直接以规则降级文本收尾，
    与 /agent/chat 的语义保持一致（mock ≠ 模型输出）。
    工具指令行（⟦TOOL⟧…）不下发为正文：见 marker 即停流转缓冲，终帧输出清洗后的正文。
    """
    messages = _chat_messages(question, history, symbol, quote)
    yield {"type": "chat_started", "final": False}
    parts: list[str] = []          # 已确定不含指令的正文（含已下发部分）
    tail = ""                      # 尾部缓冲：可能是被 delta 截断的 marker 前缀
    rest = ""                      # 命中 marker 之后的全部内容（指令区，不下发）
    hit_directive = False
    provider = ""
    model = ""
    is_mock = False
    marker_len = max(len(m) for m in _STREAM_MARKERS)

    def _marker_index(text: str) -> int:
        found = [text.find(m) for m in _STREAM_MARKERS if text.find(m) >= 0]
        return min(found) if found else -1

    try:
        async for chunk in gateway.chat_completions(
            model="zhiniu/quick", messages=messages, stream=True,
            temperature=0.4, max_tokens=600,
        ):
            provider = chunk.get("provider") or provider
            model = chunk.get("model") or model
            if chunk.get("id") == "mock-llm":
                is_mock = True
            delta = ((chunk.get("choices") or [{}])[0].get("delta") or {}).get("content")
            if not delta or is_mock:
                continue
            if hit_directive:
                rest += delta
                continue
            tail += delta
            idx = _marker_index(tail)
            if idx >= 0:
                body = tail[:idx]
                if body:
                    parts.append(body)
                    yield {"type": "delta", "content": body, "final": False}
                rest = tail[idx:]
                tail = ""
                hit_directive = True
                continue
            # 保留 marker_len-1 字符不下发，防止 marker 被 delta 截断后漏出半截指令
            safe = len(tail) - (marker_len - 1)
            if safe > 0:
                body, tail = tail[:safe], tail[safe:]
                parts.append(body)
                yield {"type": "delta", "content": body, "final": False}
    except Exception:  # noqa: BLE001 - 网关异常按降级处理，流式问答永不 500
        parts = []
        tail = ""
        rest = ""
        hit_directive = False
    # 结束：未命中指令时把尾部缓冲（防截断而保留的部分）补发，避免丢字
    if not hit_directive and tail:
        parts.append(tail)
        yield {"type": "delta", "content": tail, "final": False}
        tail = ""
    content = "".join(parts)
    if not is_mock and provider and (content.strip() or rest.strip()):
        clean_text, tools = await _process_chat_tools(content + ("\n" + rest if rest else ""), symbol)
        clean_text, charts = _extract_charts(clean_text)
        clean_text, verdict = _extract_verdict(clean_text)
        # 无指令强重试：操作意图明确但模型只说不发指令 → 补一轮只要指令（不影响已流出的正文）
        if not tools and _looks_like_tool_intent(question):
            retry_messages = messages + [
                {"role": "assistant", "content": content + ("\n" + rest if rest else "")},
                {"role": "user", "content": _TOOL_NUDGE},
            ]
            retry_content, _, _ = await _chat_once(retry_messages)
            if retry_content:
                _, retry_tools = await _process_chat_tools(retry_content, symbol)
                tools = retry_tools
        if clean_text:
            clean_text = clean_text[:1200]
        else:
            clean_text = "（本次回复仅包含工具指令）"
        for tool in tools:
            yield {"type": "tool", "name": tool["name"], "args": tool["args"], "display": tool["display"], "final": False}
        yield {
            "type": "chat_finished", "mode": "llm", "provider": provider,
            "model": model, "content": clean_text, "tools": tools,
            "verdict": verdict, "charts": charts, "final": True,
        }
        return
    yield {
        "type": "chat_finished", "mode": "deterministic_fallback",
        "provider": "rule-engine", "model": "none",
        "content": _CHAT_FALLBACK_TEXT, "tools": [],
        "verdict": None, "charts": [], "final": True,
    }


def _num_or_none(v: Any) -> float | None:
    try:
        if isinstance(v, bool):
            return None
        return float(v)
    except (TypeError, ValueError):
        return None


_APPEARANCE_LABELS = {"light": "浅色", "dark": "深色", "system": "跟随系统"}


def _norm_appearance(v: Any) -> str | None:
    """外观归一化：容忍 深色/夜间/暗黑/dark/night 等（对标 KuiklyStock setThemeColor 的别名兜底）。"""
    s = str(v or "").strip().lower()
    if not s:
        return None
    if any(w in s for w in ("深", "夜", "暗", "dark", "night")):
        return "dark"
    if any(w in s for w in ("浅", "亮", "白", "light")):
        return "light"
    if any(w in s for w in ("系统", "自动", "system", "auto")):
        return "system"
    return None


def _norm_color_mode(v: Any) -> str | None:
    """涨跌配色归一化：red_up=红涨绿跌（A 股），green_up=绿涨红跌（海外）。"""
    s = str(v or "").strip().lower()
    if not s:
        return None
    if any(w in s for w in ("绿涨", "红跌", "欧美", "海外", "green_up", "greenup", "us")):
        return "green_up"
    if any(w in s for w in ("红涨", "绿跌", "a股", "a 股", "中国", "red_up", "redup", "cn")):
        return "red_up"
    if s in ("0", "1"):
        return "red_up" if s == "0" else "green_up"
    return None


def _extract_verdict(text: str) -> tuple[str, dict | None]:
    """抽取【AI观点】结论行：返回 (剥除后的正文, {risk, action}|None)。

    宽松解析（对标 KuiklyStock AiVerdict）：容忍省略标签/语序颠倒，如
    「【AI观点】高风险｜持有」「【AI观点】操作建议：卖出｜风险：低」。
    """
    lines = text.splitlines()
    for i in range(len(lines) - 1, -1, -1):
        stripped = lines[i].strip()
        if "【AI观点】" not in stripped:
            continue
        risk = next((label for key, label in
                     (("高风险", "高"), ("中风险", "中"), ("低风险", "低"), ("高", "高"), ("中", "中"), ("低", "低"))
                     if key in stripped), "")
        action = next((a for a in ("买入", "持有", "卖出", "观望") if a in stripped), "")
        body = "\n".join(lines[:i] + lines[i + 1:]).strip()
        if risk and action:
            return body, {"risk": risk, "action": action}
        return body, None
    return text, None


_KCHART_RE = re.compile(r"\[KCHART:([A-Za-z]{2}\d{6})(?::[a-z]+)?\]")


def _extract_charts(text: str) -> tuple[str, list[str]]:
    """抽取 [KCHART:symbol] 走势卡指令：返回 (剥除后的正文, [symbol])；最多 2 个、去重。"""
    symbols: list[str] = []
    for match in _KCHART_RE.finditer(text):
        sym = match.group(1).lower()
        if sym not in symbols:
            symbols.append(sym)
    if not symbols:
        return text, []
    body = _KCHART_RE.sub("", text)
    body = "\n".join(line for line in body.splitlines() if line.strip()).strip()
    return body, symbols[:2]


def _validate_zone(zone: Any, support: float | None, pressure: float | None) -> list[float] | None:
    """买卖区间防幻觉校验：必须是 [低, 高] 且落在支撑/压力附近（±3%），否则丢弃（不编造）。"""
    if not isinstance(zone, (list, tuple)) or len(zone) != 2:
        return None
    lo, hi = _num_or_none(zone[0]), _num_or_none(zone[1])
    if lo is None or hi is None or not lo < hi:
        return None
    if support is None or pressure is None:
        return None
    if lo < support * 0.97 or hi > pressure * 1.03:
        return None
    return [round(lo, 3), round(hi, 3)]


def _sanitize_llm_insight(
    parsed: dict,
    technical: dict,
    rule_risk: dict,
    support: float | None,
    pressure: float | None,
) -> dict:
    """对模型诊股 JSON 逐字段白名单校验：枚举归位、文本截断、区间数值校验。"""
    verdict = str(parsed.get("verdict", "")).strip()
    if verdict not in _INSIGHT_VERDICTS:
        verdict = {"up": "偏强", "down": "偏弱"}.get(technical.get("direction", ""), "中性")
    risk_level = str(parsed.get("riskLevel", "")).strip()
    if risk_level not in _INSIGHT_RISK_LEVELS:
        risk_level = rule_risk.get("level", "medium")
    signals = [str(s).strip()[:90] for s in (parsed.get("signals") or []) if str(s).strip()][:5]
    follow_ups = [str(s).strip()[:40] for s in (parsed.get("followUps") or []) if str(s).strip()][:4]
    return {
        "verdict": verdict,
        "summary": str(parsed.get("summary", "")).strip()[:160],
        "trend": str(parsed.get("trend", "")).strip()[:100],
        "valuation": str(parsed.get("valuation", "")).strip()[:100],
        "earnings": str(parsed.get("earnings", "")).strip()[:100],
        "volume": str(parsed.get("volume", "")).strip()[:100],
        "risk": str(parsed.get("risk", "")).strip()[:120],
        "signals": signals,
        "buyZone": _validate_zone(parsed.get("buyZone"), support, pressure),
        "sellZone": _validate_zone(parsed.get("sellZone"), support, pressure),
        "riskLevel": risk_level,
        "followUps": follow_ups,
    }


async def _llm_insight(
    symbol: str,
    quote: dict,
    technical: dict,
    levels: dict,
    financials: dict,
    rule_risk_level: str = "medium",
) -> tuple[dict | None, str, str]:
    """单次 JSON-mode 调用产出诊股判断；失败/无 Key/解析失败返回 (None, ...) 走规则降级。"""
    evidence_lines = [
        f"标的：{quote.get('name', '')}（{symbol}）",
        f"现价 {quote.get('price')}，涨跌幅 {quote.get('changePct')}%，数据源 {quote.get('source', '')}"
        + ("（陈旧缓存）" if quote.get("isStale") else ""),
        f"今日成交量 {round((quote.get('volume') or 0) / 100)} 手，成交额 {round(quote.get('amount') or 0)} 元"
        + (f"，量比 {quote.get('volumeRatio')}" if quote.get("volumeRatio") else "")
        + (f"，换手率 {quote.get('turnoverRate')}%" if quote.get("turnoverRate") else ""),
        f"趋势 direction={technical.get('direction')}，区间涨跌 {technical.get('changePct')}%，"
        f"RSI(14) {technical.get('rsi14')}，MA20 {technical.get('ma20')}，样本 {technical.get('sampleSize')} 根",
    ]
    if levels:
        evidence_lines.append(f"压力位 {levels.get('pressure')}，支撑位 {levels.get('support')}（近 {levels.get('window')} 根 K 线高低点）")
    if financials.get("available"):
        evidence_lines.append(
            f"PE {financials.get('pe')}，PB {financials.get('pb')}，总市值 {financials.get('marketCap')} 元，"
            f"报告期 {financials.get('reportDate')}，营收 {financials.get('revenue')} 元，"
            f"归母净利 {financials.get('netProfit')} 元，ROE {financials.get('roe')}%"
        )
    else:
        evidence_lines.append("估值/财报证据：暂不可用")
    messages = [
        {"role": "system", "content": _INSIGHT_SYSTEM_PROMPT},
        {"role": "user", "content": "证据：\n" + "\n".join(evidence_lines) + "\n\n请输出诊股 JSON。"},
    ]
    try:
        response: dict | None = None
        async for chunk in gateway.chat_completions(
            model="zhiniu/quick", messages=messages, stream=False,
            temperature=0.3, max_tokens=600,
            response_format={"type": "json_object"},
        ):
            response = chunk
    except Exception:  # noqa: BLE001 - 无模型/超时 → 规则降级
        return None, "", ""
    if not response or response.get("id") == "mock-llm":
        return None, "", ""
    content = ((response.get("choices") or [{}])[0].get("message") or {}).get("content", "")
    import json as _json

    try:
        start, end = content.find("{"), content.rfind("}")
        parsed = _json.loads(content[start : end + 1] if start >= 0 else content)
    except Exception:
        return None, response.get("provider", ""), response.get("model", "")
    if not isinstance(parsed, dict) or not parsed.get("summary"):
        return None, response.get("provider", ""), response.get("model", "")
    return (
        _sanitize_llm_insight(parsed, technical, {"level": rule_risk_level}, levels.get("support"), levels.get("pressure")),
        response.get("provider", ""),
        response.get("model", ""),
    )


async def run_insight(symbol: str) -> dict:
    """个股 AI 诊股（/agent/insight）：服务端取证 → 单次 LLM 结构化判断 → 规则兜底。

    缓存/并发去重复用 store（keyword=insight:v1）；LLM 不可用时返回确定性规则结果
    并显式 mode=deterministic_fallback（不伪装模型）。买卖区间由服务端校验回填。
    """
    cached = await store.get_research(symbol, _INSIGHT_CACHE_KEY)
    if cached is not None:
        return cached
    inflight = store.begin_inflight(symbol, _INSIGHT_CACHE_KEY)
    if inflight is not None:
        shared = await store.await_inflight(inflight)
        if shared is not None:
            return {**shared, "cached": True, "cache": "inflight"}
    started = time.perf_counter()
    try:
        quote, kline, financials = await asyncio.gather(
            execute_tool_async("get_realtime_quote", {"symbol": symbol}),
            execute_tool_async("get_kline", {"symbol": symbol, "datalen": 120}),
            execute_tool_async("get_financials", {"symbol": symbol}),
        )
        technical = _technical_summary(kline)
        levels = _levels_from_kline(kline)
        risks = _risk_flags(quote, technical, {"total": 0, "isStale": False}, financials)
        base = build_insight(
            quote, technical, risks,
            {"mode": "deterministic_fallback", "provider": "rule-engine", "levels": levels, "trader": {}},
            kline,
        )
        llm, provider, model = await _llm_insight(
            symbol, quote, technical, levels, financials,
            rule_risk_level=base["riskLevel"]["level"],
        )
        risk_block = base["riskLevel"]
        if llm is not None and llm.get("riskLevel") in _INSIGHT_RISK_LEVELS:
            labels = {"low": "低", "medium_low": "中低", "medium": "中", "medium_high": "中高", "high": "高"}
            risk_block = {
                "level": llm["riskLevel"],
                "label": labels[llm["riskLevel"]],
                "rationale": f"模型判定（依据 {len(llm.get('signals', []))} 条信号）；规则基准为「{base['riskLevel']['label']}」",
            }
        result = {
            "symbol": symbol,
            "name": quote.get("name", ""),
            "mode": "llm" if llm is not None else "deterministic_fallback",
            "provider": provider if llm is not None else "rule-engine",
            "model": model if llm is not None else "none",
            "generatedAt": time.strftime("%Y-%m-%d %H:%M:%S"),
            "elapsedMs": round((time.perf_counter() - started) * 1000, 1),
            "quote": {k: quote.get(k) for k in ("name", "price", "changePct", "date", "time", "source", "isStale")},
            "technical": technical,
            "levels": levels,
            "riskLevel": risk_block,
            "advice": base["advice"],
            "llm": llm,
            "disclaimer": "AI 诊股结果仅供信息分析，不构成投资建议；数值均来自服务端行情证据。",
        }
        await store.put_research(symbol, _INSIGHT_CACHE_KEY, result)
        store.finish_inflight(symbol, _INSIGHT_CACHE_KEY, result)
        return result
    except Exception as exc:
        store.fail_inflight(symbol, _INSIGHT_CACHE_KEY, exc)
        raise


def _rule_compare_summary(stocks: list[dict]) -> dict:
    """确定性对比基准（无模型时也给出可核对结论，且数字全部来自证据）。"""
    if len(stocks) != 2:
        return {"summary": "对比标的不完整。", "stronger": "none", "pointsA": [], "pointsB": [], "conclusion": ""}
    a, b = stocks
    a_score = (a.get("changePercent") or 0) - (a.get("rsi14") or 50) / 100
    b_score = (b.get("changePercent") or 0) - (b.get("rsi14") or 50) / 100
    stronger = "A" if a_score > b_score else "B" if b_score > a_score else "none"
    def _points(s: dict) -> list[str]:
        items = [f"区间涨跌 {s.get('rangeChangePercent')}%，方向 {s.get('direction')}"]
        if s.get("rsi14"):
            items.append(f"RSI(14) {s['rsi14']}")
        if s.get("pe") is not None:
            items.append(f"PE {s['pe']}")
        return items
    return {
        "summary": (
            f"{a.get('name')} 区间涨跌 {a.get('rangeChangePercent')}%（RSI {a.get('rsi14')}），"
            f"{b.get('name')} 区间涨跌 {b.get('rangeChangePercent')}%（RSI {b.get('rsi14')}）。"
            "以上为规则口径对比；模型可用时补充定性归纳。"
        ),
        "stronger": stronger,
        "pointsA": _points(a),
        "pointsB": _points(b),
        "conclusion": "规则降级 · 未伪装模型",
    }


async def run_compare(symbols: list[str], focus: str = "") -> dict:
    """双股对比（/agent/compare）：双侧真实证据 → LLM 定性对比 → 规则基准兜底。"""
    return await _compare_impl(symbols, focus, None)


async def stream_compare(symbols: list[str], focus: str = "") -> AsyncIterator[dict[str, Any]]:
    """双股对比流式版（/agent/compare/stream）：evidence_a → evidence_b → llm → result → 终帧。"""
    queue: asyncio.Queue = asyncio.Queue()

    async def progress(stage: str, label: str) -> None:
        await queue.put({"type": "stage", "stage": stage, "label": label, "final": False})

    run_id = uuid.uuid4().hex
    task = asyncio.create_task(_compare_impl(symbols, focus, progress))

    async def pump_finish() -> dict:
        result = await task
        await queue.put({"type": "result", "result": result, "final": False})
        await queue.put({"type": "run_finished", "runId": run_id, "elapsedMs": result.get("elapsedMs", 0), "final": True})
        return result

    finish_task = asyncio.create_task(pump_finish())
    while True:
        frame = await queue.get()
        if frame.get("type") == "__end__":
            break
        yield frame
        if frame.get("final"):
            break
    await finish_task


async def _compare_impl(symbols: list[str], focus: str, progress) -> dict:
    started = time.perf_counter()
    stocks: list[dict] = []
    for index, sym in enumerate(symbols):
        quote, kline, financials = await asyncio.gather(
            execute_tool_async("get_realtime_quote", {"symbol": sym}),
            execute_tool_async("get_kline", {"symbol": sym, "datalen": 60}),
            execute_tool_async("get_financials", {"symbol": sym}),
        )
        technical = _technical_summary(kline)
        stocks.append({
            "symbol": sym,
            "name": quote.get("name", ""),
            "price": quote.get("price"),
            "changePercent": quote.get("changePct"),
            "direction": technical.get("direction"),
            "rangeChangePercent": technical.get("changePct"),
            "rsi14": technical.get("rsi14"),
            "ma20": technical.get("ma20"),
            "pe": financials.get("pe") if financials.get("available") else None,
            "pb": financials.get("pb") if financials.get("available") else None,
            "marketCap": financials.get("marketCap") if financials.get("available") else None,
            "source": quote.get("source", ""),
            "isStale": bool(quote.get("isStale")),
        })
        if progress is not None:
            await progress(f"evidence_{chr(97 + index)}", f"{quote.get('name', sym)} 证据")
    rule = _rule_compare_summary(stocks)
    llm: dict | None = None
    provider = model = ""
    if len(stocks) == 2:
        a, b = stocks
        import json as _json

        if progress is not None:
            await progress("llm", "AI 对比归纳")
        prompt = (
            f"对比标的 A：{_json.dumps(a, ensure_ascii=False)}\n"
            f"对比标的 B：{_json.dumps(b, ensure_ascii=False)}\n"
            + (f"用户关注点：{focus}\n" if focus else "")
            + "输出严格 JSON：{\"summary\":\"80字内对比归纳\",\"stronger\":\"A|B|none\","
              "\"pointsA\":[\"3条内，须引用上述数值\"],\"pointsB\":[],\"conclusion\":\"60字内结论与风险提示\"}"
        )
        try:
            response: dict | None = None
            async for chunk in gateway.chat_completions(
                model="zhiniu/quick",
                messages=[{"role": "system", "content": "你是知牛的双股对比分析师，只输出 JSON，数字只能引用输入证据。"},
                          {"role": "user", "content": prompt}],
                stream=False, temperature=0.3, max_tokens=500,
                response_format={"type": "json_object"},
            ):
                response = chunk
            if response and response.get("id") != "mock-llm":
                content = ((response.get("choices") or [{}])[0].get("message") or {}).get("content", "")
                start, end = content.find("{"), content.rfind("}")
                parsed = _json.loads(content[start : end + 1] if start >= 0 else content)
                if isinstance(parsed, dict) and parsed.get("summary"):
                    stronger = str(parsed.get("stronger", "none"))
                    llm = {
                        "summary": str(parsed.get("summary", ""))[:120],
                        "stronger": stronger if stronger in ("A", "B", "none") else "none",
                        "pointsA": [str(p)[:90] for p in (parsed.get("pointsA") or [])][:3],
                        "pointsB": [str(p)[:90] for p in (parsed.get("pointsB") or [])][:3],
                        "conclusion": str(parsed.get("conclusion", ""))[:90],
                    }
                    provider = response.get("provider", "")
                    model = response.get("model", "")
        except Exception:  # noqa: BLE001 - 对比 LLM 失败 → 规则基准
            llm = None
    return {
        "symbols": list(symbols),
        "mode": "llm" if llm is not None else "deterministic_fallback",
        "provider": provider if llm is not None else "rule-engine",
        "model": model if llm is not None else "none",
        "generatedAt": time.strftime("%Y-%m-%d %H:%M:%S"),
        "elapsedMs": round((time.perf_counter() - started) * 1000, 1),
        "stocks": stocks,
        "llm": llm,
        "rule": rule,
        "disclaimer": "对比结果仅供信息分析，不构成投资建议。",
    }


async def run_research(symbol: str, keyword: str = "") -> dict:
    # 缓存（内存 / 可选 Postgres）命中直接返回；同标的研究进行中则共享结果（并发去重）
    cached = await store.get_research(symbol, keyword)
    if cached is not None:
        return cached
    inflight = store.begin_inflight(symbol, keyword)
    if inflight is not None:
        shared = await store.await_inflight(inflight)
        if shared is not None:
            return {**shared, "cached": True, "cache": "inflight"}
    started = time.perf_counter()
    try:
        quote, kline, financials, news = await asyncio.gather(
            execute_tool_async("get_realtime_quote", {"symbol": symbol}),
            execute_tool_async("get_kline", {"symbol": symbol, "datalen": 120}),
            execute_tool_async("get_financials", {"symbol": symbol}),
            execute_tool_async("search_news", {"symbol": symbol, "keyword": keyword}),
        )
        technical = _technical_summary(kline)
        risks = _risk_flags(quote, technical, news, financials)
        synthesis = await _synthesize(symbol, keyword, quote, technical, financials, news, risks, kline)
        report = _build_report(symbol, quote, kline, financials, news, synthesis, started)
        report["insight"] = build_insight(quote, technical, risks, synthesis, kline)
        await store.put_research(symbol, keyword, report)
        store.finish_inflight(symbol, keyword, report)
        return report
    except Exception as exc:
        store.fail_inflight(symbol, keyword, exc)
        raise


async def stream_research(symbol: str, keyword: str = "") -> AsyncIterator[dict[str, Any]]:
    """Typed progress stream for UI timelines; never exposes private reasoning text."""
    run_id = uuid.uuid4().hex
    started = time.perf_counter()
    yield {"type": "run_started", "runId": run_id, "symbol": symbol, "final": False}
    tasks = {
        "market": asyncio.create_task(execute_tool_async("get_realtime_quote", {"symbol": symbol})),
        "technical": asyncio.create_task(execute_tool_async("get_kline", {"symbol": symbol, "datalen": 120})),
        "financial": asyncio.create_task(execute_tool_async("get_financials", {"symbol": symbol})),
        "news": asyncio.create_task(execute_tool_async("search_news", {"symbol": symbol, "keyword": keyword})),
    }
    labels = {
        "market": "行情 Agent",
        "technical": "技术面 Agent",
        "financial": "财务 Agent",
        "news": "资讯 Agent",
    }
    try:
        evidence: dict[str, dict] = {}
        for stage_id, task in tasks.items():
            evidence[stage_id] = await task
            yield {
                "type": "stage_completed",
                "runId": run_id,
                "stage": stage_id,
                "label": labels[stage_id],
                "source": evidence[stage_id].get("source", "market-gateway"),
                "final": False,
            }
        technical = _technical_summary(evidence["technical"])
        risks = _risk_flags(evidence["market"], technical, evidence["news"], evidence["financial"])
        yield {
            "type": "stage_completed",
            "runId": run_id,
            "stage": "risk",
            "label": "风险 Agent",
            "source": "rule-engine",
            "final": False,
        }
        # 辩论管线：progress 回调经队列实时转发为 SSE 帧（多头/空头/经理/交易员/风控）
        progress_queue: asyncio.Queue = asyncio.Queue()

        async def progress(stage: str, label: str, status: str) -> None:
            await progress_queue.put({
                "type": "stage_started" if status == "running" else "stage_completed",
                "runId": run_id,
                "stage": stage,
                "label": label,
                "source": "model-gateway",
                "final": False,
            })

        synthesis_task = asyncio.create_task(_synthesize(
            symbol, keyword, evidence["market"], technical,
            evidence["financial"], evidence["news"], risks, evidence["technical"], progress,
        ))
        queue_task = asyncio.create_task(progress_queue.get())
        while True:
            done, _ = await asyncio.wait(
                {synthesis_task, queue_task}, return_when=asyncio.FIRST_COMPLETED
            )
            if queue_task in done:
                yield queue_task.result()
                queue_task = asyncio.create_task(progress_queue.get())
            if synthesis_task in done:
                while not progress_queue.empty():
                    yield progress_queue.get_nowait()
                queue_task.cancel()
                break
        synthesis = synthesis_task.result()
        report = _build_report(
            symbol, evidence["market"], evidence["technical"], evidence["financial"],
            evidence["news"], synthesis, started
        )
        report["insight"] = build_insight(evidence["market"], technical, risks, synthesis, evidence["technical"])
        await store.put_research(symbol, keyword, report)
        yield {
            "type": "stage_completed", "runId": run_id, "stage": "synthesis",
            "label": "归纳 Agent", "source": synthesis.get("provider", "rule-engine"),
            "status": "done" if synthesis.get("mode") == "llm" else "degraded", "final": False,
        }
        yield {"type": "result", "runId": run_id, "result": report, "final": False}
        yield {"type": "run_finished", "runId": run_id, "elapsedMs": report["elapsedMs"], "final": True}
    except Exception as exc:
        for task in tasks.values():
            if not task.done():
                task.cancel()
        await asyncio.gather(*tasks.values(), return_exceptions=True)
        yield {
            "type": "run_error",
            "runId": run_id,
            "code": "research_failed",
            "message": str(exc),
            "final": True,
        }
