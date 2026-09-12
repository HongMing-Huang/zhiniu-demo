"""知牛 LLM 多模型网关 - 配置模块（providers.json 声明式）

所有厂商基址/模型/路由能力均来自 providers.json（见同目录），本模块负责:
- 加载并校验 providers.json（缺 defaultModel / routes 即抛错）
- 解析模型别名/显式 provider/model/纯模型名 → 有序候选列表（resolve_candidates）
- 提供 providers 目录与能力过滤（供 /v1/models 与 A2 模型发现使用）

真实端点（2026-08-22 官方核实，见 docs/llm-router-design.md §7.1 / backend-llm-gateway-design.md §2）：
- DeepSeek  https://api.deepseek.com
- GLM/智谱  https://open.bigmodel.cn/api/paas/v4
- 腾讯混元  https://api.hunyuan.cloud.tencent.com/v1
其余服务商基址见 providers.json（协议均为 OpenAI 兼容，网关统一用 openai SDK）。
"""
from __future__ import annotations

import json
import os
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Tuple

_CONFIG_PATH = Path(__file__).resolve().parent / "providers.json"


@dataclass
class ProviderDef:
    """单家服务商配置（读自 providers.json）。"""
    id: str
    name: str
    base_url: str
    api_key_env: str
    protocol: str = "openai"
    supports_discover: bool = False
    tier: str = ""
    requires_endpoint: bool = False
    key_hint: str = ""
    models: List[dict] = field(default_factory=list)   # [{id, capabilities}]
    extra_body: Dict[str, dict] = field(default_factory=dict)  # 厂商级固定扩展参数（如关闭思考模式）


def _load_providers() -> dict:
    """加载并校验 providers.json。缺 defaultModel / routes 即抛错。"""
    try:
        data = json.loads(_CONFIG_PATH.read_text(encoding="utf-8"))
    except FileNotFoundError as e:
        raise RuntimeError(f"缺少 providers.json: {_CONFIG_PATH}") from e
    except json.JSONDecodeError as e:
        raise RuntimeError(f"providers.json JSON 解析失败: {e}") from e

    if not data.get("defaultModel"):
        raise RuntimeError("providers.json 缺少 defaultModel（每家默认模型），A1 加载校验失败")
    if not data.get("routes"):
        raise RuntimeError("providers.json 缺少 routes（能力路由定义），A1 加载校验失败")
    if not data.get("providers"):
        raise RuntimeError("providers.json 缺少 providers 数组")
    return data


_DATA = _load_providers()

# 全局厂商降级顺序（兼容旧 PROVIDER_ORDER 引用）
PROVIDER_ORDER: List[str] = list(_DATA.get("order", []))


def _provider_defs() -> Dict[str, ProviderDef]:
    defs: Dict[str, ProviderDef] = {}
    for p in _DATA["providers"]:
        defs[p["id"]] = ProviderDef(
            id=p["id"],
            name=p.get("name", p["id"]),
            base_url=p.get("baseUrl", ""),
            api_key_env=p.get("apiKeyEnv", f'{p["id"].upper()}_API_KEY'),
            protocol=p.get("protocol", "openai"),
            supports_discover=bool(p.get("supportsDiscover", False)),
            tier=p.get("tier", ""),
            requires_endpoint=bool(p.get("requiresEndpoint", False)),
            key_hint=p.get("keyHint", ""),
            models=p.get("models", []),
            extra_body=dict(p.get("extraBody") or {}),
        )
    return defs


PROVIDERS: Dict[str, ProviderDef] = _provider_defs()

# 模型名启发式 → 能力（§7.4；route 能力过滤与自动打标签用）
MODEL_CAPABILITY_PATTERNS: Dict[str, List[str]] = {
    "reasoning": ["reasoner", "thinking", "o1", "o3", "r1"],
    "vision": ["vision", "-vl", "4v", "4o"],
    "fast": ["-mini", "-flash", "-lite"],
    "long_context": ["32k", "128k", "200k", "-long"],
}


