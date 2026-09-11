import unittest
from unittest.mock import AsyncMock, patch

from app import ta_agents
from app.agent import _levels_from_kline, _risk_flags, _technical_summary, stream_research


class TestResearchAgent(unittest.TestCase):
    def test_technical_summary_is_deterministic(self):
        result = _technical_summary({"data": [{"close": 10}, {"close": 11}]})
        self.assertEqual(result["direction"], "up")
        self.assertEqual(result["changePct"], 10.0)
        self.assertEqual(result["rsi14"], 100.0)

    def test_risk_flags_expose_stale_sources(self):
        flags = _risk_flags(
            {"isStale": True},
            {"sampleSize": 8, "changePct": 0},
            {"isStale": True},
            {"available": False},
        )
        self.assertIn("行情命中陈旧缓存", flags)
        self.assertIn("资讯为陈旧缓存或离线快照", flags)
        self.assertIn("K 线样本不足 20 根", flags)

    def test_levels_come_from_recent_kline_extremes(self):
        bars = [{"high": 10 + i, "low": 5 + i} for i in range(80)]
        levels = _levels_from_kline({"data": bars})
        self.assertEqual(levels["window"], 60)
        self.assertEqual(levels["pressure"], 89.0)   # 最近 60 根的最高价
        self.assertEqual(levels["support"], 25.0)    # 最近 60 根的最低价
        self.assertEqual(_levels_from_kline({"data": []}), {})


class TestTradingAgentsDebate(unittest.IsolatedAsyncioTestCase):
    _EVIDENCE = {
        "symbol": "sh600000",
        "quote": {"name": "测试股", "price": 11, "prevClose": 10},
        "technical": {"direction": "up", "changePct": 6.0, "rsi14": 30.0, "latest": 11.0, "ma20": 10.5, "sampleSize": 60},
        "financials": {"roe": 12.0},
        "news": {"total": 2, "items": [{"title": "利好公告", "source": "东方财富"}]},
        "riskFlags": ["行情命中陈旧缓存"],
        "levels": {"pressure": 12.5, "support": 9.8, "window": 60},
    }

    async def test_no_model_falls_back_to_rule_engine_with_same_shape(self):
        with patch.object(ta_agents, "_chat_once", new=AsyncMock(return_value=(None, {"id": "mock-llm"}))):
            synthesis = await ta_agents.run_debate(dict(self._EVIDENCE))
        self.assertEqual(synthesis["mode"], "deterministic_fallback")
        self.assertEqual(synthesis["provider"], "rule-engine")
        self.assertEqual(synthesis["rating"], "增持")
        self.assertEqual(synthesis["trend"], "上行")
        self.assertEqual(synthesis["pressure"], 12.5)
        self.assertEqual(synthesis["support"], 9.8)
        self.assertTrue(any("超卖" in p for p in synthesis["bullPoints"]))
        self.assertTrue(any("MA20" in p for p in synthesis["bullPoints"]))
        self.assertIn("行情命中陈旧缓存", synthesis["risks"])
        for key in ("stance", "confidence", "summary", "catalysts", "risks", "bearPoints", "riskNotes"):
            self.assertIn(key, synthesis)

    async def test_llm_path_runs_full_debate_and_reports_progress(self):
        answers = iter([
            "多头：动能延续。", "空头：估值偏高。",
            '{"rating":"持有","confidence":0.6,"summary":"多空均衡"}',
            '{"entry": 10.6, "stop": 9.8, "plan":"观察 MA20 支撑"}',
            "激进：可博弈。", "中性：观望。", "保守：等待回调。",
        ])

        async def fake_chat(_prompt, max_tokens=300):
            return next(answers), {"provider": "deepseek", "model": "deepseek-chat"}

        stages: list = []

        async def progress(stage, label, status):
            stages.append((stage, status))

        with patch.object(ta_agents, "_chat_once", new=fake_chat):
            synthesis = await ta_agents.run_debate(dict(self._EVIDENCE), progress)
        self.assertEqual(synthesis["mode"], "llm")
        self.assertEqual(synthesis["provider"], "deepseek")
        self.assertEqual(synthesis["rating"], "持有")
        self.assertEqual(synthesis["stance"], "震荡")
        self.assertEqual(synthesis["bullPoints"], ["多头：动能延续。"])
        self.assertEqual(synthesis["bearPoints"], ["空头：估值偏高。"])
        self.assertEqual(len(synthesis["riskNotes"]), 3)
        self.assertEqual(synthesis["trader"], {"entry": 10.6, "stop": 9.8, "plan": "观察 MA20 支撑"})
        self.assertEqual(synthesis["catalysts"], ["利好公告（东方财富）"])
        self.assertEqual(
            [s for s, status in stages if status == "done"],
            ["bull", "bear", "manager", "trader", "risk-aggressive", "risk-neutral", "risk-conservative", "synthesis"],
        )

    async def test_invalid_manager_rating_degrades_to_rules(self):
        answers = iter(["多头观点", "空头观点", '{"rating":"梭哈","confidence":0.9}'])

        async def fake_chat(_prompt, max_tokens=300):
            return next(answers), {"provider": "deepseek", "model": "deepseek-chat"}

        with patch.object(ta_agents, "_chat_once", new=fake_chat):
            synthesis = await ta_agents.run_debate(dict(self._EVIDENCE))
        self.assertEqual(synthesis["mode"], "deterministic_fallback")
        self.assertIn(synthesis["rating"], ta_agents.RATING_LABELS)


