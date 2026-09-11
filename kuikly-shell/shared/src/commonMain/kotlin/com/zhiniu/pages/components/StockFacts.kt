/* 知牛 · 详情页财务展示模型：只映射网关真实字段，不推算财务数据。 */
package com.zhiniu.pages.components

import com.zhiniu.domain.model.StockFundamentals
import com.zhiniu.domain.model.StockQuote

data class StockFacts(
    val pe: Double? = null,
    val pb: Double? = null,
    val marketCap: Double? = null,
    val turnover: Double? = null,
    val volumeRatio: Double? = null,
)

fun factsOf(fundamentals: StockFundamentals?): StockFacts = StockFacts(
    pe = fundamentals?.pe,
    pb = fundamentals?.pb,
    marketCap = fundamentals?.marketCap,
    turnover = fundamentals?.turnoverRate,
)

/** 行情快照（东财 ulist 补充的市值/换手/量比）优先，估值类字段来自基本面接口；两者都缺则为 null → "—"。 */
fun factsOf(quote: StockQuote, fundamentals: StockFundamentals?): StockFacts = StockFacts(
    pe = fundamentals?.pe,
    pb = fundamentals?.pb,
    marketCap = quote.marketCap ?: fundamentals?.marketCap,
    turnover = quote.turnoverRate ?: fundamentals?.turnoverRate,
    volumeRatio = quote.volumeRatio,
)

fun fmtOptional(value: Double?, suffix: String = ""): String =
    value?.let { fmt2(it) + suffix } ?: "—"

fun fmtOptionalAmount(value: Double?): String = value?.let(::fmtAmount) ?: "—"
