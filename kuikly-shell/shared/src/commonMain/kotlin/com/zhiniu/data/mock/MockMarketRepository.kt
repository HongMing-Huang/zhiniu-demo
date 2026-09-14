/* 知牛 · Mock 行情仓储（确定性数据：固定 Seed，刷新/录屏结果一致）
 * 规则：high >= max(open, close)；low <= min(open, close)；volume >= 0。
 * 接真实行情时替换 MarketStore.repository（见 docs/ARCHITECTURE.md）。
 */
package com.zhiniu.data.mock

import com.zhiniu.domain.model.Candle
import com.zhiniu.domain.model.MarketBreadth
import com.zhiniu.domain.model.MarketIndex
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.domain.model.Timeframe
import com.zhiniu.domain.repository.MarketRepository
import kotlin.math.sin

class MockMarketRepository : MarketRepository {

    private val liveQuoteOverrides = mutableMapOf<String, StockQuote>()

    /** 将网关返回的最新报价合并进同步仓储，让搜索、详情与 AI 共用同一份价格。 */
    fun applyLiveQuotes(quotes: List<StockQuote>) {
        val base = marketFile.stocks.associateBy({ it.symbol }, { it.pinyin })
        quotes.forEach { q ->
            liveQuoteOverrides[q.symbol] = if (q.pinyin.isBlank()) q.copy(pinyin = base[q.symbol].orEmpty()) else q
        }
    }

    // ---------------- 股票池 / 指数 / 宽度：全部由 mock_market.json 数据文件驱动 ----------------
    private val marketFile: MockMarketFile by lazy { mockMarketFile }

    override fun stockQuotes(): List<StockQuote> = marketFile.stocks.map { it.toQuote() }

    private fun MockStock.toQuote() = StockQuote(
        symbol = symbol, name = name, pinyin = pinyin,
        open = open, prevClose = prevClose, price = price, high = high, low = low,
        buy1 = price, sell1 = price + 0.01,
        volume = volume, amount = amount,
        date = "2026-08-21", time = "15:00:00",
    )

    override fun indices(): List<MarketIndex> = marketFile.indices.map {
        MarketIndex(it.symbol, it.name, it.price, it.changePercent, sparkOf(it.symbol))
    }

    override fun breadth(): MarketBreadth = marketFile.breadth.let {
        MarketBreadth(
            upCount = it.upCount.toLong(), downCount = it.downCount.toLong(),
            amountYi = it.amountYi.toLong(), status = it.status,
        )
    }

    override fun quoteOf(symbol: String): StockQuote? =
        stockQuotes().firstOrNull { it.symbol == symbol || it.code == symbol }

    // ---------------- K 线（确定性：LCG 种子 + 平滑随机游走） ----------------
    private val candleCache = mutableMapOf<String, List<Candle>>()

    override fun candles(symbol: String, timeframe: Timeframe): List<Candle> {
        val key = "$symbol|${timeframe.name}"
        candleCache[key]?.let { return it }
        val bars = when (timeframe) {
            Timeframe.INTRADAY -> intraday(symbol)
            Timeframe.DAY -> daily(symbol)
            Timeframe.WEEK -> aggregate(daily(symbol), 5, 52)
            Timeframe.MONTH -> aggregate(daily(symbol), 21, 36)
        }
        candleCache[key] = bars
        return bars
    }

    override fun spark(symbol: String): List<Double> =
        intraday(symbol).map { it.close }

    /** 120 根确定性日 K：末根 close 对齐报价，全程满足 high/low 约束。 */
    private fun daily(symbol: String): List<Candle> {
        val quote = quoteOf(symbol)
        val target = quote?.price ?: 20.0
        val seed = seedOf(symbol)
        val n = 120
        val out = ArrayList<Candle>(n)
        var price = target * 0.86                     // 从低位向报价收敛，避免末根巨柱
        var s = seed
        for (i in 0 until n) {
            s = nextRand(s)
            val progress = (i + 1).toDouble() / n
            val wave = sin(i * 0.31 + (seed % 13)) * target * 0.006
            val open = price
            val drift = (target - open) * 0.06 + wave * 0.5
            val close = open + drift
            val range = target * (0.006 + (s % 3) * 0.002)
            val high = maxOf(open, close) + range * (0.35 + (s % 5) * 0.1)
            val low = minOf(open, close) - range * (0.35 + (s % 4) * 0.12)
            val volume = 180_000L + (s % 1_900_000L)
            out.add(Candle(dayLabel(n - 1 - i), open, high, low, close, volume))
            price = close
        }
        // 末根：close 精确对齐报价；open 取上一根 close，保证实体比例正常
        val prev = out[n - 2]
        val lastClose = target
        val lastOpen = prev.close
        out[n - 1] = Candle(
            day = "08-21",
            open = lastOpen,
            high = maxOf(lastOpen, lastClose) * 1.0015,
            low = minOf(lastOpen, lastClose) * 0.9985,
            close = lastClose,
            volume = out[n - 1].volume,
        )
        return out
    }

