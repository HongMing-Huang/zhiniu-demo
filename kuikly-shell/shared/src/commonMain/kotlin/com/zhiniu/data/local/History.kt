/* 知牛 · 浏览历史（最近查看的股票）：内存态 + 页面层 SharedPreferences 持久化双写。
 * 与 Watchlist/Alerts 同模式：进页 restore()，增删后 serialize() 写回。
 */
package com.zhiniu.data.local

import com.zhiniu.domain.model.StockQuote

object ViewHistory {
    private val items = mutableListOf<HistoryItem>()
    private const val MAX = 12

    fun items(): List<HistoryItem> = items.toList()

    /** 记录一次浏览（同标的去重置顶；不伪造价格，快照缺失时价格字段由调用方回填）。 */
    fun record(symbol: String, name: String) {
        if (symbol.isBlank()) return
        items.removeAll { it.symbol == symbol }
        items.add(0, HistoryItem(symbol, name.ifBlank { symbol }))
        while (items.size > MAX) items.removeAt(items.size - 1)
    }

    fun remove(symbol: String) {
        items.removeAll { it.symbol == symbol }
    }

    fun clear() = items.clear()

    fun serialize(): String =
        items.joinToString("|") { "${it.symbol},${it.name.replace(",", " ").replace("|", " ")}" }

    fun deserialize(text: String) {
        items.clear()
        text.split("|").filter { it.isNotBlank() }.forEach { chunk ->
            val parts = chunk.split(",", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) {
                items.add(HistoryItem(parts[0], parts[1].ifBlank { parts[0] }))
            }
        }
    }

    /** 用行情快照补价格（我的页展示用；拿不到价格的不补，显示 —）。 */
    fun withPrices(quoteOf: (String) -> StockQuote?): List<HistoryItem> =
        items.map { it.copy(price = quoteOf(it.symbol)?.price) }
}

data class HistoryItem(
    val symbol: String,
    val name: String,
    val price: Double? = null,
)
