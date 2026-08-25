// 知牛 · Mock 兜底数据源（仅 // 注释，避免 KMP/JS 词法对 /* 的未闭合误判）
// 断网/无 Token 时渲染，与 data/mock/*.json 语义一致；直接构造对象、跨端可编译。
package com.zhiniu.data.mock

import com.zhiniu.domain.model.BidAsk
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote

class MockDataSource {

    /** 首页主表完整股票池：7 只核心 + 21 只补充（确定性数值）。 */
    fun stocks(): List<Quote> {
        val core = listOf(
            stock("sh600519", "贵州茅台", 1292.83, 1291.50, 1289.40, 1295.00, 1270.01),
            stock("sz000001", "平安银行", 11.30, 11.15, 11.20, 11.38, 11.12),
            stock("sh600036", "招商银行", 36.28, 35.80, 36.10, 36.45, 35.90),
            stock("sz300750", "宁德时代", 196.80, 193.50, 195.00, 198.00, 193.10),
            stock("sh601318", "中国平安", 48.35, 47.90, 48.20, 48.66, 47.80),
            stock("sz000858", "五粮液", 127.60, 129.10, 128.00, 130.00, 126.80),
            stock("sh601398", "工商银行", 5.64, 5.58, 5.60, 5.65, 5.57),
        )
        val extra = listOf(
            stock("sh600276", "恒瑞医药", 45.20, 44.60, 44.80, 45.60, 44.50),
            stock("sh601012", "隆基绿能", 17.84, 18.20, 18.05, 18.30, 17.72),
            stock("sz002594", "比亚迪", 245.60, 242.10, 243.00, 247.90, 241.20),
            stock("sz000002", "万科A", 7.92, 8.05, 8.00, 8.10, 7.86),
            stock("sz000651", "格力电器", 41.35, 40.90, 41.00, 41.60, 40.80),
            stock("sz000333", "美的集团", 62.10, 61.40, 61.60, 62.50, 61.20),
            stock("sz300059", "东方财富", 14.02, 13.70, 13.80, 14.15, 13.72),
            stock("sh600030", "中信证券", 23.46, 23.10, 23.20, 23.60, 23.05),
            stock("sh603259", "药明康德", 58.70, 59.30, 59.00, 59.80, 58.40),
            stock("sz002475", "立讯精密", 33.80, 33.10, 33.30, 34.05, 33.05),
            stock("sh600690", "海尔智家", 28.15, 27.80, 27.90, 28.40, 27.70),
            stock("sh601888", "中国中免", 68.90, 70.20, 69.80, 70.90, 68.50),
            stock("sh600031", "三一重工", 16.53, 16.20, 16.30, 16.70, 16.15),
            stock("sz000568", "泸州老窖", 132.40, 134.20, 133.50, 135.00, 131.80),
            stock("sh600809", "山西汾酒", 196.30, 198.50, 197.80, 199.60, 195.40),
            stock("sz002714", "牧原股份", 44.86, 43.90, 44.10, 45.10, 43.80),
            stock("sh603501", "韦尔股份", 102.30, 100.80, 101.20, 103.00, 100.50),
            stock("sz300124", "汇川技术", 62.84, 62.10, 62.30, 63.20, 61.90),
            stock("sh600436", "片仔癀", 232.60, 235.40, 234.50, 236.80, 231.50),
            stock("sz300015", "爱尔眼科", 14.35, 14.60, 14.50, 14.72, 14.28),
            stock("sh600900", "长江电力", 28.44, 28.20, 28.30, 28.60, 28.12),
            stock("sh600887", "伊利股份", 27.96, 28.30, 28.15, 28.50, 27.85),
        )
        return core + extra
    }

    fun indices(): List<Quote> = listOf(
        index("sh000001", "上证指数", 3245.13, 3232.68, 12.45),
        index("sz399001", "深证成指", 10420.31, 10360.19, 60.12),
        index("sz399006", "创业板指", 2080.45, 2061.79, 18.66),
    )

    fun quotes(): List<Quote> = indices() + stocks()

    /** 按 symbol 取股票（含指数兜底）。 */
    fun quoteOf(symbol: String): Quote? = quotes().firstOrNull { it.symbol == symbol }

    // 任意 symbol 的确定性日 K（60 根，基于 symbol 种子生成，均线/形态稳定）
    fun kline(symbol: String): List<KLineBar> {
        val n = 60
        val seed = (h(symbol) % 997).toInt()
        val base = when (symbol) {
            "sh600519" -> 1290.0
            else -> 20.0 + (seed % 280)
        }
        var price = base * 0.86
        val bars = ArrayList<KLineBar>(n)
        // 日期标签：最右(最新)为 08-21，向左逐日回退（演示数据，忽略周末）
        for (i in 0 until n) {
            val drift = kotlin.math.sin(i * 0.31 + seed % 13) * (base * 0.006)
            val open = price
            val amplitude = base * (0.008 + (seed % 3) * 0.002)
            val close = open + drift + kotlin.math.sin(i * 0.9 + seed % 7) * amplitude * 0.5
            val high = maxOf(open, close) + amplitude * (0.3 + (seed + i) % 5 * 0.12)
            val low = minOf(open, close) - amplitude * (0.3 + (seed * 3 + i) % 4 * 0.14)
            val vol = 180_000L + ((seed * (i + 3)) % 1_800_000L)
            var back = n - 1 - i
            var mm = 8
            var dd = 21
            while (back > 0) {
                dd -= 1
                if (dd <= 0) { mm -= 1; dd = 30 }
                back -= 1
            }
            val label = (if (mm < 10) "0$mm" else "$mm") + "-" + (if (dd < 10) "0$dd" else "$dd")
            bars.add(KLineBar(label, open, high, low, close, vol))
            price = close
        }
        // 末根对齐现价（列表价）
        val last = bars.last()
        bars[n - 1] = last.copy(close = quoteOf(symbol)?.price ?: last.close)
        return bars
    }

    companion object {
        private fun stock(
            symbol: String, name: String, price: Double, prev: Double,
            open: Double, high: Double, low: Double,
        ) = Quote(
            symbol = symbol, name = name,
            open = open, prevClose = prev, price = price, high = high, low = low,
            buy1 = price, sell1 = price + 0.01,
            volume = 200_000L + (h(symbol) % 700L) * 1000L,
            amount = 1_600_000_000.0 + (h(symbol) % 900) * 100_000_000.0,
            bids = listOf(BidAsk(price, 1000)), asks = listOf(BidAsk(price + 0.01, 800)),
            date = "2026-08-21", time = "15:00:00",
        )

        private fun h(s: String): Long {
            var x = 7L
            for (c in s) { x = x * 31 + c.code }
            return x and Long.MAX_VALUE
        }

        /** 指数 → 行情条。 */
        private fun index(symbol: String, name: String, price: Double, prev: Double, chg: Double) =
            stock(symbol, name, price, prev, price - chg, price + kotlin.math.abs(chg) * 2, price - kotlin.math.abs(chg) * 2)
    }
}