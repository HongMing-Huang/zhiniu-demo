import unittest
from unittest.mock import AsyncMock, patch

from app import store, ta_agents
from app.agent import (
    _levels_from_kline,
    _risk_flags,
    _technical_summary,
    run_chat,
    run_compare,
    run_insight,
    stream_chat,
    stream_compare,
    stream_research,
)


class TestNumCoercion(unittest.TestCase):
    def test_num_coerces_model_string_prices(self):
        self.assertEqual(ta_agents._num("1,890.5"), 1890.5)
        self.assertEqual(ta_agents._num(" 178.6 "), 178.6)
        self.assertEqual(ta_agents._num(10.6), 10.6)
        self.assertIsNone(ta_agents._num("abc"))
        self.assertIsNone(ta_agents._num(None))
        self.assertIsNone(ta_agents._num(True))


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


class TestTraderStringPrices(unittest.IsolatedAsyncioTestCase):
    _EVIDENCE = {
        "symbol": "sh600519",
        "quote": {"name": "贵州茅台", "price": 10.0, "prevClose": 9.5},
        "technical": {"direction": "sideways", "changePct": 0.2, "rsi14": 50, "ma20": 10.0, "latest": 10.0},
        "financials": {"available": True},
        "news": {"total": 0, "items": []},
        "riskFlags": [],
        "levels": {"pressure": 12.5, "support": 9.8, "window": 60},
    }

    async def test_string_trader_prices_are_coerced(self):
        answers = iter([
            "多头观点", "空头观点",
            '{"rating":"持有","confidence":0.5,"summary":"均衡"}',
            '{"entry": "10.6", "stop": " 9.8 ", "plan":"字符串价格"}',
            "激进：ok。", "中性：ok。", "保守：ok。",
        ])

        async def fake_chat(_prompt, max_tokens=300):
            return next(answers), {"provider": "deepseek", "model": "deepseek-chat"}

        with patch.object(ta_agents, "_chat_once", new=fake_chat):
            synthesis = await ta_agents.run_debate(dict(self._EVIDENCE))
        self.assertEqual(synthesis["mode"], "llm")
        self.assertEqual(synthesis["trader"]["entry"], 10.6)
        self.assertEqual(synthesis["trader"]["stop"], 9.8)
        self.assertEqual(synthesis["trader"]["plan"], "字符串价格")


class TestRunChat(unittest.IsolatedAsyncioTestCase):
    async def test_llm_reply_uses_history_and_quote_snapshot(self):
        captured: dict = {}

        async def fake_gateway_chat(*, model, messages, stream, temperature, max_tokens):
            captured["messages"] = messages
            yield {"id": "chat-1", "provider": "gemai", "model": "deepseek-v4-flash",
                   "choices": [{"message": {"content": "RSI 是动量指标，70 以上超买。"}}]}

        with patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            result = await run_chat(
                question="解释 RSI",
                history=[{"role": "user", "content": "之前的问题"}, {"role": "ai", "content": "之前的回答"}],
                symbol="sh600519",
                quote={"name": "贵州茅台", "price": 1275.16, "changePct": -0.78},
            )
        self.assertEqual(result["mode"], "llm")
        self.assertIn("RSI", result["content"])
        roles = [m["role"] for m in captured["messages"]]
        self.assertEqual(roles, ["system", "user", "assistant", "user"])
        self.assertIn("贵州茅台", captured["messages"][0]["content"])
        self.assertIn("1275.16", captured["messages"][0]["content"])

    async def test_no_model_returns_explicit_fallback(self):
        async def fake_gateway_chat(**_kw):
            yield {"id": "mock-llm"}

        with patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            result = await run_chat(question="随便问")
        self.assertEqual(result["mode"], "deterministic_fallback")
        self.assertEqual(result["provider"], "rule-engine")
        self.assertIn("多 Agent 研究", result["content"])

    async def test_gateway_exception_never_raises(self):
        async def fake_gateway_chat(**_kw):
            raise RuntimeError("network down")
            yield  # pragma: no cover

        with patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            result = await run_chat(question="随便问")
        self.assertEqual(result["mode"], "deterministic_fallback")


