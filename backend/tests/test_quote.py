"""知牛 网关 - 行情扩展路由测试（A4：indices / sectors / screener / popularity）。

运行：cd backend && python3 -m unittest tests.test_quote -v
板块/选股/人气榜已接真实东财数据；路由级测试统一 patch 出站请求为离线，
保证结果确定性；真实响应形状用 2026-09-11 抓包样本做纯解析测试。
"""
import asyncio
import unittest
from unittest import mock

from app import quote as quote_module
from app.quote import (
    _guard_external_url,
    _parse_eastmoney_flow,
    _parse_eastmoney_report,
    _parse_eastmoney_sector,
    _parse_eastmoney_snapshot,
    _parse_eastmoney_stock_row,
    _parse_eastmoney_ulist,
    _parse_sina,
    _parse_tencent_extras,
    quote_indices,
    quote_popularity,
    quote_screener,
    quote_search,
    quote_sectors,
)


def _run(coro):
    return asyncio.run(coro)


async def _offline_async(*_args, **_kwargs):
    raise OSError("offline")


def _offline_sync(*_args, **_kwargs):
    raise OSError("offline")


class TestQuoteExtensions(unittest.TestCase):

    def setUp(self):
        quote_module._sectors_cache = None
        quote_module._screener_cache = None
        quote_module._popularity_cache = None

    def test_realtime_parser_keeps_amount_in_yuan(self):
        fields = ["测试股份", "10", "9", "11", "12", "8", "10.9", "11.1", "1200", "350000000"]
        fields += ["100", "10"] * 5 + ["100", "11"] * 5 + ["2026-09-10", "14:32:00"]
        quote = _parse_sina("sh600000", 'var hq_str_sh600000="' + ",".join(fields) + '";')
        self.assertIsNotNone(quote)
        self.assertEqual(quote["amount"], 350000000.0)
        self.assertEqual(quote["volume"], 12.0)
        self.assertEqual(quote["bids"], [[10.0, 100.0]] * 5)
        self.assertEqual(quote["asks"], [[11.0, 100.0]] * 5)
        # 涨跌幅由现价/昨收现算（AI 快照注入、诊股证据、双股对比共同依赖）
        self.assertEqual(quote["changePct"], 22.22)

    def test_eastmoney_snapshot_parses_fundamental_units(self):
        result = _parse_eastmoney_snapshot(
            "sh600519",
            {
                "f58": "贵州茅台",
                "f116": 1598129320350.42,
                "f117": 1598129320350.42,
                "f127": "白酒Ⅱ",
                "f128": "贵州板块",
                "f129": "白酒,超级品牌",
                "f152": 2,
                "f162": 1795,
                "f167": 636,
                "f168": 22,
                "f171": 180,
            },
        )
        self.assertEqual(result["pe"], 17.95)
        self.assertEqual(result["pb"], 6.36)
        self.assertEqual(result["turnoverRate"], 0.22)
        self.assertEqual(result["amplitude"], 1.8)
        self.assertEqual(result["industry"], "白酒Ⅱ")
        self.assertEqual(result["concepts"], ["白酒", "超级品牌"])

    def test_eastmoney_report_parser_keeps_report_period(self):
        report = _parse_eastmoney_report({"result": {"data": [{
            "REPORTDATE": "2026-06-30 00:00:00",
            "DATATYPE": "2026年 半年报",
            "TOTAL_OPERATE_INCOME": 92278072083.21,
            "PARENT_NETPROFIT": 44516880421.86,
            "WEIGHTAVG_ROE": 16.75,
            "XSMLL": 89.56,
        }]}})
        self.assertEqual(report["reportDate"], "2026-06-30")
        self.assertEqual(report["reportType"], "2026年 半年报")
        self.assertEqual(report["revenue"], 92278072083.21)
        self.assertEqual(report["grossMargin"], 89.56)

    def test_eastmoney_money_flow_parser_uses_latest_row(self):
        flow = _parse_eastmoney_flow({"data": {"klines": [
            "2026-09-11 14:02,-250168582,-185531,250354120,-111592547,-138576035"
        ]}})
        self.assertEqual(flow["asOf"], "2026-09-11 14:02")
        self.assertEqual(flow["mainNetInflow"], -250168582.0)
        self.assertEqual(flow["smallNetInflow"], -185531.0)
        self.assertEqual(flow["superLargeNetInflow"], -138576035.0)

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

    def test_sectors_real_sample_row(self):
        # 2026-09-11 push2 clist fs=m:90+t:2 抓包样本
        row = _parse_eastmoney_sector({
            "f2": 27073.48, "f3": 5.84, "f12": "BK1592", "f14": "通信线缆及配套",
            "f104": 12, "f105": 1, "f128": "神宇股份", "f140": "300563", "f136": 19.98,
        })
        self.assertEqual(row["name"], "通信线缆及配套")
        self.assertEqual(row["changePercent"], 5.84)
        self.assertEqual(row["upCount"], 12)
        self.assertEqual(row["leadStock"], "神宇股份")
        self.assertEqual(row["leadSymbol"], "sz300563")

    def test_sectors_offline_falls_back_to_mock_desc(self):
        with mock.patch.object(quote_module, "_eastmoney_json", _offline_async):
            d = _run(quote_sectors())
        self.assertEqual(d["source"], "mock")
        self.assertTrue(d["isStale"])
        pcts = [r["changePercent"] for r in d["sectors"]]
        self.assertEqual(pcts, sorted(pcts, reverse=True))
        self.assertGreater(len(d["sectors"]), 5)

    def test_ulist_sample_parses_market_cap(self):
        # 2026-09-11 ulist.np secids=1.600519,0.000001,0.300750 抓包样本
        parsed = _parse_eastmoney_ulist({"data": {"diff": [
            {"f2": 1275.16, "f3": -0.78, "f5": 34801, "f8": 0.28, "f12": "600519",
             "f14": "贵州茅台", "f20": 1594054054331},
            {"f2": 11.74, "f8": 0.43, "f12": "000001", "f14": "平安银行", "f20": 227825479645},
        ]}})
        self.assertEqual(parsed["600519"]["marketCap"], 1594054054331.0)
        self.assertAlmostEqual(parsed["600519"]["turnoverRate"], 0.28)
        self.assertEqual(parsed["000001"]["marketCap"], 227825479645.0)

    def test_stock_row_sample_maps_symbol_and_industry(self):
        row = _parse_eastmoney_stock_row({
            "f12": "600519", "f14": "贵州茅台", "f2": 1275.16, "f3": -0.78,
            "f5": 34801, "f6": 4439000000.0, "f8": 0.28, "f9": 1795.0,
            "f20": 1594054054331, "f100": "白酒",
        })
        self.assertEqual(row["symbol"], "sh600519")
        self.assertEqual(row["industry"], "白酒")
        self.assertEqual(row["marketCap"], 1594054054331.0)
        self.assertIsNone(_parse_eastmoney_stock_row({"f12": "x", "f14": ""}))

    def test_screener_offline_pool_with_filters(self):
        with mock.patch.object(quote_module, "_eastmoney_json", _offline_async):
            d = _run(quote_screener(industry="银行", min_pct=1.0))
        self.assertEqual(d["source"], "mock")
        self.assertTrue(d["isStale"])
        self.assertGreater(len(d["rows"]), 0)
        for r in d["rows"]:
            self.assertIn("银行", r["industry"])
            self.assertGreaterEqual(r["changePercent"], 1.0)

    def test_popularity_offline_ranks_mock_by_amount(self):
        with mock.patch.object(quote_module, "_post_popularity_rank", _offline_sync):
            d = _run(quote_popularity(count=5))
        self.assertEqual(d["source"], "mock")
        self.assertTrue(d["isStale"])
        self.assertLessEqual(len(d["stocks"]), 5)
        ranks = [s["rank"] for s in d["stocks"]]
        self.assertEqual(ranks, list(range(1, len(ranks) + 1)))

    def test_popularity_real_sample_merges_rank_and_quote(self):
        payload = {"data": [
            {"sc": "SH688801", "rk": 1}, {"sc": "SZ000636", "rk": 2}, {"sc": "bad", "rk": 3},
        ]}
        sina_raw = (
            'var hq_str_sh688801="华丰科技,20.0,19.8,20.5,20.8,19.9,20.4,20.6,90000,180000000,'
            + "100,20.4," * 5 + "100,20.6," * 5 + '2026-09-11,14:32:00";'
        )

        def fake_post(page_size):
            return payload

        def fake_get(url, decode=None, headers=None):
            return sina_raw

        with mock.patch.object(quote_module, "_post_popularity_rank", fake_post), \
                mock.patch.object(quote_module, "_http_get", fake_get):
            d = _run(quote_popularity(count=3))
        self.assertEqual(d["source"], "eastmoney-popularity")
        self.assertFalse(d["isStale"])
        self.assertEqual([s["symbol"] for s in d["stocks"]], ["sh688801", "sz000636"])
        self.assertEqual(d["stocks"][0]["rank"], 1)
        self.assertAlmostEqual(d["stocks"][0]["changePercent"], round((20.5 - 19.8) / 19.8 * 100, 2))

    def test_tencent_extras_sample_converts_market_cap_to_yuan(self):
        # 2026-09-11 qt.gtimg.cn/q=sh600519 抓包样本（88 字段，仅保留用到的下标）
        fields = [""] * 88
        fields[1], fields[2], fields[3] = "贵州茅台", "600519", "1275.16"
        fields[38], fields[45], fields[49] = "0.28", "15940.54", "1.25"
        raw = 'v_sh600519="' + "~".join(fields) + '";\nv_bad="1~2";'
        parsed = _parse_tencent_extras(raw)
        self.assertEqual(list(parsed), ["sh600519"])
        self.assertEqual(parsed["sh600519"]["marketCap"], 1594054000000)
        self.assertAlmostEqual(parsed["sh600519"]["turnoverRate"], 0.28)
        self.assertAlmostEqual(parsed["sh600519"]["volumeRatio"], 1.25)

    def test_quote_extras_falls_back_to_tencent_when_eastmoney_drops(self):
        fields = [""] * 88
        fields[38], fields[45], fields[49] = "0.65", "15291.94", "0.83"
        raw = 'v_sz300750="' + "~".join(fields) + '";'

        def fake_get(url, decode=None, headers=None):
            self.assertIn("qt.gtimg.cn", url)
            return raw

        quote_module._ulist_cache.clear()
        with mock.patch.object(quote_module, "_eastmoney_json", _offline_async), \
                mock.patch.object(quote_module, "_http_get", fake_get):
            extras = _run(quote_module._quote_extras(["sz300750", "bad"]))
        self.assertEqual(extras["sz300750"]["marketCap"], 1529194000000)
        self.assertAlmostEqual(extras["sz300750"]["turnoverRate"], 0.65)

    def test_guard_external_url_blocks_non_allowlist(self):
        self.assertEqual(
            _guard_external_url("https://push2delay.eastmoney.com/api/qt/clist/get"),
            "https://push2delay.eastmoney.com/api/qt/clist/get",
        )
        with self.assertRaises(ValueError):
            _guard_external_url("http://push2delay.eastmoney.com/api")  # 非 HTTPS
        with self.assertRaises(ValueError):
            _guard_external_url("https://169.254.169.254/latest/meta-data")  # 云元数据
        with self.assertRaises(ValueError):
            _guard_external_url("https://127.0.0.1:8000/admin")  # 本机服务


