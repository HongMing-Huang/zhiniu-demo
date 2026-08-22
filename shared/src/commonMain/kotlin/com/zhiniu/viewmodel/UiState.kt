/* 知牛 · 统一 UI 状态（四态，技术方案 §4.4）*/
package com.zhiniu.viewmodel

import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    data class Loading(val skeleton: Boolean = true) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val code: String, val msg: String, val retryable: Boolean = true) : UiState<Nothing>
}

/* ---- 页面级 State ---- */

data class MarketListState(
    val indices: List<Quote> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val mode: MarketMode = MarketMode.ALL,
)

enum class MarketMode { ALL, WATCHLIST, GAINERS, LOSERS }

data class StockDetailState(
    val quote: Quote? = null,
    val kline: List<KLineBar> = emptyList(),
    val scale: Int = 240,
)

data class ChatUiState(
    val messages: List<com.zhiniu.domain.model.ChatMessage> = listOf(
        com.zhiniu.domain.model.ChatMessage(
            id = "welcome", role = com.zhiniu.domain.model.MessageRole.ASSISTANT,
            content = "你好，我是知牛。可以问我：看大盘 / 诊个股 / 解释指标 / 对比两只。",
        ),
    ),
    val quickCommands: List<String> = listOf("看大盘", "诊个股", "解释指标", "对比两只"),
    val streamingId: String? = null,
)