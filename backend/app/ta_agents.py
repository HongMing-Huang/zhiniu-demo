"""TradingAgents 改造版研究辩论管线（多 Agent 结构移植 + 本网关执行）。

角色结构移植自 TauricResearch/TradingAgents（MIT License）：
- 多头/空头研究员对抗辩论   ← tradingagents/agents/researchers/{bull,bear}_researcher.py
- 研究经理五档评级裁决      ← tradingagents/agents/managers/research_manager.py
- 交易员价格化计划          ← tradingagents/agents/trader/trader.py（入场/止损必须绝对价格）
- 风控三方辩论              ← tradingagents/agents/risk_mgmt/*_debator.py

与原版的差异（改造点）：
- 不引入 LangGraph/LangChain，直接经 gateway.chat_completions 降级链调用；
- 证据只来自 tools.py 同一批真实行情/资讯函数（AGENTS.md §4：禁止再造假数据）；
- 无可用模型 / 超时 / 异常时回退 rule-engine，mode=deterministic_fallback 显式声明，
  绝不把规则结果伪装成 LLM 输出。
"""
from __future__ import annotations

import asyncio
import json
import os
from typing import Awaitable, Callable, Optional

from .gateway import gateway

DEBATE_TIMEOUT_S = float(os.getenv("ZHINIU_AGENT_TIMEOUT", "75"))
RATING_LABELS = ("买入", "增持", "持有", "减持", "卖出")
ProgressFn = Optional[Callable[[str, str, str], Awaitable[None]]]


async def _chat_once(prompt: str, max_tokens: int = 300) -> tuple[Optional[str], dict]:
    """单次网关调用；返回 (content, meta)。meta.id == "mock-llm" 表示无真实模型。"""
    response = None
    async for chunk in gateway.chat_completions(
        model="zhiniu/quick",
        messages=[{"role": "user", "content": prompt}],
        stream=False,
        temperature=0.3,
        max_tokens=max_tokens,
    ):
        response = chunk
    if not response or response.get("id") == "mock-llm" or not response.get("provider"):
        return None, {"id": "mock-llm"}
    content = response.get("choices", [{}])[0].get("message", {}).get("content", "")
    return content, {"provider": response.get("provider", ""), "model": response.get("model", "")}


async def _emit(progress: ProgressFn, stage: str, label: str, status: str) -> None:
    if progress is None:
        return
    try:
        await progress(stage, label, status)
    except Exception:
        pass  # 进度回调失败不影响研究结果


def _compact_evidence(evidence: dict) -> str:
    quote = evidence.get("quote") or {}
    technical = evidence.get("technical") or {}
    financials = evidence.get("financials") or {}
    news = evidence.get("news") or {}
    levels = evidence.get("levels") or {}
    compact_news = [
        {"title": item.get("title", ""), "source": item.get("source", "")}
        for item in (news.get("items") or [])[:5]
    ]
    return json.dumps({
        "symbol": evidence.get("symbol", ""),
        "name": quote.get("name", ""),
        "price": quote.get("price"),
        "prevClose": quote.get("prevClose"),
        "technical": technical,
        "levels": levels,
        "financials": {
            k: financials.get(k)
            for k in ("pe", "pb", "marketCap", "roe", "revenue", "netProfit", "industry", "reportDate")
        },
        "news": compact_news,
        "riskFlags": evidence.get("riskFlags") or [],
    }, ensure_ascii=False)


def _parse_json_block(content: str) -> Optional[dict]:
    text = (content or "").strip()
    if text.startswith("```"):
        text = text.strip("`").lstrip("json").strip()
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end <= start:
        return None
    try:
        return json.loads(text[start:end + 1])
    except Exception:
        return None


def _argument_prompt(role: str, evidence_json: str, opponent: str) -> str:
    """多/空研究员提示词（TradingAgents bull/bear_researcher 的中文 A 股适配）。"""
    return (
        f"你是{role}方研究员，参与 A 股个股投资辩论。只能依据给定证据，不得编造数字。\n"
        f"证据：{evidence_json}\n"
        + (f"对方观点：{opponent}\n请针对性反驳。" if opponent else "请给出开场立论。")
        + "\n要求：一段话不超过110字，观点+数据依据+结论，禁止交易指令式措辞（如“建议立即买入”）。"
    )


