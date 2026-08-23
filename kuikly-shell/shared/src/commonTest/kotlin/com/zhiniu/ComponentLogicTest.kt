/* 知牛 · UI 组件纯逻辑测试（commonTest，无 Kuikly 依赖）
 * 覆盖：CandlestickPaint 几何映射回填 / 评分色条占比 / 信号&风险模型装配 / 总结卡装配。
 */
package com.zhiniu

import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Trend
import com.zhiniu.pages.components.buildCandlestickPaint
import com.zhiniu.pages.components.risksToBadges
import com.zhiniu.pages.components.scoreBarFraction
import com.zhiniu.pages.components.signalsToPills
import com.zhiniu.domain.model.AiSignal
import com.zhiniu.domain.model.AiRisk
import com.zhiniu.domain.model.AiInsight
import com.zhiniu.pages.components.toSummaryUi
import com.zhiniu.pages.components.riskColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComponentLogicTest {

    private val bars = (1..20).map {
        KLineBar("2026-08-22", it.toDouble(), it.toDouble() + 1, it.toDouble() - 1, it.toDouble(), 100L)
    }

    @Test fun `paint maps all candles with up fill`() {
        val p = buildCandlestickPaint(bars, widthPx = 240f, heightPx = 120f, showMA = true)
        assertEquals(20, p.candles.size)
        assertEquals(20, p.ma5.size)
        assertEquals(20, p.ma10.size)
        assertEquals(20, p.ma20.size)
        assertEquals(com.zhiniu.pages.components.Palette.FLAT, p.candles[0].fill) // open==close → 平盘灰
    }

    @Test fun `paint without ma yields empty ma lists`() {
        val p = buildCandlestickPaint(bars, 240f, 120f, showMA = false)
        assertTrue(p.ma5.isEmpty() && p.ma10.isEmpty() && p.ma20.isEmpty())
        assertEquals(20, p.candles.size)
    }

    @Test fun `score bar fraction clamps`() {
        assertEquals(0f, scoreBarFraction(0))
        assertEquals(0.5f, scoreBarFraction(50))
        assertEquals(1f, scoreBarFraction(100))
        assertEquals(1f, scoreBarFraction(120))
    }

    @Test fun `signals limited to six pills`() {
        val pills = signalsToPills(List(9) { AiSignal("S", "detail") })
        assertEquals(6, pills.size)
    }

    @Test fun `risk badges uppercase and map color`() {
        val badges = risksToBadges(listOf(AiRisk("high", "波动")))
        assertEquals("HIGH", badges[0].level)
        assertEquals(com.zhiniu.pages.components.Palette.UP, riskColor("HIGH"))
        assertEquals("#f5a623", riskColor("MEDIUM"))
        assertEquals("#f7c948", riskColor("LOW"))
    }

    @Test fun `summary ui assembly clamps score and reuses trend label`() {
        val insight = AiInsight(summary = "震荡", score = 120, trend = Trend.UP)
        val ui = insight.toSummaryUi()
        assertTrue(ui.score <= 100)
        assertEquals("看多", ui.trendLabel)
    }
}