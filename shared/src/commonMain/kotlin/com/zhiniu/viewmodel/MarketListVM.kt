/* 知牛 · MarketListVM（Kuikly 原生响应式）
 * 按 kuiklyDSL.mdc：用 observable/observableList 驱动 UI，attr 内配合 vfor/vif/vbind。
 * 纯计算（接口/仓储/UseCase）仍为普通 suspend；仅"UI 状态"进入 Kuikly 可观测域。
 * ⚠️ observable/observableList 的具体包路径以 Kuikly SDK 官方模板为准；此处标注用法。
 */
package com.zhiniu.viewmodel

import com.tencent.kuikly.ref.observable.observable
import com.tencent.kuikly.ref.observable.observableList
import com.zhiniu.domain.model.Quote
import com.zhiniu.domain.repository.MarketRepository
import com.zhiniu.domain.usecase.GetStockList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MarketListVM(
    private val scope: CoroutineScope,
    repo: MarketRepository,
) {
    private val getList = GetStockList(repo)

    // Kuikly 可观测：页面状态 + 列表数据
    val indices = observableList<Quote>()
    val quotes = observableList<Quote>()
    val loading = observable(true)
    val errorMsg = observable<String?>(null)
    val mode = observable(MarketMode.ALL)

    init { refresh() }

    fun setMode(m: MarketMode) { mode.value = m; refresh() }

    fun refresh() {
        loading.value = true
        errorMsg.value = null
        scope.launch {
            runCatching {
                val m = mode.value
                indices.clear(); indices.addAll(getList.indices())
                quotes.clear()
                when (m) {
                    MarketMode.ALL -> quotes.addAll(getList.all())
                    MarketMode.WATCHLIST -> quotes.addAll(getList.watchlist())
                    MarketMode.GAINERS -> quotes.addAll(getList.all().sortedByDescending { it.changePercent })
                    MarketMode.LOSERS -> quotes.addAll(getList.all().sortedBy { it.changePercent })
                }
            }.onSuccess {
                loading.value = false
            }.onFailure { e ->
                loading.value = false
                errorMsg.value = e.message ?: "加载失败"
            }
        }
    }

    fun retry() = refresh()
}