def _capabilities_of(provider_id: str, model_id: str) -> List[str]:
    """返回模型能力标签：优先 providers.json 显式声明，否则按模型名启发式推断。"""
    for p in _DATA["providers"]:
        if p["id"] == provider_id:
            for m in p.get("models", []):
                if m["id"] == model_id:
                    return list(m.get("capabilities", []))
            break
    caps: List[str] = []
    low = model_id.lower()
    for capability, keys in MODEL_CAPABILITY_PATTERNS.items():
        if any(k in low for k in keys):
            caps.append(capability)
    caps.append("chat")  # 兜底
    return caps


_TRACKING: set = set()  # 记录 resolve_candidates 是否已运行（仅供 import 时避免副作用断言）


@dataclass
class ProviderResolved:
    """解析后的厂商调用配置。"""
    provider: str
    model: str
    base_url: str
    api_key: Optional[str]
    extra_body: dict = field(default_factory=dict)


class ProviderError(RuntimeError):
    """厂商级错误：reason 为结构化原因码，message 含对应中文提示（含 env 变量名）。"""

    NO_KEY = "no_key_configured"
    INVALID_KEY = "invalid_key"
    RATE_LIMITED = "rate_limited"
    TIMEOUT = "timeout"
    UPSTREAM_5XX = "upstream_5xx"
    UNKNOWN = "unknown"

    RETRYABLE_REASONS = {RATE_LIMITED, TIMEOUT, UPSTREAM_5XX}

    def __init__(self, reason: str = UNKNOWN, message: Optional[str] = None):
        super().__init__(message or PROVIDER_ERROR_MESSAGES.get(reason, reason))
        self.reason = reason

    FINAL = "unknown"
    RETRYABLE = "retry"

    @staticmethod
    def reason_from_status(status: Optional[int]) -> str:
        if status in (401, 403):
            return ProviderError.INVALID_KEY
        if status == 429:
            return ProviderError.RATE_LIMITED
        if status and status >= 500:
            return ProviderError.UPSTREAM_5XX
        if status is None:
            return ProviderError.TIMEOUT
        return ProviderError.UNKNOWN


PROVIDER_ERROR_MESSAGES: Dict[str, str] = {
    ProviderError.NO_KEY: "所有厂商均未配置 API Key，请在后台 .env 填入 DEEPSEEK_API_KEY / ZHIPUAI_API_KEY / HUNYUAN_API_KEY（任一即可）",
    ProviderError.INVALID_KEY: "厂商 Key 无效或已过期（401/403）。请检查对应厂商 .env 中的 Key",
    ProviderError.RATE_LIMITED: "厂商接口限流（429）。请稍后重试，或等待 1 分钟冷却后再试",
    ProviderError.TIMEOUT: "厂商接口请求超时。请检查网络，或稍后重试",
    ProviderError.UPSTREAM_5XX: "厂商服务端异常（5xx）。已自动切换下一候选，若全部失败请稍后重试",
    ProviderError.UNKNOWN: "网关调用厂商时发生未知错误，请查看后端日志",
}


def build_gateway_key() -> Optional[str]:
    return os.getenv("GATEWAY_API_KEY") or None


def _fallback_model() -> Dict[str, str]:
    """每家默认模型（兼容旧 _FALLBACK_MODEL 引用）。"""
    return dict(_DATA.get("defaultModel", {}))


_FALLBACK_MODEL: Dict[str, str] = _fallback_model()


def _resolve_route(alias: str) -> List[Tuple[str, str]]:
    """按 routes 段解析别名 → 有序候选 [(provider, model)]，候选按 want 能力过滤。

    对每家 fallbackOrder 里的 provider：遍历其 models，选第一个能力覆盖 want 的模型；
    want 需要 reasoning 而该家无 reasoning 模型 → 跳过这家。缺 Key 不 skip（保留候选，
    由 gateway 层 Mock 兜底），保证旧单测链序稳定。
    """
    route = _DATA["routes"].get(alias)
    if not route:
        raise ProviderError(ProviderError.UNKNOWN, f"未注册的别名: {alias}")

    want = set(route.get("want", []))
    fallback_order: List[str] = route.get("fallbackOrder") or []

    chain: List[Tuple[str, str]] = []
    for provider_id in fallback_order:
        conf = PROVIDERS.get(provider_id)
        if not conf:
            continue
        provider_models = _provider_models(provider_id)
        if not provider_models:
            continue
        # 能力过滤（A1）：want 非空时，仅当该家有模型能力覆盖 want 才选入候选
        if want:
            picked: Optional[str] = None
            for mid in provider_models:
                if want & set(_capabilities_of(provider_id, mid)):
                    picked = mid
                    break
            if picked is None:
                # 该家无匹配 want 能力的模型 → 跳过整家（不得回退默认模型，否则绕过过滤）
                continue
            chain.append((provider_id, picked))
        else:
            # want 为空 → 用默认模型
            default_model = _fallback_model().get(provider_id)
            if default_model is not None:
                chain.append((provider_id, default_model))
    if not chain:
        raise ProviderError(ProviderError.NO_KEY, "无可用的厂商候选")
    return chain


