"""知牛 LLM 多模型网关 - FastAPI 入口

对外 OpenAI 兼容协议（客户端只认这一份，与底层厂商无关）：
- POST /v1/chat/completions   聊天/诊股/多 Agent 编排（流式 SSE）
- GET  /v1/models             可用模型列表
- GET  /healthz               存活 + 各厂商连通性/Key 配置自检
启动：cd backend && uvicorn app.main:app --host 0.0.0.0 --port 8000
"""
from __future__ import annotations

import asyncio
import json
import os
import time
from typing import Optional

from dotenv import load_dotenv
from fastapi import Depends, FastAPI, Header, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from openai import AsyncOpenAI
from pydantic import BaseModel, Field

from .config import (
    PROVIDERS,
    ProviderError,
    build_gateway_key,
    list_available_models,
)
from .discovery import merged_models_for_provider, refresh_models
from .analytics import record_usage, usage_summary
from .agent import (
    run_chat,
    run_compare,
    run_insight,
    run_research,
    stream_chat,
    stream_compare,
    stream_research,
)
from . import store
from .tools import execute_tool_async
from .gateway import gateway
from .news import news_detail, news_list
from .quote import (
    quote_fundamentals,
    quote_indices,
    quote_kline,
    quote_popularity,
    quote_realtime,
    quote_screener,
    quote_search,
    quote_sectors,
)

load_dotenv(override=True)

app = FastAPI(title="知牛 ZhiNiu LLM Gateway", version="0.1.0")

# Web(H5)/小程序 跨端调用网关需 CORS（Web 端硬约束#4）。
# 演示期 allow_origins=["*"]；生产可收紧为白名单域名列表。
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,  # "*" 不能与 credentials=True 并存
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------- 鉴权（网关自身 Key；厂商 Key 只在服务端） ----------
def require_gateway_key(authorization: str = Header(None)) -> None:
    gw_key = build_gateway_key()
    if not gw_key or gw_key == "changeme":
        # 未设置：演示期放行，但生产必须配置
        return
    if authorization != f"Bearer {gw_key}":
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "网关 Key 无效")


def _any_key_configured() -> bool:
    """是否任一厂商已配置 Key（A6 判定是否走降级/Mock）。"""
    return any(os.getenv(p.api_key_env) for p in PROVIDERS.values())


def require_admin_key(authorization: str = Header(None)) -> None:
    """A2：管理端点强制要求 GATEWAY_API_KEY（未配置则拒绝写操作）。"""
    gw_key = build_gateway_key()
    if not gw_key or gw_key == "changeme":
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            "管理端点在未配置 GATEWAY_API_KEY 时不可用（演示期仅只读）",
        )
    if authorization != f"Bearer {gw_key}":
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "网关 Key 无效")


# ---------- 请求体 ----------
class ChatMessage(BaseModel):
    role: str
    content: Optional[str] = None
    tool_calls: Optional[list] = None
    tool_call_id: Optional[str] = None


class ChatCompletionRequest(BaseModel):
    model: str = Field(..., description="统一别名，如 zhiniu/quick")
    messages: list[ChatMessage]
    stream: bool = True
    temperature: Optional[float] = None
    max_tokens: Optional[int] = None
    tools: Optional[list] = None
    tool_choice: Optional[object] = None
    response_format: Optional[dict] = None
    extra_body: Optional[dict] = None  # 透传厂商特有参数


class AgentResearchRequest(BaseModel):
    symbol: str = Field(..., min_length=8, max_length=12, description="如 sh600519")
    keyword: str = Field("", max_length=80)
    request_id: str = Field("", max_length=64, description="可选幂等键：重复提交返回同一结果")


class AgentChatRequest(BaseModel):
    question: str = Field(..., min_length=1, max_length=300)
    history: list[dict] = Field(default_factory=list, description='[{"role":"user|ai","content":"..."}]，最多 20 条')
    symbol: str = Field("", max_length=12, description="可选上下文标的，如 sh600519")


