"""知牛 网关 - Function Calling 工具测试（A3）。

覆盖：7 工具 schema 完整、各工具执行返回结构化结果、未知工具报错。
运行：cd backend && python3 -m unittest tests.test_tools -v
"""
import os
import unittest

from app.tools import TOOLS_SCHEMA, execute_tool, get_tools_schema


class TestTools(unittest.TestCase):

    def test_7_tools_schema_complete(self):
        # 7 工具 schema 全部注册
        names = {t["function"]["name"] for t in get_tools_schema()}
        self.assertEqual(len(names), 7)
        expected = {
            "get_realtime_quote", "get_kline", "get_financials", "search_news",
            "screen_stocks", "compare_stocks", "create_alert",
        }
        self.assertEqual(names, expected)

    def test_get_realtime_quote(self):
        r = execute_tool("get_realtime_quote", {"symbol": "sh600519"})
        self.assertEqual(r["name"], "贵州茅台")
        self.assertIn("price", r)

    def test_get_kline(self):
        r = execute_tool("get_kline", {"symbol": "sh600519"})
        self.assertIn("data", r)
        self.assertGreater(len(r["data"]), 0)
        self.assertIn("close", r["data"][0])

    def test_get_financials(self):
        r = execute_tool("get_financials", {"symbol": "sh600519"})
        self.assertIn("pe", r)
        self.assertIn("pb", r)

    def test_search_news(self):
        r = execute_tool("search_news", {"keyword": "贵州茅台"})
        self.assertIn("items", r)
        self.assertGreater(len(r["items"]), 0)

    def test_screen_stocks(self):
        r = execute_tool("screen_stocks", {"industry": "银行", "min_pct": 0})
        self.assertIn("rows", r)

    def test_compare_stocks(self):
        r = execute_tool("compare_stocks", {"symbols": ["sh600519", "sz000001"]})
        self.assertEqual(len(r["rows"]), 2)

    def test_create_alert(self):
        r = execute_tool("create_alert", {"symbol": "sh600519", "condition": "above", "price": 1300})
        self.assertEqual(r["status"], "created")

    def test_unknown_tool_returns_error(self):
        r = execute_tool("no_such_tool", {})
        self.assertIn("error", r)

    def test_missing_symbol_returns_error_obj(self):
        # 缺必需参数 → 工具内部容错，返回结构化结果而非抛异常
        r = execute_tool("get_realtime_quote", {"symbol": "unknown000"})
        self.assertIn("error", r)  # 未知 symbol 返回错误对象


if __name__ == "__main__":
    unittest.main()