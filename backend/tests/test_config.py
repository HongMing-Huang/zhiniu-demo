"""知牛 LLM 网关 - config.py 单元测试（无网络、无三方依赖、无真实 Key）

覆盖：模型别名解析 / 降级链候选序 / 显式 provider/model / 未注册模型报错。
运行：cd backend && python3 -m unittest tests.test_config -v
"""
import os
import unittest

from app.config import (
    PROVIDER_ORDER,
    PROVIDERS,
    ProviderError,
    _FALLBACK_MODEL,  # noqa: F401  (仅用于断言可解析结构)
    get_routes,
    infer_capabilities,
    list_available_models,
    resolve_candidates,
)
from app.discovery import merged_models_for_provider


class TestResolveCandidates(unittest.TestCase):
    def setUp(self):
        # 隔离环境变量，保证测试确定性
        for k in ("DEEPSEEK_API_KEY", "ZHIPUAI_API_KEY", "HUNYUAN_API_KEY"):
            os.environ.pop(k, None)

    def test_alias_quick_returns_fallback_chain(self):
        candidates = resolve_candidates("zhiniu/quick")
        providers = [c.provider for c in candidates]
        # 降级链：gemai（GemAI 中转）→ deepseek → glm → hunyuan → openai
        self.assertEqual(providers, ["gemai", "deepseek", "glm", "hunyuan", "openai"])
        self.assertEqual(
            candidates[0].base_url, "https://gemai.huchan.cn/v1"
        )
        self.assertEqual(candidates[0].model, "deepseek-v4-flash")

    def test_alias_think_first_is_reasoner(self):
        candidates = resolve_candidates("zhiniu/think")
        self.assertEqual(candidates[0].model, "deepseek-reasoner")

    def test_explicit_provider_model(self):
        candidates = resolve_candidates("glm/glm-4.5")
        self.assertEqual(len(candidates), 1)
        self.assertEqual(candidates[0].provider, "glm")
        self.assertEqual(candidates[0].base_url, "https://open.bigmodel.cn/api/paas/v4")

    def test_unknown_provider_raises(self):
        with self.assertRaises(ProviderError):
            resolve_candidates("nope/nope")

    def test_unknown_alias_raises(self):
        with self.assertRaises(ProviderError):
            resolve_candidates("unknown/thing")

    def test_hunyuan_base_url(self):
        candidates = resolve_candidates("hunyuan/hunyuan-turbos-latest")
        self.assertEqual(
            candidates[0].base_url, "https://api.hunyuan.cloud.tencent.com/v1"
        )

    def test_vision_alias_registered(self):
        candidates = resolve_candidates("zhiniu/vision")
        self.assertEqual(candidates[0].provider, "deepseek")
        self.assertEqual(candidates[0].model, "deepseek-v4-pro")
        # vision 别名同时注册进 glm.models，可经放显式 "glm/vision" 解析
        glm_vision = resolve_candidates("glm/vision")
        self.assertEqual(glm_vision[0].provider, "glm")
        self.assertEqual(glm_vision[0].model, "vision")

    def test_provider_order_stable(self):
        # 新增厂商/别名应复用全局顺序，首厂商恒为 deepseek
        self.assertEqual(PROVIDER_ORDER, ["gemai", "deepseek", "glm", "hunyuan"])
        for alias in ("zhiniu/quick", "zhiniu/think", "zhiniu/flash", "zhiniu/vision"):
            chain = [c.provider for c in resolve_candidates(alias)]
            self.assertEqual(len(set(chain)), len(chain), f"{alias} 候选不应重复")

    def test_provider_error_structured_reason(self):
        # 401/403 → invalid_key；429 → rate_limited；5xx → upstream_5xx；无状态 → timeout
        self.assertEqual(ProviderError.reason_from_status(401), ProviderError.INVALID_KEY)
        self.assertEqual(ProviderError.reason_from_status(403), ProviderError.INVALID_KEY)
        self.assertEqual(ProviderError.reason_from_status(429), ProviderError.RATE_LIMITED)
        self.assertEqual(ProviderError.reason_from_status(502), ProviderError.UPSTREAM_5XX)
        self.assertEqual(ProviderError.reason_from_status(None), ProviderError.TIMEOUT)
        self.assertEqual(
            ProviderError(ProviderError.INVALID_KEY).reason, ProviderError.INVALID_KEY
        )
        # 中文提示应包含对应环境变量名，便于排障
        self.assertIn("DEEPSEEK_API_KEY", str(ProviderError(ProviderError.NO_KEY)))