def _manager_prompt(evidence_json: str, debate_text: str) -> str:
    """研究经理提示词（TradingAgents research_manager 的五档评级结构）。"""
    return (
        "你是研究经理，裁决多空辩论并给出五档评级（买入/增持/持有/减持/卖出）。"
        "证据均衡或不足时必须选持有，不为显得果断而强行给方向。\n"
        f"证据：{evidence_json}\n辩论记录：\n{debate_text}\n"
        '输出严格 JSON：{"rating":"持有","confidence":0.55,"summary":"不超过90字"}'
        "（rating 取五档之一，confidence 0~1）。"
    )


def _trader_prompt(evidence_json: str, plan_json: dict) -> str:
    """交易员提示词（TradingAgents trader：价位必须为绝对价格并锚定技术结构）。"""
    return (
        "你是交易员，把研究计划落成价格化观察方案。价位必须为绝对价格（如 189.5），"
        "禁止百分比或区间；压力/支撑只能取自证据中的 levels，无法给出时设为 null。\n"
        f"证据：{evidence_json}\n研究计划：{json.dumps(plan_json, ensure_ascii=False)}\n"
        '输出严格 JSON：{"entry": null, "stop": null, "plan":"不超过60字观察思路"}'
        "（entry/stop 为价格数字或 null；本环节只做观察记录，不下达交易指令）。"
    )


def _risk_prompt(stance: str, evidence_json: str, prior_text: str) -> str:
    return (
        f"你是{'激进' if stance == 'aggressive' else '中性' if stance == 'neutral' else '保守'}风控分析师，"
        f"对上述研究结论做风险评估。只能依据证据。\n证据：{evidence_json}\n"
        + (f"前一位观点：{prior_text}\n请针对性回应。" if prior_text else "请给出本视角的首要判断。")
        + "\n不超过80字，指出最关键的一个风险或机会。"
    )


def _rule_debate(evidence: dict) -> dict:
    """无模型时的确定性辩论（同构输出，显式 rule-engine 身份）。"""
    technical = evidence.get("technical") or {}
    financials = evidence.get("financials") or {}
    levels = evidence.get("levels") or {}
    risks = list(evidence.get("riskFlags") or [])
    direction = technical.get("direction", "insufficient")
    rsi = float(technical.get("rsi14", 50) or 50)
    price = float(technical.get("latest", 0) or 0)
    ma20 = float(technical.get("ma20", 0) or 0)
    roe = financials.get("roe")

    bull: list[str] = []
    bear: list[str] = []
    if direction == "up":
        bull.append(f"区间上行 {technical.get('changePct', 0):+.2f}%，动能延续")
    elif direction == "down":
        bear.append(f"区间下行 {technical.get('changePct', 0):+.2f}%，趋势偏弱")
    else:
        bear.append("区间震荡，方向信号不足")
    if price and ma20:
        (bull if price >= ma20 else bear).append(
            f"价格{'站上' if price >= ma20 else '跌破'} MA20（{ma20:.2f}）"
        )
    if rsi <= 35:
        bull.append(f"RSI({rsi:.0f}) 处于超卖区，存在修复动能")
    elif rsi >= 65:
        bear.append(f"RSI({rsi:.0f}) 偏超买，注意回撤")
    if isinstance(roe, (int, float)) and roe >= 10:
        bull.append(f"ROE {roe:.1f}%，盈利质量较好")
    bull = bull or ["暂无明确利多证据"]
    bear = bear or ["暂无明确利空证据"]
    rating = {"up": "增持", "down": "减持", "sideways": "持有"}.get(direction, "持有")
    stance = {"买入": "偏强", "增持": "偏强", "减持": "偏弱", "卖出": "偏弱"}.get(rating, "震荡")
    return {
        "mode": "deterministic_fallback",
        "provider": "rule-engine",
        "model": "none",
        "stance": stance,
        "rating": rating,
        "confidence": 0.5 if direction != "insufficient" else 0.4,
        "summary": (
            f"规则引擎裁定：{stance}（{rating}）；区间涨跌 {technical.get('changePct', 0):+.2f}%，"
            f"RSI(14) {rsi:.1f}。配置模型 Key 后可启用多 Agent 辩论。"
        ),
        "trend": {"up": "上行", "down": "下行", "sideways": "震荡"}.get(direction, "数据不足"),
        "pressure": levels.get("pressure"),
        "support": levels.get("support"),
        "bullPoints": bull,
        "bearPoints": bear,
        "riskNotes": risks or ["未触发规则风险项，仍需关注波动与数据时效"],
        "trader": None,
        "catalysts": [f"已检索 {int((evidence.get('news') or {}).get('total', 0) or 0)} 条相关资讯"],
        "risks": risks[:3] or ["未触发规则风险项，仍需关注波动与数据时效"],
    }


