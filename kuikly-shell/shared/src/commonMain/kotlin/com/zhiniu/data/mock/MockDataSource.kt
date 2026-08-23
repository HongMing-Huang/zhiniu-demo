// 知牛 · Mock 兜底数据源（仅 // 注释，避免 KMP/JS 词法对 /* 的未闭合误判）
// 断网/无 Token 时渲染，与 data/mock/*.json 语义一致；直接构造对象、跨端可编译。
package com.zhiniu.data.mock

import com.zhiniu.domain.model.BidAsk
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote

class MockDataSource {

    fun quotes(): List<Quote> = listOf(
        index("sh000001", "上证指数", 3245.13, 3232.68, 12.45),
        index("sz399001", "深证成指", 10420.31, 10360.19, 60.12),
        index("sz399006", "创业板指", 2080.45, 2061.79, 18.66),
        stock("sh600519", "贵州茅台", 1292.83, 1291.50, 1272.00, 1295.00, 1270.01),
        stock("sz000001", "平安银行", 11.30, 11.15, 11.20, 11.38, 11.12),
        stock("sh600036", "招商银行", 36.28, 35.80, 36.10, 36.45, 35.90),
        stock("sz300750", "宁德时代", 196.80, 193.50, 195.00, 198.00, 193.10),
        stock("sh601318", "中国平安", 48.35, 47.90, 48.20, 48.66, 47.80),
        stock("sz000858", "五粮液", 127.60, 129.10, 128.00, 130.00, 126.80),
        stock("sh601398", "工商银行", 5.64, 5.58, 5.60, 5.65, 5.57),
    )

    // 20 根日 K（sh600519 演示序列）
    fun kline(symbol: String): List<KLineBar> {
        val opens = doubleArrayOf(
            1290.0, 1305.0, 1300.0, 1295.0, 1292.0, 1285.0, 1290.0, 1288.0, 1280.0, 1276.0,
            1282.0, 1290.0, 1300.0, 1296.0, 1292.0, 1288.0, 1282.0, 1285.0, 1280.0, 1278.0,
        )
        val closes = doubleArrayOf(
            1305.0, 1300.0, 1295.0, 1292.0, 1285.0, 1290.0, 1288.0, 1280.0, 1276.0, 1282.0,
            1290.0, 1300.0, 1296.0, 1292.0, 1288.0, 1282.0, 1285.0, 1280.0, 1279.0, 1292.83,
        )
        val days = arrayOf(
            "07-27", "07-28", "07-29", "07-30", "07-31", "08-03", "08-04", "08-05", "08-06", "08-07",
            "08-10", "08-11", "08-12", "08-13", "08-14", "08-17", "08-18", "08-19", "08-20", "08-21",
        )
        return opens.indices.map { i ->
            val o = opens[i]; val c = closes[i]
            KLineBar(
                day = "2026-" + days[i],
                open = o,
                high = maxOf(o, c) + 4.0,
                low = minOf(o, c) - 4.0,
                close = c,
                volume = (26000L + i * 700L),
            )
        }
    }

    private fun index(symbol: String, name: String, price: Double, prev: Double, chg: Double) =
        stock(symbol, name, price, prev, price - chg, price + kotlin.math.abs(chg) * 2, price - kotlin.math.abs(chg) * 2)

    private fun stock(
        symbol: String, name: String, price: Double, prev: Double,
        open: Double, high: Double, low: Double,
    ) = Quote(
        symbol = symbol, name = name,
        open = open, prevClose = prev, price = price, high = high, low = low,
        buy1 = price, sell1 = price + 0.01,
        volume = 300_000L, amount = 3_000_000_000.0,
        bids = listOf(BidAsk(price, 1000)), asks = listOf(BidAsk(price + 0.01, 800)),
        date = "2026-08-21", time = "15:00:00",
    )
}