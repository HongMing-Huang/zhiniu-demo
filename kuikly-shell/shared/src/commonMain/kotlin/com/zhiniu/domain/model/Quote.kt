/* 知牛 · 行情模型（Quote + KLineBar）*/
package com.zhiniu.domain.model

import kotlinx.serialization.Serializable

/** 单只股票/指数实时报价。字段源自新浪 hq.sinajs.cn 真实结构（索引见 api-matrix.md）。 */
@Serializable
data class Quote(
    val symbol: String,          // 带前缀，如 sh600519 / s_sh000001
    val name: String,
    val open: Double,
    val prevClose: Double,
    val price: Double,
    val high: Double,
    val low: Double,
    val buy1: Double,
    val sell1: Double,
    val volume: Long,            // 手
    val amount: Double,          // 元
    val bids: List<BidAsk> = emptyList(),
    val asks: List<BidAsk> = emptyList(),
    val date: String = "",
    val time: String = "",
) {
    val change: Double get() = price - prevClose
    val changePercent: Double
        get() = if (prevClose != 0.0) (change / prevClose) * 100 else 0.0
    /** 红涨绿跌：严格大于为涨，等于为中性（flat）。 */
    val isUp: Boolean get() = price > prevClose
}

/** 盘口一档。 */
@Serializable
data class BidAsk(val price: Double, val volume: Long)

/** K 线单根。字段对齐新浪 getKLineData 返回 {day,open,high,low,close,volume}。 */
@Serializable
data class KLineBar(
    val day: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)