    /** 48 点分时：09:30-15:00 平滑走势。 */
    private fun intraday(symbol: String): List<Candle> {
        val quote = quoteOf(symbol) ?: return emptyList()
        val seed = seedOf(symbol)
        val n = 48
        val out = ArrayList<Candle>(n)
        var price = quote.prevClose
        var s = seed
        val labels = listOf("09:30", "10:30", "11:30", "13:00", "14:00", "15:00")
        for (i in 0 until n) {
            s = nextRand(s)
            val progress = (i + 1).toDouble() / n
            val wave = sin(i * 0.55 + (seed % 11)) * quote.prevClose * 0.0018
            val close = price + (quote.price - quote.prevClose) * 0.04 + wave
            val range = quote.prevClose * 0.0012
            val labelIdx = when {
                i == 0 -> 0
                i == n / 4 -> 1
                i == n / 2 - 1 -> 2
                i == n * 2 / 3 -> 3
                i == n * 5 / 6 -> 4
                i == n - 1 -> 5
                else -> -1
            }
            out.add(
                Candle(
                    day = if (labelIdx >= 0) labels[labelIdx] else "",
                    open = close, high = close + range, low = close - range,
                    close = close, volume = 1_200_000L + (s % 3_000_000L),
                )
            )
            price = close
        }
        return out
    }

    /** 按固定步长聚合日 K 为 周K/月K。 */
    private fun aggregate(daily: List<Candle>, step: Int, maxCount: Int): List<Candle> {
        val out = ArrayList<Candle>()
        var i = daily.size
        while (i > 0 && out.size < maxCount) {
            val start = (i - step).coerceAtLeast(0)
            val group = daily.subList(start, i)
            val first = group.first()
            val last = group.last()
            out.add(
                0,
                Candle(
                    day = last.day,
                    open = first.open,
                    high = group.maxOf { it.high },
                    low = group.minOf { it.low },
                    close = last.close,
                    volume = group.sumOf { it.volume },
                )
            )
            i = start
        }
        return out
    }

    // ---------------- 搜索（名称/拼音/代码/代码+后缀） ----------------
    override fun search(query: String): List<StockQuote> {
        val q = query.trim().lowercase().replace(" ", "")
        if (q.isEmpty()) return emptyList()
        return stockQuotes().filter { it.matches(q) }
    }

    private fun StockQuote.matches(q: String): Boolean {
        val codeLower = code.lowercase()
        val suffixed = "$codeLower.$marketSuffix".lowercase()
        return name.contains(q, ignoreCase = true) ||
            pinyin.contains(q) ||
            codeLower.contains(q) ||
            symbol.lowercase().contains(q) ||
            suffixed.contains(q)
    }

    // ---------------- 工具 ----------------
    private fun seedOf(symbol: String): Long {
        var h = 7L
        for (c in symbol) { h = h * 31 + c.code }
        return h and Long.MAX_VALUE
    }

    private fun nextRand(seed: Long): Long = (seed * 48271) % 2147483647

    /** 末根为 08-21，向前逐日回退（忽略周末，演示数据）。 */
    private fun dayLabel(back: Int): String {
        var mm = 8
        var dd = 21
        var b = back
        while (b > 0) {
            dd -= 1
            if (dd <= 0) { mm -= 1; dd = 30 }
            b -= 1
        }
        return (if (mm < 10) "0$mm" else "$mm") + "-" + (if (dd < 10) "0$dd" else "$dd")
    }

    private fun sparkOf(symbol: String): List<Double> {
        val seed = seedOf(symbol)
        val n = 14
        val out = ArrayList<Double>(n)
        var s = seed
        var v = 0.0
        for (i in 0 until n) {
            s = nextRand(s)
            v += (s % 1000) / 1000.0 - 0.5
            out.add(v)
        }
        return out
    }

}
