/* 知牛 · AlertStore（价格预警）纯逻辑单测：触发判定幂等 / 序列化往返 / 覆盖同标的同方向 / 脏数据过滤。 */
package com.zhiniu

import com.zhiniu.data.local.AlertStore
import com.zhiniu.data.local.PriceAlert
import com.zhiniu.domain.model.StockQuote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlertStoreTest {

    private fun quote(symbol: String, price: Double) = StockQuote(
        symbol = symbol, name = symbol, open = price, prevClose = price, price = price,
        high = price, low = price, buy1 = price, sell1 = price,
        volume = 0L, amount = 0.0,
    )

    @Test
    fun belowAlertFiresWhenPriceDrops() {
        AlertStore.restore(emptyList())
        AlertStore.add("sz300750", "宁德时代", "below", 300.0)
        // 未跌穿：不触发
        assertTrue(AlertStore.check(listOf(quote("sz300750", 330.51))).isEmpty())
        // 跌穿：触发
        val fired = AlertStore.check(listOf(quote("sz300750", 298.0)))
        assertEquals(1, fired.size)
        assertEquals("宁德时代", fired[0].name)
        // 幂等：已触发不再重复
        assertTrue(AlertStore.check(listOf(quote("sz300750", 290.0))).isEmpty())
    }

    @Test
    fun aboveAlertFiresWhenPriceBreaks() {
        AlertStore.restore(emptyList())
        AlertStore.add("sh600519", "贵州茅台", "above", 1300.0)
        val fired = AlertStore.check(listOf(quote("sh600519", 1305.0)))
        assertEquals(1, fired.size)
        assertTrue(fired[0].triggered)
    }

    @Test
    fun quotesMissingSymbolDoNotTrigger() {
        AlertStore.restore(emptyList())
        AlertStore.add("sz300750", "宁德时代", "below", 300.0)
        assertTrue(AlertStore.check(listOf(quote("sh600519", 100.0))).isEmpty())
        assertFalse(AlertStore.alerts()[0].triggered)
    }

    @Test
    fun sameSymbolAndOperatorOverwrites() {
        AlertStore.restore(emptyList())
        AlertStore.add("sz300750", "宁德时代", "below", 300.0)
        AlertStore.add("sz300750", "宁德时代", "below", 280.0)
        assertEquals(1, AlertStore.alerts().size)
        assertEquals(280.0, AlertStore.alerts()[0].price)
        // 不同方向不覆盖
        AlertStore.add("sz300750", "宁德时代", "above", 400.0)
        assertEquals(2, AlertStore.alerts().size)
    }

    @Test
    fun serializeRoundTripAndDirtyDataFiltered() {
        AlertStore.restore(emptyList())
        AlertStore.add("sh600519", "贵州茅台", "above", 1300.0)
        val text = AlertStore.serialize()
        val restored = AlertStore.deserialize(text)
        assertEquals(1, restored.size)
        assertEquals("sh600519", restored[0].symbol)

        // 脏数据：非法 symbol / 非法价格在 restore 时过滤
        AlertStore.restore(
            listOf(
                PriceAlert("a", "bad", "X", "above", 100.0),
                PriceAlert("b", "sh600519", "贵州茅台", "above", -5.0),
                PriceAlert("c", "sh600519", "贵州茅台", "above", 100.0),
            )
        )
        assertEquals(1, AlertStore.alerts().size)

        // 非法 JSON → 空列表（不抛异常）
        assertTrue(AlertStore.deserialize("not-json{").isEmpty())
    }
}