def _provider_models(provider_id: str) -> List[str]:
    conf = PROVIDERS.get(provider_id)
    if not conf:
        return []
    return [m["id"] for m in conf.models]


def resolve_candidates(model_alias: str) -> List[ProviderResolved]:
    """把客户端传的 model 别名解析为有序的等价候选配置列表。

    支持三种形式：
    - 统一别名（如 "zhiniu/quick"）→ routes 段能力路由 + 降级链
    - 显式厂商/模型（如 "deepseek/deepseek-chat"）→ 单候选
    - 纯模型名（如 "deepseek-chat"）→ 按声明顺序找第一个提供的厂商
    """
    if model_alias in _DATA["routes"]:
        chain = _resolve_route(model_alias)
    elif "/" in model_alias:
        provider, _, model = model_alias.partition("/")
        if provider in PROVIDERS and model in _provider_models(provider):
            chain = [(provider, model)]
        else:
            raise ProviderError(ProviderError.UNKNOWN, f"不支持的模型标识: {model_alias}")
    else:
        chain = [
            (p, model_alias)
            for p in PROVIDERS
            if model_alias in _provider_models(p)
        ]
        if not chain:
            raise ProviderError(ProviderError.UNKNOWN, f"未注册的模型: {model_alias}")

    resolved: List[ProviderResolved] = []
    for provider, model in chain:
        conf = PROVIDERS[provider]
        extra: dict = dict(conf.extra_body)
        resolved.append(
            ProviderResolved(
                provider=provider,
                model=model,
                base_url=conf.base_url,
                api_key=os.getenv(conf.api_key_env) or None,
                extra_body=extra,
            )
        )
    return resolved


def list_available_models() -> List[dict]:
    """GET /v1/models 返回网关已配置可用模型（含别名/厂商/能力/是否已配 Key）。"""
    models: List[dict] = []
    for alias, route in _DATA["routes"].items():
        models.append(
            {
                "id": alias,
                "object": "model",
                "want": route.get("want", []),
                "fallback_order": route.get("fallbackOrder", []),
                "resolved_chain": resolve_candidates_maybe(alias),
            }
        )
    for provider, conf in PROVIDERS.items():
        models.append(
            {
                "id": provider,
                "object": "provider",
                "name": conf.name,
                "base_url": conf.base_url,
                "tier": conf.tier,
                "supports_discover": conf.supports_discover,
                "requires_endpoint": conf.requires_endpoint,
                "key_configured": bool(os.getenv(conf.api_key_env)),
                "key_hint": conf.key_hint,
                "models": _provider_models(provider),
            }
        )
    return models


def resolve_candidates_maybe(model_alias: str) -> List[dict]:
    """与 resolve_candidates 同解析，但失败时返回空链而非抛错（供列表展示）。"""
    try:
        return [
            {"provider": c.provider, "model": c.model} for c in resolve_candidates(model_alias)
        ]
    except ProviderError:
        return []


def get_routes() -> Dict[str, dict]:
    """A2/A6 等用：能力路由定义。"""
    return dict(_DATA.get("routes", {}))


def get_default_model(provider_id: str) -> Optional[str]:
    """A2 模型发现聚合 后用于 fallback。"""
    return _fallback_model().get(provider_id)


def infer_capabilities(model_id: str) -> List[str]:
    """A2：按模型名启发式推断能力标签（无需显式声明）。"""
    return _capabilities_of("", model_id).copy()


def providers_by_key() -> List[ProviderDef]:
    """只返回已配置了 Key 的厂商（供模型自动发现拉取真实模型清单）。"""
    return [p for p in PROVIDERS.values() if os.getenv(p.api_key_env)]