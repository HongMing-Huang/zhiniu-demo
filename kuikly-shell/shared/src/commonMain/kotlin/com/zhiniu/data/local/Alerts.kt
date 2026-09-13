/* 知牛 · 价格预警（AI 可经 ⟦TOOL⟧ 指令设置；内存态 + 页面层 SharedPreferences 持久化双写）。
 * 纯逻辑可单测：check(quotes) 只依据传入行情标记触发，不自行取数；
 * 持久化走与 Watchlist/Todo 相同模式：页面增删后 serialize() 写 SP，进页 restore()。
 */
package com.zhiniu.data.local

import com.zhiniu.domain.model.StockQuote
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PriceAlert(
    val id: String,
    val symbol: String,
    val name: String,
    val operator: String,   // above=突破 | below=跌破
    val price: Double,
    val triggered: Boolean = false,
)

object AlertStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val alerts = mutableListOf<PriceAlert>()
    private var idSeq = 0

    fun alerts(): List<PriceAlert> = alerts.toList()

    fun add(symbol: String, name: String, operator: String, price: Double): PriceAlert {
        // 同标的同方向旧预警覆盖（避免 AI/用户重复设置堆叠）
        removeAll { it.symbol == symbol && it.operator == operator }
        val alert = PriceAlert(
            id = "alert-${idSeq++.toString(36)}-${(symbol.hashCode() and 0xffff).toString(36)}",
            symbol = symbol,
            name = name.ifBlank { symbol },
            operator = operator,
            price = price,
        )
        alerts.add(0, alert)
        return alert
    }

    fun remove(id: String) {
        alerts.removeAll { it.id == id }
    }

    fun removeAll(predicate: (PriceAlert) -> Boolean) {
        alerts.removeAll(predicate)
    }

    /** 依据传入行情标记触发（幂等：已触发的不再重复返回）。 */
    fun check(quotes: List<StockQuote>): List<PriceAlert> {
        val bySymbol = quotes.associateBy { it.symbol }
        val fired = mutableListOf<PriceAlert>()
        alerts.indices.toList().forEach { index ->
            val alert = alerts[index]
            if (alert.triggered) return@forEach
            val quote = bySymbol[alert.symbol] ?: return@forEach
            val hit = if (alert.operator == "above") quote.price >= alert.price else quote.price <= alert.price
            if (hit) {
                alerts[index] = alert.copy(triggered = true)
                fired += alerts[index]
            }
        }
        return fired
    }

    fun restore(list: List<PriceAlert>) {
        alerts.clear()
        alerts.addAll(list.filter { it.symbol.length == 8 && it.price > 0.0 })
    }

    fun serialize(): String = runCatching { json.encodeToString(alerts.toList()) }.getOrDefault("[]")

    fun deserialize(text: String): List<PriceAlert> = runCatching {
        json.decodeFromString<List<PriceAlert>>(text)
    }.getOrDefault(emptyList())
}
