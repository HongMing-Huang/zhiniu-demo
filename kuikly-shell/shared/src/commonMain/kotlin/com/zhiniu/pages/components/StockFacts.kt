/* 知牛 · 个股关键数据（确定性演示值）与稳定哈希 */
package com.zhiniu.pages.components

import com.zhiniu.domain.model.StockQuote

/** 稳定哈希（同 symbol 恒定）。 */
internal fun stableHash(s: String): Int {
    var h = 7
    for (c in s) { h = h * 31 + c.code }
    return h and Int.MAX_VALUE
}

/** 个股关键数据（演示）。 */
data class StockFacts(
    val pe: Double,          // 市盈率
    val pb: Double,          // 市净率
    val marketCap: String,   // 总市值
    val turnover: Double,    // 换手率 %
    val volumeRatio: Double, // 量比
)

fun factsOf(q: StockQuote): StockFacts {
    val h = stableHash(q.symbol)
    val pe = 12.0 + (h % 300) / 10.0
    val pb = 1.2 + (h % 90) / 10.0
    val turnover = 0.25 + (h % 160) / 100.0
    val vr = 0.7 + (h % 130) / 100.0
    val cap = q.amount * (9 + (h % 40))
    val capStr = if (cap >= 1e12) fmt2(cap / 1e12) + "T" else fmt2(cap / 1e8) + "亿"
    return StockFacts(
        pe = pe, pb = pb,
        marketCap = capStr,
        turnover = turnover, volumeRatio = vr,
    )
}
