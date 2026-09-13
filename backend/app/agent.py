"""Auditable research-agent pipeline: code computes, an LLM may narrate.

Market/news acquisition and indicators stay deterministic and provenance
tracked.  A configured gateway model performs the final synthesis; when no
provider is available the same response shape is returned with an explicit
``deterministic_fallback`` mode instead of pretending that rules are an LLM.
"""
from __future__ import annotations

import asyncio
import time
import uuid
from collections.abc import AsyncIterator
from typing import Any

from .tools import execute_tool_async
from .ta_agents import run_debate
from .gateway import gateway

_CHAT_SYSTEM_PROMPT = (
    "你是知牛（ZhiNiu）的股票研究助手，面向 A 股个人投资者，用中文回答。规则：\n"
    "1. 概念/方法类问题（如指标含义、术语、分析方法）给出专业、简洁、结构化的解释，可用短列表。\n"
    "2. 严禁编造具体价格、财报数字或新闻；若上下文提供了标的快照，数字只能引用该快照。\n"
    "3. 涉及实时行情的问题，提醒用户在行情页查看最新数据。\n"
    "4. 不构成投资建议，涉及决策时提示风险。\n"
    "5. 回答不超过 300 字。"
)


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


async def run_chat(question: str, history: list[dict] | None = None, symbol: str = "", quote: dict | None = None) -> dict:
    """通用问答（非个股研究链路）：走 LLM 网关，无模型时显式规则降级。

    history 为 [{role: "user"|"ai", content: str}]，最多取最近 8 轮；
    symbol/quote 提供时把最新快照注入 system 上下文（防模型编造价格）。
    """
    system = _CHAT_SYSTEM_PROMPT
    if symbol and quote:
        name = quote.get("name", "")
        price = quote.get("price")
        change = quote.get("changePct")
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
            return {
                "mode": "llm",
                "provider": response.get("provider", ""),
                "model": response.get("model", ""),
                "content": content[:1200],
            }
    return {
        "mode": "deterministic_fallback",
        "provider": "rule-engine",
        "model": "none",
        "content": (
            "模型网关当前不可用，已切换规则模式。你可以：\n"
            "1) 输入股票名称（如「分析宁德时代」）触发多 Agent 研究；\n"
            "2) 稍后重试通用提问。"
        ),
    }


async def run_research(symbol: str, keyword: str = "") -> dict:
    started = time.perf_counter()
    quote, kline, financials, news = await asyncio.gather(
        execute_tool_async("get_realtime_quote", {"symbol": symbol}),
        execute_tool_async("get_kline", {"symbol": symbol, "datalen": 120}),
        execute_tool_async("get_financials", {"symbol": symbol}),
        execute_tool_async("search_news", {"symbol": symbol, "keyword": keyword}),
    )
    technical = _technical_summary(kline)
    risks = _risk_flags(quote, technical, news, financials)
    synthesis = await _synthesize(symbol, keyword, quote, technical, financials, news, risks, kline)
    return _build_report(symbol, quote, kline, financials, news, synthesis, started)


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
