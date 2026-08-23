/* 知牛 · 纯逻辑单元测试（commonTest，待 gradle 脚手架生成后随 :shared:allTests 运行）
 * 覆盖：Quote 涨跌计算 / AiInsight 展示映射 / Mock 数据兜底。
 * 说明：此为「验证工程」，不依赖 Kuikly 编译，仅需 junit5 + kotlin-test。
 */
package com.zhiniu

import com.zhiniu.data.mock.MockDataSource
import com.zhiniu.domain.model.AiInsight
import com.zhiniu.domain.model.AiSignal
import com.zhiniu.domain.model.Quote
import com.zhiniu.domain.model.Trend
import com.zhiniu.pages.components.toUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuoteMathTest {
    private fun q(price: Double, prev: Double) =
        Quote("s", "n", prev, prev, price, price, price, price, price, 0L, 0.0)

    @Test fun `up stock positive change`() {
        val qq = q(10.0, 9.0)
        assertTrue(qq.isUp)
        assertEquals(1.0, qq.change)
        assertEquals(11.111, qq.changePercent, absoluteTolerance = 0.01)
    }

    @Test fun `down stock negative change`() {
        val qq = q(9.0, 10.0)
        assertFalse(qq.isUp)
        assertEquals(-10.0, qq.changePercent, absoluteTolerance = 0.01)
    }

    @Test fun `flat`() {
        val qq = q(1.0, 1.0)
        assertEquals("FLAT", com.zhiniu.pages.components.priceColor(qq))
    }
}

class MockDataTest {
    private val mock = MockDataSource()

    @Test fun `quotes returns real blue chips`() {
        val list = mock.quotes()
        assertTrue(list.size >= 5)
        assertTrue(list.any { it.symbol == "sh600519" && it.name == "贵州茅台" })
    }

    @Test fun `kline returns 20 bars consistent OHLC`() {
        val bars = mock.kline("anything")
        assertEquals(20, bars.size)
        bars.forEach { b ->
            assertTrue(b.high >= b.close && b.high >= b.open)
            assertTrue(b.low <= b.close && b.low <= b.open)
        }
    }
}

class AiInsightUiTest {
    @Test fun `maps insight to ui model`() {
        val insight = AiInsight(
            summary = "震荡向上",
            score = 72,
            trend = Trend.SIDEWAYS,
            signals = listOf(AiSignal("MA_BREAK", "金叉")),
        )
        val ui = insight.toUi()
        assertEquals(72, ui.score)
        assertEquals("震荡", ui.trendLabel)
        assertEquals(1, ui.signals.size)
    }
}