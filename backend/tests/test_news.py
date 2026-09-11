import unittest
from unittest.mock import patch

from app import data_sources
from app.data_sources import NewsQuery, fetch_market_news


class TestNewsProvider(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        data_sources._news_cache.clear()

    async def test_live_items_are_cached_and_keep_provenance(self):
        item = {
            "id": "em-test", "title": "测试新闻", "content": "摘要", "source": "测试媒体",
            "provider": "eastmoney-search", "isStale": False, "url": "https://example.com",
        }
        with patch("app.data_sources._fetch_eastmoney", return_value=[item]) as fetch:
            first, stale = await fetch_market_news(NewsQuery(symbol="sh600519"))
            second, stale_again = await fetch_market_news(NewsQuery(symbol="sh600519"))
        self.assertFalse(stale)
        self.assertFalse(stale_again)
        self.assertEqual(first[0]["provider"], "eastmoney-search")
        self.assertEqual(second[0]["source"], "测试媒体")
        fetch.assert_called_once()

    async def test_failure_without_cache_is_explicitly_stale(self):
        with patch("app.data_sources._fetch_eastmoney", side_effect=TimeoutError()):
            items, stale = await fetch_market_news(NewsQuery(keyword="A股"))
        self.assertEqual(items, [])
        self.assertTrue(stale)
