/* 知牛 · 行情仓储接口
 * UI 只依赖本接口；当前由 MockMarketRepository 实现。
 * 接入真实行情（新浪等）时仅需替换实现（见 docs/ARCHITECTURE.md 替换点）。
 */
package com.zhiniu.domain.repository

import com.zhiniu.domain.model.Candle
import com.zhiniu.domain.model.MarketBreadth
import com.zhiniu.domain.model.MarketIndex
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.domain.model.Timeframe

interface MarketRepository {
    /** 全部股票池（市场表）。 */
    fun stockQuotes(): List<StockQuote>

    /** 主要指数（Market Pulse）。 */
    fun indices(): List<MarketIndex>

    /** 市场宽度（涨跌家数 / 成交额）。 */
    fun breadth(): MarketBreadth

    /** 按 symbol 查个股；不存在返回 null。 */
    fun quoteOf(symbol: String): StockQuote?

    /** 指定周期 K 线（分时/日K/周K/月K），确定性数据。 */
    fun candles(symbol: String, timeframe: Timeframe): List<Candle>

    /** 当日迷你走势（表格 sparkline），确定性。 */
    fun spark(symbol: String): List<Double>

    /** 按 名称/拼音/代码/代码+市场后缀 匹配。 */
    fun search(query: String): List<StockQuote>
}