class TestRunInsight(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        # insight 复用 store 缓存，测试间必须清空避免相互污染
        store._research_cache.clear()
        store._inflight.clear()

    @staticmethod
    def _tools(symbol: str = "sh600519"):
        async def fake_tool(name, params):
            if name == "get_realtime_quote":
                return {"name": "测试股", "price": 11.0, "prevClose": 10.0, "changePct": 10.0,
                        "source": "sina", "isStale": False, "date": "2026-09-13", "time": "15:00:00"}
            if name == "get_kline":
                return {"data": [{"high": 12.5, "low": 9.8, "close": 11.0} for _ in range(60)],
                        "source": "sina"}
            if name == "get_financials":
                return {"available": True, "pe": 19.5, "pb": 6.3, "marketCap": 1.5e12,
                        "reportDate": "2026-06-30", "revenue": 9.2e10, "netProfit": 4.4e10,
                        "roe": 16.8, "source": "eastmoney"}
            raise ValueError(name)

        return fake_tool

    async def test_llm_insight_keeps_valid_zone_and_drops_hallucinated(self):
        async def fake_gateway_chat(**_kw):
            yield {
                "id": "chat-insight", "provider": "gemai", "model": "deepseek-v4-flash",
                "choices": [{"message": {"content": (
                    '{"verdict":"偏强","summary":"量价配合良好","trend":"站上 MA20",'
                    '"valuation":"PE 19.5 中等","earnings":"净利 440 亿","volume":"温和放量",'
                    '"risk":"注意高位波动","signals":["RSI 55 中性","PE 19.5"],'
                    '"buyZone":[9.9,10.5],"sellZone":[99.0,120.0],'
                    '"riskLevel":"medium","followUps":["跌破支撑怎么办"]}'
                )}}],
            }

        with patch("app.agent.execute_tool_async", new=self._tools()), \
                patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            result = await run_insight("sh600519")
        self.assertEqual(result["mode"], "llm")
        self.assertEqual(result["llm"]["verdict"], "偏强")
        self.assertEqual(result["llm"]["buyZone"], [9.9, 10.5])   # 在支撑 9.8~压力 12.5 附近 → 保留
        self.assertIsNone(result["llm"]["sellZone"])              # 99~120 超出压力 3% → 防幻觉丢弃
        self.assertEqual(result["llm"]["riskLevel"], "medium")

    async def test_llm_invalid_enum_coerced_to_rule_baseline(self):
        async def fake_gateway_chat(**_kw):
            yield {"id": "chat-2", "provider": "gemai", "model": "m1",
                   "choices": [{"message": {"content": '{"verdict":"暴涨","summary":"s",'
                                '"riskLevel":"extreme","signals":[],"followUps":[]}'}}]}

        with patch("app.agent.execute_tool_async", new=self._tools()), \
                patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            result = await run_insight("sh600519")
        self.assertEqual(result["llm"]["verdict"], "中性")       # K 线平坦 direction=sideways → 中性
        self.assertEqual(result["llm"]["riskLevel"], "low")      # 非法枚举 → 规则基准（平坦K线/无风险项 → low）
        self.assertEqual(result["mode"], "llm")

    async def test_no_model_explicit_rule_fallback(self):
        async def fake_gateway_chat(**_kw):
            yield {"id": "mock-llm"}

        with patch("app.agent.execute_tool_async", new=self._tools()), \
                patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            result = await run_insight("sh600519")
        self.assertEqual(result["mode"], "deterministic_fallback")
        self.assertEqual(result["provider"], "rule-engine")
        self.assertIsNone(result["llm"])
        self.assertIn("riskLevel", result)
        self.assertIn("advice", result)

    async def test_second_call_hits_cache(self):
        async def fake_gateway_chat(**_kw):
            yield {"id": "chat-3", "provider": "gemai", "model": "m1",
                   "choices": [{"message": {"content": '{"verdict":"中性","summary":"s","signals":[]}'}}]}

        with patch("app.agent.execute_tool_async", new=self._tools()), \
                patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            first = await run_insight("sh600519")
            second = await run_insight("sh600519")
        self.assertNotIn("cached", first)
        self.assertTrue(second.get("cached"))
        self.assertEqual(second["cache"], "memory")


class TestStreamChat(unittest.IsolatedAsyncioTestCase):
    async def _collect(self, **kwargs):
        frames = []
        async for frame in stream_chat(**kwargs):
            frames.append(frame)
        return frames

    async def test_stream_forwards_deltas_and_finishes_llm(self):
        async def fake_gateway_chat(**_kw):
            yield {"id": "chat-s", "provider": "gemai", "model": "m",
                   "choices": [{"delta": {"role": "assistant"}}]}
            yield {"id": "chat-s", "provider": "gemai", "model": "m",
                   "choices": [{"delta": {"content": "RSI 是"}}]}
            yield {"id": "chat-s", "provider": "gemai", "model": "m",
                   "choices": [{"delta": {"content": "动量指标。"}, "finish_reason": "stop"}]}

        with patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            frames = await self._collect(question="解释 RSI")
        types = [f["type"] for f in frames]
        self.assertEqual(types[0], "chat_started")
        self.assertEqual(types[-1], "chat_finished")
        self.assertEqual(frames[-1]["mode"], "llm")
        self.assertEqual(frames[-1]["content"], "RSI 是动量指标。")
        deltas = [f["content"] for f in frames if f["type"] == "delta"]
        self.assertEqual(deltas, ["RSI 是", "动量指标。"])

    async def test_stream_mock_llm_suppressed_into_rule_fallback(self):
        async def fake_gateway_chat(**_kw):
            yield {"id": "mock-llm", "choices": [{"delta": {"content": "占位文本不应透传"}}]}

        with patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            frames = await self._collect(question="随便问")
        deltas = [f for f in frames if f["type"] == "delta"]
        final = frames[-1]
        self.assertEqual(final["type"], "chat_finished")
        self.assertEqual(final["mode"], "deterministic_fallback")
        self.assertNotIn("占位文本", final["content"])
        self.assertTrue(all("占位文本" not in d["content"] for d in deltas))

    async def test_stream_gateway_exception_falls_back(self):
        async def fake_gateway_chat(**_kw):
            raise RuntimeError("down")
            yield  # pragma: no cover

        with patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            frames = await self._collect(question="随便问")
        self.assertEqual(frames[-1]["mode"], "deterministic_fallback")


class TestRunCompare(unittest.IsolatedAsyncioTestCase):
    async def test_compare_rule_baseline_always_present(self):
        async def fake_tool(name, params):
            sym = params.get("symbol", "")
            if name == "get_realtime_quote":
                return {"name": "股" + sym[-2:], "price": 10.0 + hash(sym) % 5, "prevClose": 10.0,
                        "changePct": 1.5, "source": "sina", "isStale": False}
            if name == "get_kline":
                return {"data": [{"high": 11.0, "low": 9.0, "close": 10.5} for _ in range(60)]}
            return {"available": False}

        async def fake_gateway_chat(**_kw):
            yield {"id": "mock-llm"}

        with patch("app.agent.execute_tool_async", new=fake_tool), \
                patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            result = await run_compare(["sh600519", "sz300750"])
        self.assertEqual(result["mode"], "deterministic_fallback")
        self.assertIsNone(result["llm"])
        self.assertEqual(len(result["stocks"]), 2)
        self.assertIn("summary", result["rule"])
        self.assertIn("stronger", result["rule"])

    async def test_compare_llm_json_parsed_and_bounded(self):
        async def fake_tool(name, params):
            if name == "get_realtime_quote":
                return {"name": "A", "price": 10, "prevClose": 9, "changePct": 1.0,
                        "source": "sina", "isStale": False}
            if name == "get_kline":
                return {"data": [{"high": 11, "low": 9, "close": 10} for _ in range(60)]}
            return {"available": False}

        async def fake_gateway_chat(**_kw):
            yield {"id": "chat-c", "provider": "gemai", "model": "m",
                   "choices": [{"message": {"content": (
                       '{"summary":"A 强于 B","stronger":"X","pointsA":["PE 更低"],"pointsB":[],'
                       '"conclusion":"均衡配置"}'
                   )}}]}

        with patch("app.agent.execute_tool_async", new=fake_tool), \
                patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            result = await run_compare(["sh600519", "sz300750"], focus="估值")
        self.assertEqual(result["mode"], "llm")
        self.assertEqual(result["llm"]["stronger"], "none")   # 非法枚举 → none
        self.assertEqual(result["llm"]["pointsA"], ["PE 更低"])


class TestStreamCompare(unittest.IsolatedAsyncioTestCase):
    async def test_stream_compare_emits_stages_and_result(self):
        stages: list[str] = []

        async def fake_tool(name, params):
            if name == "get_realtime_quote":
                return {"name": "股", "price": 10, "prevClose": 10, "changePct": 0.0,
                        "source": "sina", "isStale": False}
            if name == "get_kline":
                return {"data": [{"high": 11, "low": 9, "close": 10} for _ in range(60)]}
            return {"available": False}

        async def fake_gateway_chat(**_kw):
            yield {"id": "mock-llm"}

        with patch("app.agent.execute_tool_async", new=fake_tool), \
                patch("app.agent.gateway.chat_completions", new=fake_gateway_chat):
            frames = []
            async for frame in stream_compare(["sh600519", "sz300750"]):
                frames.append(frame)
                if frame.get("type") == "stage":
                    stages.append(frame["stage"])
        types = [f["type"] for f in frames]
        self.assertEqual(types[0], "stage")
        self.assertIn("evidence_a", stages)
        self.assertIn("evidence_b", stages)
        self.assertIn("llm", stages)
        self.assertEqual(types[-1], "run_finished")
        self.assertTrue(frames[-1]["final"])
        result_frame = next(f for f in frames if f["type"] == "result")
        self.assertEqual(len(result_frame["result"]["stocks"]), 2)
