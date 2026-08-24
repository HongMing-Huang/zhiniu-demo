"""知牛 网关 - 行情扩展路由测试（A4：indices / sectors / screener）。

运行：cd backend && python3 -m unittest tests.test_quote -v
"""
import asyncio
import unittest

from app.quote import quote_indices, quote_screener, quote_sectors


def _run(coro):
    return asyncio.run(coro)


class TestQuoteExtensions(unittest.TestCase):

    def test_indices_returns_list(self):
        d = _run(quote_indices())
        self.assertIn("indices", d)
        self.assertIn("source", d)
        self.assertGreater(len(d["indices"]), 0)
        # 第一个应含 name/price/symbol
        first = d["indices"][0]
        self.assertIn("symbol", first)
        self.assertIn("name", first)
        self.assertIn("prevClose", first)

    def test_indices_include_main(self):
        d = _run(quote_indices())
        symbols = {i["symbol"] for i in d["indices"]}
        self.assertIn("sh000001", symbols)  # 上证指数
        self.assertIn("sz399001", symbols)  # 深证成指

    def test_sectors_sorted_desc(self):
        d = _run(quote_sectors())
        self.assertIn("sectors", d)
        pcts = [r["changePercent"] for r in d["sectors"]]
        self.assertEqual(pcts, sorted(pcts, reverse=True))  # 降序
        self.assertGreater(len(d["sectors"]), 5)

    def test_screener_industry_filter(self):
        d = _run(quote_screener(industry="银行", min_pct=1.0))
        rows = d["rows"]
        self.assertGreater(len(rows), 0)
        for r in rows:
            self.assertIn("银行", r["industry"])
            self.assertGreaterEqual(r["changePercent"], 1.0)

    def test_screener_min_pct(self):
        d = _run(quote_screener(min_pct=2.0))
        self.assertGreater(len(d["rows"]), 0)
        for r in d["rows"]:
            self.assertGreaterEqual(r["changePercent"], 2.0)

    def test_screener_no_filter_returns_pool(self):
        d = _run(quote_screener())
        self.assertGreater(len(d["rows"]), 0)


if __name__ == "__main__":
    unittest.main()