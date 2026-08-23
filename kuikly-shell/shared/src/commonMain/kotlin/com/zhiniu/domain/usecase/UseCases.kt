/* 知牛 · UseCase 层 */
package com.zhiniu.domain.usecase

import com.zhiniu.data.local.WatchlistStore
import com.zhiniu.domain.model.AiInsight
import com.zhiniu.domain.model.ChatMessage
import com.zhiniu.domain.model.Quote
import com.zhiniu.domain.repository.ChatRepository
import com.zhiniu.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.zhiniu.domain.model.StreamChunk

/** 获取行情列表（指数 + 个股 + 自选）。 */
class GetStockList(private val repo: MarketRepository) {
    suspend fun indices(): List<Quote> = repo.quotes(listOf("sh000001", "sz399001", "sz399006"))
    suspend fun all(): List<Quote> = repo.quotes(WatchDefaults.ALL)
    suspend fun watchlist(): List<Quote> {
        val symbols = repo.watchlist.observe().first()
        return if (symbols.isEmpty()) emptyList() else repo.quotes(symbols.toList())
    }
}

/** 首页默认自选/展示代码。 */
object WatchDefaults {
    val ALL = listOf(
        "sh600519", "sz000001", "sh600036", "sz300750", "sh601318", "sz000858",
        "sh601398", "sh601166", "sz000333",
    )
}

/** 获取个股详情（实时 + K线）。 */
class GetStockDetail(private val repo: MarketRepository) {
    suspend fun execute(symbol: String, scale: Int = 240): Pair<Quote?, List<com.zhiniu.domain.model.KLineBar>> {
        val q = repo.quotes(listOf(symbol)).firstOrNull()
        val k = repo.kline(symbol, scale, 120)
        return q to k
    }
}

/** 对话流式（SSE → StreamChunk）。 */
class AskChat(private val repo: ChatRepository) {
    fun execute(model: String, history: List<ChatMessage>): Flow<StreamChunk> =
        repo.chatStream(model, history)
}

/** 一键诊股。 */
class DiagnoseStock(private val repo: ChatRepository) {
    suspend fun execute(symbol: String, name: String): AiInsight = repo.diagnose(symbol, name)
}

/** 多空辩论（复用诊股数据，模拟双视角）。 */
class Debate(private val insight: AiInsight) {
    data class Viewpoint(val side: String, val title: String, val points: List<String>)
    fun views(): List<Viewpoint> = listOf(
        Viewpoint("多", "看多逻辑", listOf("趋势向上", "机构增持", "量能配合")),
        Viewpoint("空", "看空逻辑", listOf("估值偏高", "波动加大", "获利回吐")),
    )
}