class TestResearchStream(unittest.IsolatedAsyncioTestCase):
    async def _fake_tool(self, name, _arguments):
        if name == "get_realtime_quote":
            return {"source": "quote-test", "name": "测试股", "price": 11, "prevClose": 10}
        if name == "get_kline":
            return {"source": "kline-test", "data": [{"close": 10, "high": 10.5, "low": 9.5}, {"close": 11, "high": 11.2, "low": 10.4}]}
        return {"source": "news-test", "total": 1, "items": []}

    async def test_stream_has_typed_progress_result_and_terminal_event(self):
        with patch("app.agent.execute_tool_async", new=AsyncMock(side_effect=self._fake_tool)), patch(
            "app.agent.run_debate",
            new=AsyncMock(return_value={"mode": "llm", "provider": "test-llm", "model": "test", "summary": "测试", "stance": "偏强", "confidence": 0.8, "catalysts": [], "risks": [], "rating": "增持", "pressure": 11.2, "support": 9.5}),
        ):
            events = [event async for event in stream_research("sh600000", "测试")]

        self.assertEqual(events[0]["type"], "run_started")
        self.assertEqual(
            [e["stage"] for e in events if e["type"] == "stage_completed"],
            ["market", "technical", "financial", "news", "risk", "synthesis"],
        )
        result = next(e for e in events if e["type"] == "result")["result"]
        self.assertEqual(result["status"], "complete")
        self.assertEqual(result["synthesis"]["mode"], "llm")
        self.assertEqual(result["evidence"]["levels"], {"pressure": 11.2, "support": 9.5, "window": 2})
        self.assertIn("bull", [s["id"] for s in result["stages"]])
        self.assertEqual(events[-1]["type"], "run_finished")
        self.assertTrue(events[-1]["final"])

    async def test_stream_forwards_debate_progress_as_typed_stages(self):
        async def fake_debate(_evidence, progress=None):
            await progress("bull", "多头研究员", "running")
            await progress("bull", "多头研究员", "done")
            await progress("bear", "空头研究员", "done")
            return {"mode": "llm", "provider": "test-llm", "model": "t", "summary": "s", "stance": "震荡", "confidence": 0.5, "catalysts": [], "risks": []}

        with patch("app.agent.execute_tool_async", new=AsyncMock(side_effect=self._fake_tool)), patch(
            "app.agent.run_debate", new=fake_debate,
        ):
            events = [event async for event in stream_research("sh600000")]

        started = [e["stage"] for e in events if e["type"] == "stage_started"]
        completed = [e["stage"] for e in events if e["type"] == "stage_completed"]
        self.assertEqual(started, ["bull"])
        self.assertEqual(completed, ["market", "technical", "financial", "news", "risk", "bull", "bear", "synthesis"])
        self.assertEqual(events[-1]["type"], "run_finished")

    async def test_stream_ends_with_error_event(self):
        with patch("app.agent.execute_tool_async", new=AsyncMock(side_effect=RuntimeError("offline"))):
            events = [event async for event in stream_research("sh600000")]

        self.assertEqual(events[-1]["type"], "run_error")
        self.assertEqual(events[-1]["code"], "research_failed")
        self.assertTrue(events[-1]["final"])
