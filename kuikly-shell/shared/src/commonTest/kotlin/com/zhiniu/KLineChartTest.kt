/* 知牛 · KLineChart 纯逻辑单元测试（commonTest，无 Kuikly 依赖）
 * 覆盖：MA5/10/20 / 价格区间 / 蜡烛方向(A股红涨绿跌) / 像素几何 / 空表兜底。
 */
package com.zhiniu

import com.zhiniu.domain.model.CandleDirection
import com.zhiniu.domain.model.Candle
import com.zhiniu.domain.model.KLineChart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KLineChartTest {

    private fun bar(open: Double, close: Double, high: Double, low: Double) =
        Candle("2026-01-01", open, high, low, close, 100L)

    @Test fun `ma5 correct windowed average`() {
        val closes = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        val ma = KLineChart.movingAverage(closes, 5)
        assertNull(ma[3], "前 4 项样本不足应为 null")
        assertEquals(3.0, ma[4]!!)
        assertEquals(4.0, ma[5]!!)
    }

    @Test fun `ma10 null until enough samples`() {
        val ma = KLineChart.movingAverage(List(9) { 1.0 }, 10)
        assertEquals(9, ma.count { it == null })
    }

    @Test fun `price bounds pads around range`() {
        val bars = listOf(bar(10.0, 20.0, 22.0, 8.0))
        val (lo, hi) = KLineChart.priceBounds(bars)
        assertTrue(lo < 8.0 && hi > 22.0, "应留 8% 空边")
    }

    @Test fun `empty bounds return safe default`() {
        val (lo, hi) = KLineChart.priceBounds(emptyList())
        assertEquals(0.0, lo); assertEquals(1.0, hi)
    }

    @Test fun `a-share color up red down green`() {
        assertEquals(CandleDirection.UP, KLineChart.candleDirection(10.0, 11.0))
        assertEquals(CandleDirection.DOWN, KLineChart.candleDirection(11.0, 10.0))
        assertEquals(CandleDirection.FLAT, KLineChart.candleDirection(10.0, 10.0))
    }

    @Test fun `layout maps all bars to pix and keeps high north`() {
        val bars = listOf(
            bar(10.0, 11.0, 12.0, 9.0),
            bar(11.0, 10.0, 11.5, 8.5),
        )
        val geo = KLineChart.layout(bars, width = 240f, height = 120f)
        assertEquals(2, geo.size)
        // 影线上沿(低 y) 应高于实体上沿；实体像素高度为正
        geo.forEach { g ->
            assertTrue(g.wickHigh <= g.bodyTop, "最高价影线 y 应 ≤ 实体上沿 y")
            assertTrue(g.wickLow >= g.bodyBottom, "最低价影线 y 应 ≥ 实体下沿 y")
            assertTrue(g.bodyBottom > g.bodyTop, "实体应有一定像素高度")
        }
    }

    @Test fun `empty layout returns empty`() {
        assertTrue(KLineChart.layout(emptyList(), 240f, 120f).isEmpty())
    }

    @Test fun `bundle exposes ma alignment`() {
        // 20 根递增 close → MA 逐根存在
        val bars = (1..20).map { bar(it.toDouble(), it.toDouble(), it + 1.0, it - 1.0) }
        val b = KLineChart.buildBundle(bars, 240f, 120f)
        assertEquals(20, b.ma5.size)
        assertEquals(20, b.ma10.size)
        assertEquals(20, b.ma20.size)
        assertTrue(b.ma20[19] != null)
        assertTrue(b.ma5[0] == null)
    }
}