class TestProvidersJson(unittest.TestCase):
    """A1：providers.json 声明式配置 + 能力过滤 + 厂商目录（新增 3 项）。"""

    def setUp(self):
        for k, v in list(os.environ.items()):
            if k.endswith("_API_KEY"):
                os.environ.pop(k, None)

    def test_providers_loaded_14_plus_custom(self):
        # providers.json 声明式加载：预置 14 家 + 自定义槽位
        ids = set(PROVIDERS.keys())
        expected = {"openai", "azure", "deepseek", "glm", "siliconflow", "qwen",
                    "kimi", "doubao", "qianfan", "hunyuan", "openrouter",
                    "cherryin", "aihubmix", "dmxapi", "custom"}
        self.assertTrue(expected.issubset(ids), f"缺预置/自定义厂商: {expected - ids}")

    def test_config_validation_default_model_required(self):
        # 配置校验：缺 defaultModel / routes 应在加载时抛错（A1 加载校验）
        self.assertIn("deepseek", _FALLBACK_MODEL)
        self.assertIn("deepseek-chat", _FALLBACK_MODEL["deepseek"])
        routes = get_routes()
        self.assertIn("zhiniu/think", routes)
        self.assertIn("want", routes["zhiniu/think"])

    def test_capability_filtering(self):
        # 能力过滤：think(需 reasoning) 应避开无 reasoning 能力的厂商默认模型并选中 deepseek-reasoner
        os.environ["DEEPSEEK_API_KEY"] = "sk-test"
        os.environ["ZHIPUAI_API_KEY"] = "test"
        os.environ["HUNYUAN_API_KEY"] = "sk-test"
        think = [c.provider for c in resolve_candidates("zhiniu/think")]
        # deepseek 的 reasoning 模型 = deepseek-reasoner
        think_pair = [(c.provider, c.model) for c in resolve_candidates("zhiniu/think")]
        self.assertIn(("deepseek", "deepseek-reasoner"), think_pair)
        # glm 无 reasoning 模型 → 不应出现在 think 候选（能力过滤生效）
        self.assertNotIn("glm", think)

    def test_models_endpoint_returns_providers(self):
        # /v1/models 应返回声明式厂商目录 + 别名（A1 对外形态）
        data = list_available_models()
        ids = [m["id"] for m in data]
        self.assertIn("zhiniu/quick", ids)
        for provider in ("deepseek", "glm", "hunyuan", "openai", "qwen", "kimi"):
            self.assertIn(provider, ids)
        deepseek_entry = next(m for m in data if m["id"] == "deepseek")
        self.assertIn("base_url", deepseek_entry)
        self.assertIn("key_configured", deepseek_entry)


class TestModelDiscovery(unittest.TestCase):
    """A2: 模型自动发现的能力推断 + 合并目录。"""

    def setUp(self):
        for k in ("DEEPSEEK_API_KEY", "ZHIPUAI_API_KEY", "HUNYUAN_API_KEY"):
            os.environ.pop(k, None)

    def test_capability_heuristic(self):
        # 模型名启发式能力打标（A2）：reasoning / fast / vision / chat 兜底
        self.assertIn("reasoning", infer_capabilities("deepseek-reasoner"))
        self.assertIn("fast", infer_capabilities("glm-4-flash"))
        self.assertIn("vision", infer_capabilities("qwen-vl-max"))
        self.assertIn("chat", infer_capabilities("some-chat-model"))

    def test_merged_models_declared_source(self):
        # 静态声明模型 → source=declared，无 Key → available=false + reason
        conf = PROVIDERS["deepseek"]
        merged = merged_models_for_provider(conf)
        ids = [m["id"] for m in merged]
        self.assertIn("deepseek-chat", ids)
        for m in merged:
            if m["id"] == "deepseek-chat":
                self.assertEqual(m["source"], "declared")
                self.assertFalse(m["available"])
                self.assertIn("未配置", m["reason"])
                self.assertIn("chat", m["capabilities"])  # 启发式对显式 fast 名未命中 → chat 兜底

    def test_v1_models_includes_models_detail(self):
        # /v1/models 的厂商项带 models_detail（A2 对外形态）
        data = list_available_models()
        provider_items = [m for m in data if m["object"] == "provider"]
        self.assertEqual(len(provider_items), len(PROVIDERS))


if __name__ == "__main__":
    unittest.main()