_SUGGEST_SAMPLE = {
    "QuotationCodeTable": {
        "Status": 0,
        "Data": [
            {"Code": "600519", "Name": "贵州茅台", "MktNum": "1", "Classify": "AStock",
             "SecurityType": "25", "SecurityTypeName": "A股"},
            {"Code": "000001", "Name": "平安银行", "MktNum": "0", "Classify": "AStock",
             "SecurityType": "25", "SecurityTypeName": "A股"},
            {"Code": "688981", "Name": "中芯国际", "MktNum": "1", "Classify": "23",
             "SecurityType": "25", "SecurityTypeName": "A股"},   # 科创板独立分类
            {"Code": "00700", "Name": "腾讯控股", "MktNum": "116", "Classify": "HK",
             "SecurityType": "25", "SecurityTypeName": "港股"},  # 港股应被过滤
            {"Code": "000001", "Name": "上证指数", "MktNum": "1", "Classify": "Index",
             "SecurityType": "25", "SecurityTypeName": "指数"},  # 指数应被过滤
        ],
    }
}


class TestQuoteSearch(unittest.TestCase):

    def setUp(self):
        quote_module._search_cache = (0.0, "", {"items": [], "source": "", "isStale": False})

    def test_suggest_parser_maps_only_a_shares(self):
        from app.quote import _parse_eastmoney_suggest

        items = _parse_eastmoney_suggest(_SUGGEST_SAMPLE, 10)
        symbols = [i["symbol"] for i in items]
        self.assertEqual(symbols, ["sh600519", "sz000001", "sh688981"])
        self.assertEqual(items[0]["name"], "贵州茅台")
        self.assertEqual(items[0]["market"], "SH")

    def test_search_real_sample_cached(self):
        with mock.patch.object(quote_module, "_http_get", return_value='{"QuotationCodeTable":{"Data":['
                '{"Code":"600519","Name":"贵州茅台","MktNum":"1","Classify":"AStock","SecurityType":"25"}]}}'):
            first = _run(quote_search("茅台", 10))
        self.assertEqual(first["source"], "eastmoney-suggest")
        self.assertEqual(first["items"][0]["symbol"], "sh600519")
        self.assertFalse(first["isStale"])
        # 第二次命中缓存（不再出网）
        with mock.patch.object(quote_module, "_http_get", side_effect=OSError("should not fetch")):
            second = _run(quote_search("茅台", 10))
        self.assertTrue(second.get("cached"))
        self.assertEqual(second["items"], first["items"])

    def test_search_offline_falls_back_to_local_snapshot(self):
        with mock.patch.object(quote_module, "_http_get", side_effect=OSError("offline")):
            result = _run(quote_search("茅台", 10))
        self.assertTrue(result["isStale"])
        self.assertEqual(result["source"], "local-snapshot")
        self.assertTrue(any(i["symbol"].endswith("600519") for i in result["items"]))

    def test_search_invalid_keyword_returns_empty(self):
        result = _run(quote_search("  ", 10))
        self.assertEqual(result["items"], [])
        self.assertEqual(result["source"], "invalid")


