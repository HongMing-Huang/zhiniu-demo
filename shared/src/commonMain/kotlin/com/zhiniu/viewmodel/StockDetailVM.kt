/* 知牛 · StockDetailVM（含 AI 诊股）*/
package com.zhiniu.viewmodel

import com.zhiniu.domain.model.AiInsight
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote
import com.zhiniu.domain.repository.ChatRepository
import com.zhiniu.domain.repository.MarketRepository
import com.zhiniu.domain.usecase.GetStockDetail
import com.zhiniu.domain.usecase.DiagnoseStock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StockDetailVM(
    private val scope: CoroutineScope,
    repo: MarketRepository,
    chat: ChatRepository,
    private val symbol: String,
    private val name: String,
) {
    private val detailCase = GetStockDetail(repo)
    private val diagnoseCase = DiagnoseStock(chat)

    private val _ui = MutableStateFlow<UiState<StockDetailState>>(UiState.Idle)
    val ui: StateFlow<UiState<StockDetailState>> = _ui.asStateFlow()

    private val _insight = MutableStateFlow<UiState<AiInsight>>(UiState.Idle)
    val insight: StateFlow<UiState<AiInsight>> = _insight.asStateFlow()

    init { load() }

    fun load() {
        _ui.value = UiState.Loading()
        scope.launch {
            runCatching {
                val (q, k) = detailCase.execute(symbol)
                q ?: return@launch run { _ui.value = UiState.Error("NO_DATA", "无该标的数据") }
                StockDetailState(quote = q, kline = k)
            }.onSuccess { _ui.value = UiState.Success(it) }
                .onFailure { e -> _ui.value = UiState.Error("DOWNSTREAM", e.message ?: "加载失败") }
        }
    }

    fun switchScale(scale: Int) {
        _ui.value = UiState.Loading(skeleton = false)
        scope.launch {
            val (q, k) = detailCase.execute(symbol, scale)
            _ui.value = UiState.Success(StockDetailState(quote = q, kline = k, scale = scale))
        }
    }

    /** 一键诊股。 */
    fun diagnose() {
        _insight.value = UiState.Loading()
        scope.launch {
            runCatching { diagnoseCase.execute(symbol, name) }
                .onSuccess { _insight.value = UiState.Success(it) }
                .onFailure { e -> _insight.value = UiState.Error("LLM", e.message ?: "诊断失败") }
        }
    }

    val kline: List<KLineBar> get() = (_ui.value as? UiState.Success)?.data?.kline ?: emptyList()
    val quote: Quote? get() = (_ui.value as? UiState.Success)?.data?.quote
}