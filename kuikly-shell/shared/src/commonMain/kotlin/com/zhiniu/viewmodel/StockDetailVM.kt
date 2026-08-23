/* 知牛 · StockDetailVM（Kuikly 响应式，含 AI 诊股）*/
package com.zhiniu.viewmodel

import com.tencent.kuikly.ref.observable.observable
import com.zhiniu.domain.model.AiInsight
import com.zhiniu.domain.model.AppError
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote
import com.zhiniu.domain.repository.ChatRepository
import com.zhiniu.domain.repository.MarketRepository
import com.zhiniu.domain.usecase.DiagnoseStock
import com.zhiniu.domain.usecase.GetStockDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class StockDetailVM(
    private val scope: CoroutineScope,
    repo: MarketRepository,
    chat: ChatRepository,
    private val symbol: String,
    private val name: String,
) {
    private val detail = GetStockDetail(repo)
    private val diagnose = DiagnoseStock(chat)

    val quote = observable<Quote?>(null)
    val kline = observable<List<KLineBar>>(emptyList())
    val scale = observable(240)
    val loading = observable(true)
    val errorMsg = observable<String?>(null) // display = "[code] 中文分级文案"

    // AI 诊股
    val insight = observable<AiInsight?>(null)
    val diagnosing = observable(false)
    val aiError = observable<String?>(null)

    init { load() }

    fun load() {
        loading.value = true; errorMsg.value = null
        scope.launch {
            runCatching {
                val (q, k) = detail.execute(symbol, scale.value)
                q ?: error("无该标的数据")
                quote.value = q; kline.value = k
            }.onSuccess { loading.value = false }
                .onFailure { e -> loading.value = false; errorMsg.value = AppError.fromThrowable(e).display() }
        }
    }

    fun switchScale(s: Int) { scale.value = s; load() }

    fun diagnose() {
        diagnosing.value = true; aiError.value = null
        scope.launch {
            runCatching { diagnose.execute(symbol, name) }
                .onSuccess { insight.value = it }
                .onFailure { e -> aiError.value = AppError.fromThrowable(e).display() }
                .also { diagnosing.value = false }
        }
    }
}