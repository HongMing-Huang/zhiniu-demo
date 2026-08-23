/* 知牛 · QuoteDisplay 纯逻辑单元测试（commonTest，无 Kuikly / java.util 依赖）
 * 覆盖：价格补零 / 涨跌幅正负号 / 量-额单位 / 五档中文档位。
 */
package com.zhiniu

import com.zhiniu.domain.model.BidAsk
import com.zhiniu.domain.model.QuoteDisplay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuoteDisplayTest {

    @Test fun `price two decimals with trailing zero`() {
        assertEquals("11.30", QuoteDisplay.price(11.3))
        assertEquals("1292.83", QuoteDisplay.price(1292.833))
        assertEquals("-1.25", QuoteDisplay.price(-1.25))
    }

    @Test fun `pct keeps sign and percent`() {
        assertEquals("+5.20%", QuoteDisplay.pct(5.2))
        assertEquals("-2.30%", QuoteDisplay.pct(-2.3))
        assertEquals("0.00%", QuoteDisplay.pct(0.0))
    }

    @Test fun `volume unit hand wan yi`() {
        assertEquals("3347手", QuoteDisplay.volume(3347))
        assertEquals("3.35万手", QuoteDisplay.volume(33472))
        assertEquals("2.10亿手", QuoteDisplay.volume(210000000))
        assertEquals("0", QuoteDisplay.volume(-1))
    }

    @Test fun `amount from wan yi wan yuan`() {
        assertEquals("42.78亿", QuoteDisplay.amountFromWan(427800.0))
        assertEquals("4278.30万", QuoteDisplay.amountFromWan(4278.3))
        assertEquals("1200元", QuoteDisplay.amountFromWan(0.12))
    }

    @Test fun `depth rows chinese levels buy then sell`() {
        val bids = (1..5).map { BidAsk(it.toDouble(), 100L) }
        val asks = (6..10).map { BidAsk(it.toDouble(), 50L) }
        val rows = QuoteDisplay.depthRows(bids, asks)
        assertEquals(10, rows.size)
        assertEquals("一", rows[0].level); assertEquals("买", rows[0].side)
        assertEquals("五", rows[4].level)
        assertEquals("一", rows[5].level); assertEquals("卖", rows[5].side)
        assertEquals("五", rows[9].level)
    }

    @Test fun `depth caps at five per side`() {
        val many = List(8) { BidAsk(it.toDouble(), 1L) }
        assertEquals(5, QuoteDisplay.depthRows(many, emptyList()).count { it.side == "买" })
    }

    @Test fun `widgets pricePct stays kmp safe`() {
        val quote = com.zhiniu.domain.model.Quote(
            "sh600519", "贵州茅台", 100.0, 100.0, 105.2, 100.0, 100.0, 100.0, 100.0, 0L, 0.0,
        )
        assertTrue(com.zhiniu.pages.components.pricePct(quote).startsWith("+5.20%"))
        assertEquals("105.20", com.zhiniu.pages.components.priceValue(quote))
    }
}