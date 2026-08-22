"""知牛 LLM 网关 - config.py 单元测试（无网络、无三方依赖、无真实 Key）

覆盖：模型别名解析 / 降级链候选序 / 显式 provider/model / 未注册模型报错。
运行：cd backend && python3 -m unittest tests.test_config -v
"""
import os
import unittest

from app.config import (
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


if __name__ == "__main__":
    unittest.main()