class AgentCompareRequest(BaseModel):
    symbols: list[str] = Field(..., min_items=2, max_items=2, description="两个对比标的，如 ['sh600519','sz300750']")
    focus: str = Field("", max_length=80, description="可选关注点，如「估值与趋势」")


# ---------- SSE 工具（支持 event+data 双行格式） ----------
def _sse(data: dict, event: Optional[str] = None) -> str:
    if event:
        return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"
    return f"data: {json.dumps(data, ensure_ascii=False)}\n\n"


def _sse_done() -> str:
    return "data: [DONE]\n\n"


# ---------- 入口 ----------
@app.post("/v1/chat/completions")
async def chat_completions(
    body: ChatCompletionRequest,
    _=Depends(require_gateway_key),
):
    def _build_payload():
        return dict(
            model=body.model,
            messages=[m.model_dump(exclude_none=True) for m in body.messages],
            stream=body.stream,  # 读请求体，不硬编码
            temperature=body.temperature,
            tools=body.tools,
            tool_choice=body.tool_choice,
            response_format=body.response_format,
            max_tokens=body.max_tokens,
            extra_body=body.extra_body,
        )

    async def event_stream():
        payload = _build_payload()
        _t0 = time.perf_counter()
        _error_code: Optional[str] = None
        _degraded = _any_key_configured() is False
        try:
            if payload.get("tools"):
                # A3：带工具 → 走 Function Calling 编排（内部多轮，对外发 progress/delta 帧）
                async for frame in gateway.chat_completions_orchestrated(
                    model=payload.get("model"),
                    messages=payload.get("messages", []),
                    temperature=payload.get("temperature"),
                    max_tokens=payload.get("max_tokens"),
                    response_format=payload.get("response_format"),
                    extra_body=payload.get("extra_body"),
                ):
                    ftype = frame.get("type")
                    if ftype == "progress":
                        # 工具调用进度 → SSE event:agent_progress 帧
                        yield _sse(
                            {"step": frame.get("step"), "label": frame.get("label"), "tool_call_id": frame.get("tool_call_id")},
                            event="agent_progress",
                        )
                    elif ftype == "delta":
                        yield _sse(
                            {
                                "id": "orchestrated",
                                "object": "chat.completion.chunk",
                                "model": payload.get("model"),
                                "choices": [{"index": 0, "delta": {"content": frame.get("content")}, "finish_reason": None}],
                            }
                        )
                    else:
                        # 最终 assistant 完整块
                        yield _sse(
                            {
                                "id": "orchestrated",
                                "object": "chat.completion",
                                "model": payload.get("model"),
                                "choices": [{"index": 0, "message": {"role": "assistant", "content": frame.get("content")}, "finish_reason": frame.get("finish_reason", "stop")}],
                            }
                        )
            else:
                async for chunk in gateway.chat_completions(**payload):
                    yield _sse(chunk)
            yield _sse_done()
        except ProviderError as e:
            _error_code = e.reason
            err = {
                "error": {
                    "type": "gateway_upstream",
                    "code": e.reason,
                    "message": str(e),
                }
            }
            yield _sse(err, event="error")
            yield _sse_done()
        except Exception as e:  # 最外层兜底：任何异常都走 SSE 错误帧 + [DONE]，禁止 500 半条流
            _error_code = "unknown"
            err = {
                "error": {
                    "type": "gateway_internal",
                    "code": "unknown",
                    "message": f"网关内部错误: {e}",
                }
            }
            yield _sse(err, event="error")
            yield _sse_done()
        finally:
            # A6：记录用量（模型/延迟/降级/错误）供 /analytics/usage 聚合
            try:
                record_usage(
                    model=payload.get("model", "unknown"),
                    latency_ms=(time.perf_counter() - _t0) * 1000,
                    degraded=_degraded,
                    error=_error_code,
                )
            except Exception:
                pass

    return StreamingResponse(event_stream(), media_type="text/event-stream")


