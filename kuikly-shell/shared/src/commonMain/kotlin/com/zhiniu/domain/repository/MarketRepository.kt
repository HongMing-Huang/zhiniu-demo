/* 知牛 · Repository 接口与实现 */
package com.zhiniu.domain.repository

import com.zhiniu.data.local.WatchlistStore
import com.zhiniu.data.mock.MockDataSource
import com.zhiniu.data.remote.SinaKlineApi
import com.zhiniu.data.remote.SinaQuoteApi
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote

/** 行情仓储：真实(新浪) → Mock 兜底。 */
interface MarketRepository {
    suspend fun quotes(symbols: List<String>): List<Quote>
    suspend fun kline(symbol: String, scale: Int = 240, datalen: Int = 180): List<KLineBar>
    val watchlist: WatchlistStore
}

class SinaMarketRepository(
    private val quoteApi: SinaQuoteApi,
    private val klineApi: SinaKlineApi,
    override val watchlist: WatchlistStore,
    private val mock: MockDataSource = MockDataSource(),
) : MarketRepository {

    override suspend fun quotes(symbols: List<String>): List<Quote> = runCatching {
        quoteApi.fetch(*symbols.toTypedArray())
    }.getOrElse {
        // 网络/解析失败 → Mock 兜底（演示绝不空屏）
        mock.quotes().filter { it.symbol in symbols }
    }

    override suspend fun kline(symbol: String, scale: Int, datalen: Int): List<KLineBar> =
        runCatching { klineApi.fetch(symbol, scale, datalen) }
            .getOrElse {
                mock.kline(symbol)
            }
}