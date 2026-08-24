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
from .gateway import gateway
from .quote import quote_indices, quote_kline, quote_realtime, quote_screener, quote_sectors

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
            err = {
                "error": {
                    "type": "gateway_internal",
                    "code": "unknown",
                    "message": f"网关内部错误: {e}",
                }
            }
            yield _sse(err, event="error")
            yield _sse_done()

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


@app.get("/healthz")
async def healthz(_=Depends(require_gateway_key)):
    return {
        "status": "ok",
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


@app.get("/quote/indices", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_indices():
    """A4：主流指数列表。GET /quote/indices"""
    return await quote_indices()


@app.get("/quote/sectors", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_sectors():
    """A4：申万板块涨跌排行。GET /quote/sectors"""
    return await quote_sectors()


@app.get("/quote/screener", dependencies=[Depends(require_gateway_key)])
async def proxy_quote_screener(industry: str = "", min_pct: float = 0.0):
    """A4：条件选股。GET /quote/screener?industry=&min_pct="""
    return await quote_screener(industry=industry, min_pct=min_pct)