/* 知牛 · MarketListVM */
package com.zhiniu.viewmodel

import com.zhiniu.domain.repository.MarketRepository
import com.zhiniu.domain.usecase.GetStockList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MarketListVM(
    private val scope: CoroutineScope,
    repo: MarketRepository,
) {
    private val getStockList = GetStockList(repo)
    private val _ui = MutableStateFlow<UiState<MarketListState>>(UiState.Idle)
    val ui: StateFlow<UiState<MarketListState>> = _ui.asStateFlow()

    private val _mode = MutableStateFlow(MarketMode.ALL)
    val mode: StateFlow<MarketMode> = _mode.asStateFlow()

    init { refresh() }

    fun setMode(m: MarketMode) { _mode.value = m; refresh() }

    fun refresh() {
        _ui.value = UiState.Loading()
        scope.launch {
            runCatching {
                val mode = _mode.value
                val indices = getStockList.indices()
                val quotes = when (mode) {
                    MarketMode.ALL -> getStockList.all()
                    MarketMode.WATCHLIST -> getStockList.watchlist()
                    MarketMode.GAINERS -> getStockList.all().sortedByDescending { it.changePercent }
                    MarketMode.LOSERS -> getStockList.all().sortedBy { it.changePercent }
                }
                MarketListState(indices, quotes, mode)
            }.onSuccess { _ui.value = UiState.Success(it) }
                .onFailure { e -> _ui.value = UiState.Error("DOWNSTREAM", e.message ?: "加载失败") }
        }
    }
}