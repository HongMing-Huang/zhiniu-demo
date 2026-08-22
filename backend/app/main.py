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
from fastapi.responses import StreamingResponse
from openai import AsyncOpenAI
from pydantic import BaseModel, Field

from .config import (
    PROVIDERS,
    ProviderError,
    build_gateway_key,
    list_available_models,
)
from .gateway import gateway

load_dotenv(override=True)

app = FastAPI(title="知牛 ZhiNiu LLM Gateway", version="0.1.0")


# ---------- 鉴权（网关自身 Key；厂商 Key 只在服务端） ----------
def require_gateway_key(authorization: str = Header(None)) -> None:
    gw_key = build_gateway_key()
    if not gw_key or gw_key == "changeme":
        # 未设置：演示期放行，但生产必须配置
        return
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


# ---------- SSE 工具 ----------
def _sse(data: dict) -> str:
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
            stream=True,
            temperature=body.temperature,
            tools=body.tools,
            tool_choice=body.tool_choice,
            response_format=body.response_format,
            max_tokens=body.max_tokens,
        )

    async def event_stream():
        try:
            async for chunk in gateway.chat_completions(**_build_payload()):
                yield _sse(chunk)
            yield _sse_done()
        except ProviderError as e:
            err = {
                "error": {
                    "type": "gateway_upstream",
                    "message": str(e),
                }
            }
            yield _sse(err)
            yield _sse_done()

    return StreamingResponse(event_stream(), media_type="text/event-stream")


@app.get("/v1/models", dependencies=[Depends(require_gateway_key)])
async def models():
    return {"object": "list", "data": list_available_models()}


@app.get("/healthz")
async def healthz(_=Depends(require_gateway_key)):
    return {
        "status": "ok",
        "providers": {
            name: {
                "base_url": conf["base_url"],
                "key_configured": bool(__import__("os").getenv(conf["api_key_env"])),
            }
            for name, conf in PROVIDERS.items()
        },
    }