async def run_debate(evidence: dict, progress: ProgressFn = None) -> dict:
    """执行 TradingAgents 结构的辩论管线，返回与 rule-engine 同构的 synthesis。

    progress(stage_id, label, status) 可选，用于 SSE 时间线；任何异常/超时
    都会降级到 _rule_debate，保证研究链路永不失败。
    """
    try:
        return await asyncio.wait_for(_run_llm_debate(evidence, progress), timeout=DEBATE_TIMEOUT_S)
    except Exception:
        return _rule_debate(evidence)


async def _run_llm_debate(evidence: dict, progress: ProgressFn) -> dict:
    evidence_json = _compact_evidence(evidence)
    meta: dict = {}

    await _emit(progress, "bull", "多头研究员", "running")
    bull_text, meta = await _chat_once(_argument_prompt("多（Bull）", evidence_json, ""))
    if bull_text is None:
        return _rule_debate(evidence)  # 首跳即 mock → 无模型，直接规则回退
    await _emit(progress, "bull", "多头研究员", "done")

    await _emit(progress, "bear", "空头研究员", "running")
    bear_text, meta = await _chat_once(_argument_prompt("空（Bear）", evidence_json, bull_text))
    if bear_text is None:
        return _rule_debate(evidence)
    await _emit(progress, "bear", "空头研究员", "done")

    await _emit(progress, "manager", "研究经理", "running")
    manager_text, meta = await _chat_once(_manager_prompt(evidence_json, f"多：{bull_text}\n空：{bear_text}"), max_tokens=220)
    plan = _parse_json_block(manager_text or "")
    if not plan or plan.get("rating") not in RATING_LABELS:
        return _rule_debate(evidence)
    await _emit(progress, "manager", "研究经理", "done")

    await _emit(progress, "trader", "交易员", "running")
    trader_text, meta = await _chat_once(_trader_prompt(evidence_json, plan), max_tokens=200)
    trader = _parse_json_block(trader_text or "") or {}
    await _emit(progress, "trader", "交易员", "done")

    risk_notes: list[str] = []
    prior = ""
    for stance, stage, label in (("aggressive", "risk-aggressive", "激进风控"),
                                 ("neutral", "risk-neutral", "中性风控"),
                                 ("conservative", "risk-conservative", "保守风控")):
        await _emit(progress, stage, label, "running")
        text, meta = await _chat_once(_risk_prompt(stance, evidence_json, prior), max_tokens=160)
        if text is None:
            return _rule_debate(evidence)
        risk_notes.append(f"{label}：{text.strip()}")
        prior = text
        await _emit(progress, stage, label, "done")

    await _emit(progress, "synthesis", "归纳 Agent", "running")
    rating = plan["rating"]
    stance_label = {"买入": "偏强", "增持": "偏强", "减持": "偏弱", "卖出": "偏弱"}.get(rating, "震荡")
    levels = evidence.get("levels") or {}
    news_items = (evidence.get("news") or {}).get("items") or []
    try:
        confidence = max(0.0, min(1.0, float(plan.get("confidence", 0.5))))
    except Exception:
        confidence = 0.5
    synthesis = {
        "mode": "llm",
        "provider": meta.get("provider", ""),
        "model": meta.get("model", ""),
        "stance": stance_label,
        "rating": rating,
        "confidence": confidence,
        "summary": str(plan.get("summary", ""))[:180],
        "trend": {"up": "上行", "down": "下行", "sideways": "震荡"}.get(
            (evidence.get("technical") or {}).get("direction", ""), "数据不足"
        ),
        "pressure": levels.get("pressure"),
        "support": levels.get("support"),
        "bullPoints": [bull_text.strip()[:120]],
        "bearPoints": [bear_text.strip()[:120]],
        "riskNotes": [note[:120] for note in risk_notes],
        "trader": {
            "entry": trader.get("entry") if isinstance(trader.get("entry"), (int, float)) else None,
            "stop": trader.get("stop") if isinstance(trader.get("stop"), (int, float)) else None,
            "plan": str(trader.get("plan", ""))[:80],
        },
        "catalysts": [
            f"{item.get('title', '')[:60]}（{item.get('source', '')}）" for item in news_items[:3]
        ] or [f"已检索 {int((evidence.get('news') or {}).get('total', 0) or 0)} 条相关资讯"],
        "risks": list(evidence.get("riskFlags") or [])[:3],
    }
    await _emit(progress, "synthesis", "归纳 Agent", "done")
    return synthesis