class TestKlineCacheIntegrity(unittest.IsolatedAsyncioTestCase):
    """回归：离线快照/空结果不得进入 kline 缓存（此前一次瞬时失败 → 日线 6h 假 K）。"""

    def setUp(self):
        quote_module._kline_cache.clear()

    tearDown = setUp

    _SINA_BARS = (
        '[{"day":"2026-09-10","open":"10.0","high":"11.0","low":"9.5","close":"10.5","volume":"1000"},'
        '{"day":"2026-09-11","open":"10.5","high":"11.5","low":"10.0","close":"11.0","volume":"1200"}]'
    )

    async def test_failed_fetch_mock_not_cached(self):
        # 源失败 → 返回离线快照（sh600519 有快照夹具），但不得写入缓存
        with mock.patch.object(quote_module, "_http_get", side_effect=OSError("offline")):
            result = await quote_module.quote_kline("sh600519", 240, 5)
        self.assertEqual(result["provider"], "offline-snapshot")
        self.assertNotIn("sh600519|240|5", quote_module._kline_cache)

    async def test_next_request_retries_live_after_failure(self):
        # 失败后的下一次请求必须重试真实源（不被 mock 缓存顶掉）
        with mock.patch.object(quote_module, "_http_get", side_effect=OSError("offline")):
            await quote_module.quote_kline("sz000002", 240, 5)
        with mock.patch.object(
            quote_module, "_http_get", return_value=self._SINA_BARS
        ):
            result = await quote_module.quote_kline("sz000002", 240, 5)
        self.assertEqual(result["provider"], "sina-kline")
        self.assertFalse(result["isStale"])
        self.assertEqual(result["data"][-1]["close"], "11.0")

    async def test_expired_real_entry_serves_stale_and_is_not_rearmed(self):
        # 过期真实数据：源失败时作 stale 兜底返回，但不得写回缓存刷新时间戳
        real = {
            "symbol": "sz000003", "name": "x", "data": [{"day": "2026-09-11", "close": "9.9"}],
            "scale": 240, "source": "新浪财经", "provider": "sina-kline", "isStale": False,
        }
        import time as _time
        quote_module._kline_cache["sz000003|240|5"] = (_time.time() - 7 * 3600, real)
        with mock.patch.object(quote_module, "_http_get", side_effect=OSError("offline")):
            result = await quote_module.quote_kline("sz000003", 240, 5)
        self.assertTrue(result["isStale"])
        stored_ts, stored = quote_module._kline_cache["sz000003|240|5"]
        self.assertLess(stored_ts, _time.time() - 6 * 3600)  # 时间戳未被刷新
        self.assertFalse(stored["isStale"])  # 缓存里仍是无 stale 标记的原始真实数据

    async def test_empty_source_result_not_cached(self):
        # 新浪返回空列表且无快照可用 → 空结果不进缓存
        with mock.patch.object(quote_module, "_http_get", return_value="[]"):
            result = await quote_module.quote_kline("sz000004", 240, 5)
        self.assertEqual(result.get("data"), [])
        self.assertNotIn("sz000004|240|5", quote_module._kline_cache)


if __name__ == "__main__":
    unittest.main()
