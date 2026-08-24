"""知牛 网关 - 模型自动发现（A2）

对已配置 Key 的厂商真实调用 GET {baseUrl}/models 聚合其模型清单（复用 openai SDK，
与网关同一致，不引入新 HTTP 依赖），并结合静态声明合并成统一目录；缺 Key 不静默。

接口：
- POST /admin/refresh-models（GATEWAY_API_KEY 保护）→ 触发一次发现并更新 registry
- merged_models_for_provider → 供 /v1/models 合并静态+动态模型目录
"""
from __future__ import annotations

import os
import threading
from typing import Dict, List

from openai import AsyncOpenAI

from .config import PROVIDERS, ProviderDef, infer_capabilities

# 进程内模型发现 registry：provider_id -> [model_id]
_discovered: Dict[str, List[str]] = {}
_lock = threading.Lock()


def discovered_models(provider_id: str) -> List[str]:
    with _lock:
        return list(_discovered.get(provider_id, []))


async def _fetch_provider_models(conf: ProviderDef) -> List[str]:
    """用 openai SDK 对单家调用 models.list() 返回模型 id 列表。"""
    api_key = os.getenv(conf.api_key_env)
    if not api_key or not conf.base_url:
        return []
    client = AsyncOpenAI(base_url=conf.base_url, api_key=api_key)
    try:
        resp = await client.models.list()
        return [m.id for m in resp.data]
    except Exception:
        return []


async def refresh_models() -> Dict[str, dict]:
    """对 supportsDiscover 且已配 Key 的厂商发起真实模型发现，更新 registry。

    返回 {provider: {models:[...], ok:bool, reason:str}}。
    """
    keyed = providers_by_key_refresh()
    if not keyed:
        return {}

    result: Dict[str, dict] = {}
    for conf in keyed:
        models = await _fetch_provider_models(conf)
        if not models:
            result[conf.id] = {"models": [], "ok": False, "reason": "discover failed or empty"}
            continue
        with _lock:
            _discovered[conf.id] = list(models)
        result[conf.id] = {"models": list(models), "ok": True, "reason": ""}
    return result


def providers_by_key_refresh() -> List[ProviderDef]:
    """返回 supportsDiscover 且已配 Key 的厂商（A2 模型发现对象）。"""
    return [p for p in PROVIDERS.values()
            if p.supports_discover and os.getenv(p.api_key_env)]


def merged_models_for_provider(conf: ProviderDef) -> List[dict]:
    """合并 静态声明 + 动态发现 → 模型目录项（含 available/reason/能力）。"""
    static = [m["id"] for m in conf.models]
    dynamic = discovered_models(conf.id)
    seen: Dict[str, float] = {}
    for mid in static:
        seen.setdefault(mid, 0.0)
    for mid in dynamic:
        seen.setdefault(mid, 1.0)

    key_configured = bool(os.getenv(conf.api_key_env))
    entries: List[dict] = []
    for mid, src in seen.items():
        entries.append(
            {
                "id": mid,
                "capabilities": infer_capabilities(mid),
                "available": key_configured,
                "reason": "" if key_configured else "未配置 API Key",
                "source": "declared" if src == 0.0 else "discovered",
            }
        )
    return entries