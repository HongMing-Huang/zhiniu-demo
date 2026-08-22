"""知牛 LLM 网关 - config.py 单元测试（无网络、无三方依赖、无真实 Key）

覆盖：模型别名解析 / 降级链候选序 / 显式 provider/model / 未注册模型报错。
运行：cd backend && python3 -m unittest tests.test_config -v
"""
import os
import unittest

from app.config import (
    PROVIDER_ORDER,
    ProviderError,
    _FALLBACK_MODEL,  # noqa: F401  (仅用于断言可解析结构)
    resolve_candidates,
)


class TestResolveCandidates(unittest.TestCase):
    def setUp(self):
        # 隔离环境变量，保证测试确定性
        for k in ("DEEPSEEK_API_KEY", "ZHIPUAI_API_KEY", "HUNYUAN_API_KEY"):
            os.environ.pop(k, None)

    def test_alias_quick_returns_fallback_chain(self):
        candidates = resolve_candidates("zhiniu/quick")
        providers = [c.provider for c in candidates]
        # 降级链：deepseek → glm → hunyuan
        self.assertEqual(providers, ["deepseek", "glm", "hunyuan"])
        self.assertEqual(
            candidates[0].base_url, "https://api.deepseek.com"
        )
        self.assertEqual(candidates[0].model, "deepseek-chat")

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

    def test_provider_order_stable(self):
        # 新增厂商/别名应复用全局顺序，首厂商恒为 deepseek
        self.assertEqual(PROVIDER_ORDER, ["deepseek", "glm", "hunyuan"])
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


if __name__ == "__main__":
    unittest.main()