@app.get("/v1/models", dependencies=[Depends(require_gateway_key)])
async def models():
    # 别名 + 厂商目录；每个厂商带静态+动态模型合并（含能力/available/reason）
    data = list_available_models()
    for item in data:
        if item.get("object") == "provider":
            conf = PROVIDERS.get(item["id"])
            if conf:
                item["models_detail"] = merged_models_for_provider(conf)
    return {"object": "list", "data": data}


@app.post("/admin/refresh-models", dependencies=[Depends(require_admin_key)])
async def admin_refresh_models():
    """A2：对已配 Key 厂商真实调 GET {baseUrl}/models 聚合模型清单（GATEWAY_API_KEY 保护）。"""
    result = await refresh_models()
    if not result:
        return {
            "status": "no_keyed_provider",
            "message": "无已配置 Key 的厂商，无法发现；返回 Mock/静态目录",
            "providers": {},
        }
    return {"status": "ok", "providers": result}


@app.on_event("startup")
async def _init_store() -> None:
    await store.init()


@app.get("/healthz")
async def healthz(_=Depends(require_gateway_key)):
    configured = [name for name, conf in PROVIDERS.items() if os.getenv(conf.api_key_env)]
    return {
        "status": "ok",
        "database": store.status(),
        "services": {
            "market": {"status": "ready", "provider": "sina+eastmoney"},
            "agent": {
                "status": "ready" if configured else "degraded",
                "mode": "llm" if configured else "deterministic_fallback",
                "configuredProviders": configured,
            },
        },
        "providers": {
            name: {
                "base_url": conf.base_url,
                "key_configured": bool(__import__("os").getenv(conf.api_key_env)),
            }
            for name, conf in PROVIDERS.items()
        },
    }


