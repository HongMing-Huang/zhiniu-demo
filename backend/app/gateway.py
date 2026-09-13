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
    ProviderError,
    ProviderResolved,
    resolve_candidates,
)
from .tools import execute_tool_async, get_tools_schema


class LLMGateway:
    def __init__(self) -> None:
        # 按厂商复用 AsyncOpenAI 客户端：辩论管线一次研究要串行 7+ 次调用，
        # 每次新建客户端会重复 TLS 握手，明显拉长端到端耗时。
        self._clients: Dict[tuple, AsyncOpenAI] = {}

    def _client_for(self, base_url: str, api_key: Optional[str]) -> AsyncOpenAI:
        cache_key = (base_url, api_key or "")
        client = self._clients.get(cache_key)
        if client is None:
            # 单请求 45s 超时：中转站延迟抖动大（实测同 prompt 3s~60s），
            # 默认 600s 会让一次卡顿拖垮整条串行辩论链。
            client = AsyncOpenAI(base_url=base_url, api_key=api_key, timeout=45.0, max_retries=1)
            self._clients[cache_key] = client
        return client

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
        client = self._client_for(cand.base_url, cand.api_key)
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

    # ------------------------------------------------------------------ #
    # A3: Function Calling 工具编排（多轮 tool_calls → 执行 → 回灌 → 再请求）
    # ------------------------------------------------------------------ #

    async def chat_completions_orchestrated(
        self,
        model: str,
        messages: list,
        *,
        temperature: Optional[float] = None,
        max_tokens: Optional[int] = None,
        response_format: Optional[dict] = None,
        extra_body: Optional[dict] = None,
        max_rounds: int = 5,
    ) -> AsyncIterator[dict]:
        """带 Function Calling 的多轮工具编排，产出统一 SSE 事件流。

        每轮：
          1. 请求模型（非流式），附带 tools schema
          2. 若响应含 tool_calls → 发 agent_progress 进度帧，执行工具，回灌 role:tool，继续
          3. 直到 finish_reason != "tool_calls" 或达 MAX_TOOL_ROUNDS

        产出帧：
          - {"type":"progress","step":N,"label":工具名}   (agent_progress)
          - {"type":"delta","content":最终答复}           (最终内容)
          - {"role":"assistant",...}完整块（非流式对外兼容）
        工具执行不依赖上游，纯本地，缺 Key 也可演示编排。
        """
        tools = get_tools_schema()
        history: List[dict] = [dict(m) for m in messages]

        for round_index in range(1, max_rounds + 1):
            final = await self._call_once(
                model, history,
                tools=tools, temperature=temperature, max_tokens=max_tokens,
                response_format=response_format, extra_body=extra_body,
            )
            if final is None:
                # 全候选失败 / 无 Key → 走 Mock 兜底单次答复
                user = next((m["content"] for m in reversed(history) if m.get("role") == "user"), "")
                content = (
                    "【本地 Mock LLM · 离线兜底】\n"
                    f"> 触发原因：所有候选厂商均未配置 API Key。\n\n"
                    f"针对「{user[:40] or '（无输入）'}」的演示答复。"
                )
                yield {"type": "delta", "content": content}
                yield {"role": "assistant", "content": content, "finish_reason": "stop"}
                return

            # 检查是否需要调工具
            tool_calls = final.get("tool_calls")
            finish_reason = final.get("finish_reason")
            if not tool_calls or finish_reason == "stop":
                # 最终答复
                content = final.get("content") or ""
                yield {"type": "delta", "content": content}
                if content:
                    yield {"role": "assistant", "content": content, "finish_reason": "stop"}
                return

            # 执行本轮所有工具：助理 tool_calls 消息 + 每工具 role:tool 结果回灌
            history.append({"role": "assistant", "content": final.get("content") or None, "tool_calls": tool_calls})
            for tc in tool_calls:
                fn = tc.get("function", {})
                name = fn.get("name", "")
                try:
                    args = json.loads((fn.get("arguments") or "{}".encode()).decode() if isinstance(fn.get("arguments"), bytes) else (fn.get("arguments") or "{}"))
                except Exception:
                    args = {}
                used_tool_names.append(name)
                result = await execute_tool_async(name, args)
                yield {"type": "progress", "step": round_index, "label": name, "tool_call_id": tc.get("id")}
                history.append({
                    "role": "tool",
                    "tool_call_id": tc.get("id"),
                    "name": name,
                    "content": json.dumps(result, ensure_ascii=False),
                })

            if round_index >= max_rounds:
                # 达上限收敛
                yield {"type": "delta", "content": "（已达工具轮数上限，结束）"}
                yield {"role": "assistant", "content": "已达工具调用上限，请简化问题重试。", "finish_reason": "stop"}
                return

    async def _call_once(
        self,
        model: str,
        messages: list,
        *,
        tools: Optional[list] = None,
        temperature: Optional[float] = None,
        max_tokens: Optional[int] = None,
        response_format: Optional[dict] = None,
        extra_body: Optional[dict] = None,
    ) -> Optional[dict]:
        """单次非流式请求首选候选；全失败/无 Key 返回 None（交给上层 Mock 兜底）。"""
        candidates = resolve_candidates(model)
        used_any_key = False
        for cand in candidates:
            if not cand.api_key:
                continue
            used_any_key = True
            try:
                client = self._client_for(cand.base_url, cand.api_key)
                payload = dict(model=cand.model, messages=messages, stream=False)
                if tools:
                    payload["tools"] = tools
                    payload["tool_choice"] = "auto"
                if temperature is not None:
                    payload["temperature"] = temperature
                if max_tokens:
                    payload["max_tokens"] = max_tokens
                if response_format:
                    payload["response_format"] = response_format
                extra = dict(cand.extra_body)
                if extra_body:
                    extra.update(extra_body)
                resp = await client.chat.completions.create(**payload, extra_body=extra or None)
                msg = resp.choices[0].message
                out = {"model": cand.model, "provider": cand.provider}
                if getattr(resp.choices[0], "finish_reason", None):
                    out["finish_reason"] = str(resp.choices[0].finish_reason)
                if getattr(msg, "content", None):
                    out["content"] = msg.content
                if getattr(msg, "tool_calls", None):
                    out["tool_calls"] = [
                        {
                            "id": tc.id,
                            "type": "function",
                            "function": {"name": tc.function.name, "arguments": tc.function.arguments},
                        }
                        for tc in (msg.tool_calls or [])
                    ]
                return out
            except Exception:
                continue
        return None


gateway = LLMGateway()
