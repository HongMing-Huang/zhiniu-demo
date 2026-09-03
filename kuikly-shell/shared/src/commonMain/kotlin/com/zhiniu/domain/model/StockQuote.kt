/* 知牛 · 行情领域模型（UI 只依赖本文件类型；Mock/Real 差异封装在 Repository 层） */
package com.zhiniu.domain.model

import kotlinx.serialization.Serializable

/** 单只股票实时报价（含演示用拼音字段，供搜索匹配）。 */
@Serializable
data class StockQuote(
    val symbol: String,          // 带市场前缀，如 sh600519
    val name: String,
    val pinyin: String = "",     // 全拼小写（搜索用），如 maotai
    val open: Double,
    val prevClose: Double,
    val price: Double,
    val high: Double,
    val low: Double,
    val buy1: Double = 0.0,
    val sell1: Double = 0.0,
    val volume: Long,            // 手
    val amount: Double,          // 元
    val date: String = "",
    val time: String = "",
) {
    val change: Double get() = price - prevClose
    val changePercent: Double
        get() = if (prevClose != 0.0) (change / prevClose) * 100 else 0.0
    /** 红涨绿跌：严格大于为涨。 */
    val isUp: Boolean get() = price > prevClose
    /** 展示代码，如 600519；市场后缀 SH/SZ。 */
    val code: String get() = symbol.drop(2)
    val marketSuffix: String get() = symbol.take(2).uppercase()
}

/** 市场指数（Pulse 区）。 */
@Serializable
data class MarketIndex(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
    val spark: List<Double> = emptyList(),
) {
    val isUp: Boolean get() = changePercent >= 0
}

/** 市场宽度（涨跌家数 + 两市成交额）。 */
@Serializable
data class MarketBreadth(
    val upCount: Long,
    val downCount: Long,
    val amountYi: Long,   // 两市成交额（亿）
    val status: String,   // 偏强 / 中性 / 偏弱
) {
    val total: Long get() = upCount + downCount
    val upRatio: Double get() = if (total == 0L) 0.0 else upCount.toDouble() / total
}

/** K 线单根。字段对齐新浪 getKLineData {day,open,high,low,close,volume}；分时复用 day 作 HH:mm。 */
@Serializable
data class Candle(
    val day: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)

/** 技术指标快照（AI 卡片 / Context Rail 用）。 */
@Serializable
data class TechnicalIndicator(
    val name: String,     // MA5 / RSI14 / PE ...
    val value: Double,
    val detail: String = "",
)

/** K 线周期（P0 仅四档）。 */
enum class Timeframe(val label: String) {
    INTRADAY("分时"),
    DAY("日K"),
    WEEK("周K"),
    MONTH("月K"),
}

/** 会话（AI Research 左侧列表）。 */
@Serializable
data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: String,
)