# ---------- 行情代理（多端统一走后端网关，规避新浪 Referer/GBK/CORS） ----------
@app.get("/quote/realtime", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_realtime(codes: str):
    """实时行情代理：GET /quote/realtime?codes=sh600519,sz000001"""
    result, was_stale = await quote_realtime(
        [c.strip() for c in codes.split(",") if c.strip()]
    )
    return JSONResponse(content=result, headers={"X-Gateway-Stale": "true"} if was_stale else None)


@app.get("/quote/kline", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_kline(
    symbol: str,
    scale: int = 240,
    datalen: int = 120,
):
    """K 线代理：GET /quote/kline?symbol=sh600519&scale=240&datalen=120"""
    return await quote_kline(symbol, scale=scale, datalen=datalen)


@app.get("/quote/fundamentals", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_fundamentals(symbol: str):
    """估值、最近一期财报和盘中资金流；缺失字段保持为空。"""
    return await quote_fundamentals(symbol)


@app.get("/quote/indices", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_indices():
    """A4：主流指数列表。GET /quote/indices"""
    return await quote_indices()


@app.get("/quote/sectors", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_sectors():
    """A4：东财行业板块涨跌排行（真实 + stale/mock 兜底）。GET /quote/sectors"""
    return await quote_sectors()


@app.get("/quote/screener", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_screener(industry: str = "", min_pct: float = 0.0, limit: int = 30):
    """A4：条件选股。GET /quote/screener?industry=&min_pct=&limit="""
    return await quote_screener(industry=industry, min_pct=min_pct, limit=limit)


@app.get("/quote/popularity", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_popularity(count: int = 20):
    """A4：东财人气榜（真实排名 + 新浪实时增强）。GET /quote/popularity?count="""
    return await quote_popularity(count=count)


@app.get("/quote/search", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_search(keyword: str, count: int = 10):
    """全市场 A 股搜索（东财 suggest，名称/代码/拼音；离线回退本地快照）。GET /quote/search?keyword=&count="""
    return await quote_search(keyword=keyword, count=count)


@app.get("/news/list", dependencies=[Depends(require_gateway_key)])
async def proxy_news_list(keyword: str = "", symbol: str = ""):
    """A5：资讯列表（7×24 快讯 + 个股新闻）。GET /news/list?keyword=&symbol="""
    return await news_list(keyword=keyword, symbol=symbol)


@app.get("/news/detail", dependencies=[Depends(require_gateway_key)])
async def proxy_news_detail(news_id: str):
    """A5：资讯详情。GET /news/detail?news_id="""
    return await news_detail(news_id)


@app.post("/agent/research", dependencies=[Depends(require_gateway_key)])
async def agent_research(body: AgentResearchRequest):
    """可审计的多阶段研究管线；无 LLM Key 也能返回真实/降级数据证据。"""
    return await run_research(symbol=body.symbol.lower(), keyword=body.keyword)


@app.post("/agent/chat", dependencies=[Depends(require_gateway_key)])
async def agent_chat(body: AgentChatRequest):
    """通用问答（快捷指令/概念解释/方法论）：走 LLM 网关，无模型显式规则降级。

    history 每条最多保留 500 字、取最近 8 轮（run_chat 内裁剪）。
    """
    quote = None
    symbol = body.symbol.strip().lower()
    if symbol:
        quote = await execute_tool_async("get_realtime_quote", {"symbol": symbol}) or None
    return await run_chat(
        question=body.question[:300],
        history=body.history[:20],
        symbol=symbol,
        quote=quote or None,
    )


@app.post("/agent/research/stream", dependencies=[Depends(require_gateway_key)])
async def agent_research_stream(body: AgentResearchRequest):
    """类型化 SSE：阶段、结果和终止帧，供多端渲染可恢复的研究进度。"""
    async def event_stream():
        async for frame in stream_research(symbol=body.symbol.lower(), keyword=body.keyword):
            yield _sse(frame, event=frame["type"])

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@app.post("/agent/insight", dependencies=[Depends(require_gateway_key)])
async def agent_insight(body: AgentResearchRequest):
    """个股 AI 诊股：服务端取证 → 单次 LLM 结构化判断（买/卖观察区间 + 风险五档 + 信号）→
    规则降级兜底；结果缓存 + 同标的并发去重（对标课题组 SaiRen stock-analyses 模式）。"""
    return await run_insight(symbol=body.symbol.lower())


@app.post("/agent/compare", dependencies=[Depends(require_gateway_key)])
async def agent_compare(body: AgentCompareRequest):
    """双股对比：双侧真实证据（行情/技术面/估值）→ LLM 定性对比；规则基准始终返回。"""
    symbols = [s.strip().lower() for s in body.symbols if s.strip()]
    return await run_compare(symbols=symbols, focus=body.focus.strip())


@app.post("/agent/compare/stream", dependencies=[Depends(require_gateway_key)])
async def agent_compare_stream(body: AgentCompareRequest):
    """双股对比流式版：stage（双侧证据 / AI 归纳）→ result → 终帧（LLM 延迟抖动下可见进度）。"""
    symbols = [s.strip().lower() for s in body.symbols if s.strip()]

    async def event_stream():
        async for frame in stream_compare(symbols=symbols, focus=body.focus.strip()):
            yield _sse(frame, event=frame["type"])

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@app.post("/agent/chat/stream", dependencies=[Depends(require_gateway_key)])
async def agent_chat_stream(body: AgentChatRequest):
    """通用问答流式版：chat_started → delta* → chat_finished（逐字上屏）。"""
    quote = None
    symbol = body.symbol.strip().lower()
    if symbol:
        quote = await execute_tool_async("get_realtime_quote", {"symbol": symbol}) or None

    async def event_stream():
        async for frame in stream_chat(
            question=body.question[:300],
            history=body.history[:20],
            symbol=symbol,
            quote=quote or None,
        ):
            yield _sse(frame, event=frame["type"])

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@app.get("/analytics/usage", dependencies=[Depends(require_gateway_key)])
async def proxy_usage(hours: int = 1):
    """A6：用量统计（按模型/时段的请求/延迟/降级/错误分布）。GET /analytics/usage?hours="""
    return usage_summary(hours=hours)
