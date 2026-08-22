"""知牛 LLM 多模型网关 - LLM 调用网关层

职责：
- 对客户端暴露 OpenAI 兼容的 chat.completions（含流式 SSE 透传 / tool_calls 透传）
- 多厂商降级链：默认候选 → 下一候选 → 最终兜底纯文本
- 复用官方 openai SDK（公共组件，不自研 HTTP），仅按厂商换 base_url/api_key
"""
from __future__ import annotations

import json
from typing import AsyncIterator, Dict, Optional

from openai import AsyncOpenAI

from .config import ProviderError, ProviderResolved, resolve_candidates


class LLMGateway:
    async def chat_completions(
        self,
        model: str,
        messages: list,
        *,
        stream: bool = False,
        temperature: Optional[float] = None,
        tools: Optional[list] = None,
        tool_choice: Optional[object] = None,
        response_format: Optional[dict] = None,
        max_tokens: Optional[int] = None,
    ) -> AsyncIterator[dict]:
        """按降级链流式调用，产出统一的 OpenAI 兼容增量/事件。"""
        candidates = resolve_candidates(model)
        if not candidates:
            raise ProviderError(f"无可用的厂商候选: {model}")

        last_retryable: Optional[Exception] = None
        used_provider: Optional[ProviderResolved] = None

        for cand in candidates:
            if not cand.api_key:
                # Key 未配置：该候选跳过，不当作硬错误
                continue
            try:
                used_provider = cand
                async for chunk in self._call(
                    cand,
                    messages=messages,
                    stream=stream,
                    temperature=temperature,
                    tools=tools,
                    tool_choice=tool_choice,
                    response_format=response_format,
                    max_tokens=max_tokens,
                ):
                    # 附带当前厂商信息，便于客户端 Agent 时间线展示
                    if chunk.get("provider") is None:
                        chunk["provider"] = cand.provider
                    yield chunk
                return
            except ProviderError as e:
                if e.args and e.args[0] == ProviderError.FINAL:
                    # 不可降级（如 JSON 模式该厂商确实不支持但已配置 Key）
                    raise
                last_retryable = e
            except Exception as e:  # noqa: BLE001
                # 网络/超时/5xx 等 → 记录并走下一候选
                last_retryable = e

        # 全候选失败或无任何 Key
        if last_retryable is None:
            raise ProviderError(
                "未配置任何厂商 API Key，请在后台 .env 填入 DEEPSEEK/ZHIPUAI/HUNYUAN Key"
            )
        raise ProviderError(f"所有厂商调用失败: {last_retryable}")

    async def _call(self, cand: ProviderResolved, **kw) -> AsyncIterator[dict]:
        client = AsyncOpenAI(base_url=cand.base_url, api_key=cand.api_key)
        try:
            payload = dict(
                model=cand.model,
                messages=kw["messages"],
                stream=kw.get("stream", False),
            )
            if kw.get("temperature") is not None:
                payload["temperature"] = kw["temperature"]
            if kw.get("tools"):
                payload["tools"] = kw["tools"]
            if kw.get("tool_choice") is not None:
                payload["tool_choice"] = kw["tool_choice"]
            if kw.get("response_format"):
                payload["response_format"] = kw["response_format"]
            if kw.get("max_tokens"):
                payload["max_tokens"] = kw["max_tokens"]

            if payload["stream"]:
                stream = await client.chat.completions.create(**payload, extra_body=cand.extra_body)
                async for sse in stream:
                    yield sse.model_dump(exclude_none=True)
            else:
                resp = await client.chat.completions.create(**payload, extra_body=cand.extra_body)
                yield resp.model_dump(exclude_none=True)
        except Exception as e:
            status = getattr(getattr(e, "status_code", None), None, None)
            if status == 401 or status == 403:
                raise ProviderError(ProviderError.FINAL) from e
            raise ProviderError(ProviderError.RETRYABLE) from e


gateway = LLMGateway()