"""知牛 LLM 多模型网关 - 配置模块

真实端点（2026-08-22 官方核实，见 docs/backend-llm-gateway-design.md §2）：
- DeepSeek  https://api.deepseek.com                         OpenAI 兼容
- GLM/智谱  https://open.bigmodel.cn/api/paas/v4             OpenAI 兼容
- 腾讯混元  https://api.hunyuan.cloud.tencent.com/v1         OpenAI 兼容

所有厂商均原生 OpenAI ChatCompletions 兼容，故网关统一用 openai SDK，
仅按厂商切换 base_url + api_key (+ 透传厂商特有参数)，无业务层差异代码。
"""
from __future__ import annotations

import os
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

# 厂商官方可用模型（可按需增补）
PROVIDERS: Dict[str, dict] = {
    "deepseek": {
        "base_url": "https://api.deepseek.com",
        "api_key_env": "DEEPSEEK_API_KEY",
        "models": ["deepseek-chat", "deepseek-reasoner", "deepseek-v4-pro"],
    },
    "glm": {
        "base_url": "https://open.bigmodel.cn/api/paas/v4",
        "api_key_env": "ZHIPUAI_API_KEY",
        "models": ["glm-4", "glm-4.5", "glm-4.6"],
    },
    "hunyuan": {
        "base_url": "https://api.hunyuan.cloud.tencent.com/v1",
        "api_key_env": "HUNYUAN_API_KEY",
        "models": ["hunyuan-turbos-latest"],
    },
}

# 演示/互通默认型号（按降级链顺序）
_FALLBACK_MODEL: Dict[str, str] = {
    "deepseek": "deepseek-chat",
    "glm": "glm-4.6",
    "hunyuan": "hunyuan-turbos-latest",
}

# 统一别名 → 降级候选 [(provider, model), ...]
ALIASES: Dict[str, List[Tuple[str, str]]] = {
    "zhiniu/quick": [
        ("deepseek", "deepseek-chat"),
        ("glm", "glm-4.6"),
        ("hunyuan", "hunyuan-turbos-latest"),
    ],
    "zhiniu/think": [
        ("deepseek", "deepseek-reasoner"),
        ("glm", "glm-4.6"),
        ("hunyuan", "hunyuan-turbos-latest"),
    ],
    "zhiniu/flash": [
        ("glm", "glm-4.6"),
        ("deepseek", "deepseek-chat"),
        ("hunyuan", "hunyuan-turbos-latest"),
    ],
}


@dataclass
class ProviderResolved:
    """解析后的厂商调用配置。"""
    provider: str
    model: str
    base_url: str
    api_key: Optional[str]
    # 厂商特有扩充参数（如混元 enable_enhancement）
    extra_body: dict = field(default_factory=dict)


class ProviderError(RuntimeError):
    """厂商级错误：reason 用于区分是否可降级。"""
    FINAL = "final"        # 不可降级（Key 无效/配置错误）
    RETRYABLE = "retry"    # 可降级（限流/超时/5xx）


def build_gateway_key() -> Optional[str]:
    return os.getenv("GATEWAY_API_KEY") or None


def resolve_candidates(model_alias: str) -> List[ProviderResolved]:
    """把客户端传的 model 别名解析为有序的等价候选配置列表。

    支持两种 model 参数形式：
    - 统一别名：如 "zhiniu/quick" → 走 ALIASES 的降级链
    - 显式厂商/模型：如 "deepseek/deepseek-chat" → 单候选
    """
    if model_alias in ALIASES:
        chain = ALIASES[model_alias]
    elif "/" in model_alias:
        provider, _, model = model_alias.partition("/")
        if provider in PROVIDERS and model in PROVIDERS[provider]["models"]:
            chain = [(provider, model)]
        else:
            raise ProviderError(f"不支持的模型标识: {model_alias}", )
    else:
        # 纯模型名：按降级顺序找第一个提供该模型的厂商
        chain = [
            (p, model_alias)
            for p in PROVIDERS
            if model_alias in PROVIDERS[p]["models"]
        ]
        if not chain:
            raise ProviderError(f"未注册的模型: {model_alias}")

    resolved: List[ProviderResolved] = []
    for provider, model in chain:
        conf = PROVIDERS[provider]
        extra: dict = {}
        if provider == "hunyuan":
            # 混元扩展参数（可选演示用）
            extra = {}
        resolved.append(
            ProviderResolved(
                provider=provider,
                model=model,
                base_url=conf["base_url"],
                api_key=os.getenv(conf["api_key_env"]) or None,
                extra_body=extra,
            )
        )
    return resolved


def list_available_models() -> List[dict]:
    """GET /v1/models 返回网关已配置可用模型（含别名/厂商/是否已配 Key）。"""
    models: List[dict] = []
    for alias, chain in ALIASES.items():
        models.append(
            {
                "id": alias,
                "object": "model",
                "resolved_chain": [
                    {"provider": p, "model": m} for p, m in chain
                ],
            }
        )
    for provider, conf in PROVIDERS.items():
        models.append(
            {
                "id": provider,
                "object": "provider",
                "base_url": conf["base_url"],
                "key_configured": bool(os.getenv(conf["api_key_env"])),
                "models": conf["models"],
            }
        )
    return models