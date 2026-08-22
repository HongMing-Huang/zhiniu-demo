"""知牛 LLM 多模型网关 - LLM 调用网关层

职责：
- 对客户端暴露 OpenAI 兼容的 chat.completions（含流式 SSE 透传 / tool_calls 透传）
- 多厂商降级链：默认候选 → 下一候选 → 最终全部失败走本地 Mock LLM 兜底（绝不弹 401/500）
- 复用官方 openai SDK（公共组件，不自研 HTTP），仅按厂商换 base_url/api_key
"""
from __future__ import annotations

import json
from typing import AsyncIterator, Dict, List, Optional

from openai import AsyncOpenAI

from .config import (
    PROVIDERS,
    ProviderError,
    ProviderResolved,
    resolve_candidates,
)


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
        extra_body: Optional[dict] = None,
    ) -> AsyncIterator[dict]:
        """按降级链流式调用，产出统一的 OpenAI 兼容增量/事件。

        全部候选失败或未配置任何 Key 时，落地到本地 Mock LLM 预置答复，
        保证演示链路不因缺 Key / 上游故障而白屏或抛 401/500。
        """
        candidates: List[ProviderResolved] = []
        try:
            candidates = resolve_candidates(model)
        except ProviderError:
            # 未注册模型：无 Mock 可兜底，交给上层产出错误帧
            raise
        if not candidates:
            raise ProviderError(ProviderError.UNKNOWN, f"无可用的厂商候选: {model}")

        last_retryable: Optional[ProviderError] = None
        used_any_key = False

        for cand in candidates:
            if not cand.api_key:
                # Key 未配置：该候选跳过，不当作硬错误
                continue
            used_any_key = True
            try:
                async for chunk in self._call(
                    cand,
                    messages=messages,
                    stream=stream,
                    temperature=temperature,
                    tools=tools,
                    tool_choice=tool_choice,
                    response_format=response_format,
                    max_tokens=max_tokens,
                    extra_body=extra_body,
                ):
                    # 附带当前厂商信息，便于客户端 Agent 时间线展示
                    if chunk.get("provider") is None:
                        chunk["provider"] = cand.provider
                    yield chunk
                return
            except ProviderError as e:
                if e.reason in ProviderError.RETRYABLE_REASONS:
                    # 可降级（限流/超时/5xx）→ 记录并走下一候选
                    last_retryable = e
                    continue
                # 不可降级（Key 无效）→ 走 Mock 兜底，不让客户端白屏
                last_retryable = e
                break
            except Exception as e:  # noqa: BLE001
                # 网络/超时/5xx 等 → 记录并走下一候选
                last_retryable = ProviderError(
                    ProviderError.UNKNOWN, f"调用厂商时发生未知网络错误: {e}"
                )

        # 全候选失败 / 未配置任何 Key → 本地 Mock LLM 元兜底（绝不直接抛异常）
        if not used_any_key or last_retryable is not None:
            no_key = not used_any_key
            async for chunk in self._mock_llm(
                messages=messages,
                model=model,
                stream=stream,
                no_key=no_key,
                fallback_reason=last_retryable.reason if last_retryable else None,
            ):
                yield chunk
            return

    # ------------------------------------------------------------------ #

    async def _mock_llm(
        self,
        messages: list,
        model: str,
        *,
        stream: bool,
        no_key: bool,
        fallback_reason: Optional[str],
    ) -> AsyncIterator[dict]:
        """将预置 Mock LLM 答复按 stream 模式产出统一增量/对象（离线兜底，绝不抛异常）。"""
        user = next(
            (m.get("content") or "" for m in reversed(messages) if m.get("role") == "user"),
            "",
        )
        snippet = user.strip().replace("\n", " ")[:40] or "（无输入）"
        content = (
            "【本地 Mock LLM · 离线兜底】\n"
            f"> 触发原因：{'所有厂商均未配置 API Key（.env 缺 DEEPSEEK/ZHIPUAI/HUNYUAN Key）' if no_key else '已配置候选厂商调用失败，已降级到本地兜底'}。\n\n"
            f"针对「{snippet}」的演示回复如下（预置 JSON/Markdown 结构）：\n\n"
            "A股行情仍以实时数据为准，建议关注均线收敛与量能配合；本回复为离线预置占位，用于演示链路兜底。"
        )

        if not stream:
            yield {
                "id": "mock-llm",
                "object": "chat.completion",
                "model": model,
                "choices": [
                    {
                        "index": 0,
                        "message": {"role": "assistant", "content": content},
                        "finish_reason": "stop",
                    }
                ],
                "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2},
            }
            return

        # 流式：首 chunk 带空 content 的 role=assistant，随后逐字产出，末 chunk 带 usage
        yield {
            "id": "mock-llm",
            "object": "chat.completion.chunk",
            "model": model,
            "choices": [{"index": 0, "delta": {"role": "assistant"}, "finish_reason": None}],
        }
        _chunks = [content[:50]] + [
            content[i : i + 60] for i in range(50, len(content), 60)
        ]
        for i, frag in enumerate(_chunks):
            last = frag.endswith("兜底。")
            yield {
                "id": "mock-llm",
                "object": "chat.completion.chunk",
                "model": model,
                "choices": [
                    {
                        "index": 0,
                        "delta": {"content": frag},
                        "finish_reason": "stop" if last else None,
                    }
                ],
                "usage": (
                    {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}
                    if last
                    else None
                ),
            }

    async def _call(
        self,
        cand: ProviderResolved,
        **kw,
    ) -> AsyncIterator[dict]:
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

            extra = dict(cand.extra_body)
            if kw.get("extra_body"):
                extra.update(kw["extra_body"])
            if not extra:
                extra = None

            if payload["stream"]:
                stream = await client.chat.completions.create(**payload, extra_body=extra)
                async for sse in stream:
                    yield sse.model_dump(exclude_none=True)
            else:
                resp = await client.chat.completions.create(**payload, extra_body=extra)
                yield resp.model_dump(exclude_none=True)
        except Exception as e:  # noqa: BLE001
            status = getattr(getattr(e, "status_code", None), "value", None)
            reason = ProviderError.reason_from_status(status)
            raise ProviderError(reason, message=None) from e


gateway = LLMGateway()