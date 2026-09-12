/* 知牛 · 页面公共基类：Header / 搜索浮层 / 主题浮层 / 响应式几何
 * 响应式状态与创建它的 Pager 绑定（跨页不同步）。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.zhiniu.base.BasePager
import com.zhiniu.base.closeCurrentPage
import com.zhiniu.base.openAiResearchPage
import com.zhiniu.base.openMarketPage
import com.zhiniu.base.openStockDetail
import com.zhiniu.base.openTodoPage
import com.zhiniu.data.mock.MarketStore
import com.zhiniu.data.remote.GatewayMarketClient
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.domain.repository.MarketRepository
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.common.AppHeader
import com.zhiniu.pages.components.common.StockSearchOverlay
import com.zhiniu.pages.components.common.ThemePopover
import com.tencent.kuikly.core.coroutines.launch

/** 页面公共基类（所有一级页面继承）。 */
internal abstract class AppBasePage : BasePager() {

    internal val repo: MarketRepository = MarketStore.repository

    internal var isSearchVisible by observable(false)
    internal var searchQuery by observable("")
    internal val searchRecent by observableList<StockQuote>()
    internal val searchHot by observableList<StockQuote>()
    internal val searchResults by observableList<StockQuote>()
    internal var isThemePopoverVisible by observable(false)
    internal var gatewayOnline by observable(false)
    internal var agentReady by observable(false)

    /** 内容宽（全部显式 Float，避开 Comparable 重载歧义）。 */
    internal fun contentWidth(): Float {
        val vw: Float = pageData.activityWidth
        val cw: Float = 1360f
        val m = kotlin.math.min(vw, cw)
        return if (m.compareTo(0f) < 0) 0f else m
    }
    internal fun isNarrow(): Boolean {
        val vw: Float = pageData.activityWidth
        return vw <= 1280f
    }
    /** 760px 以下切换为单列/精简布局，覆盖常见手机横竖屏与窄窗口。 */
    internal fun isCompact(): Boolean = pageData.activityWidth <= 760f
    internal fun isMedium(): Boolean = pageData.activityWidth <= 1024f
    internal fun safeTopInset(): Float = pageData.safeAreaInsets.top.coerceAtLeast(0f)
    internal fun safeBottomInset(): Float = pageData.safeAreaInsets.bottom.coerceAtLeast(0f)
    internal fun searchOverlayWidth(): Float = if (isCompact()) (pageData.activityWidth - 32f).coerceAtLeast(280f) else 520f
    internal fun settingsOverlayWidth(): Float = if (isCompact()) (pageData.activityWidth - 32f).coerceAtLeast(280f) else 304f
    internal fun overlayLeft(width: Float): Float {
        val vw: Float = pageData.activityWidth
        val cw: Float = kotlin.math.min(vw, 1360f)
        val pad: Float = if (vw <= 760f) 16f else 32f
        val contentLeft: Float = (vw - cw) / 2f
        return (contentLeft + cw - pad - width).coerceAtLeast(pad)
    }

    internal fun refreshSearchResults() { searchResults.diffUpdate(repo.search(searchQuery)) }
    internal fun rememberSearch(symbol: String) {
        val idx = searchRecent.indexOfFirst { it.symbol == symbol }
        if (idx >= 0) searchRecent.removeAt(idx)
        repo.quoteOf(symbol)?.let { searchRecent.add(0, it) }
        while (searchRecent.size > 6) searchRecent.removeAt(searchRecent.size - 1)
    }
    internal fun initSearch() {
        if (searchHot.isEmpty()) repo.stockQuotes().take(5).forEach { searchHot.add(it) }
    }

    internal fun navMarket() = openMarketPage()
    internal fun navAiResearch() = openAiResearchPage()
    internal fun navTodo() = openTodoPage()
    internal fun openStock(symbol: String) = openStockDetail(symbol)
    internal fun goBack() = closeCurrentPage()

    override fun created() {
        super.created(); AppTheme.start(); initSearch()
        // 真机/局域网联调：?gateway=http://192.168.x.x:8000 覆盖默认本机网关（非法值忽略）
        pageData.params.optString("gateway", "").takeIf { it.isNotBlank() }?.let { GatewayMarketClient.baseUrl = it }
        lifecycleScope.launch {
            runCatching { GatewayMarketClient.health() }.onSuccess { status ->
                gatewayOnline = status.online
                agentReady = status.agentReady
            }
        }
    }
}

/** 公共浮层渲染（顶级扩展，body lambda 可隐式 ViewContainer receiver 调用）。 */
internal fun ViewContainer<*, *>.renderCommonOverlays(host: AppBasePage, activeNav: String) {
    AppHeader(
        activeNav = activeNav,
        contentWidth = host.contentWidth(),
        topInset = host.safeTopInset(),
        onNavMarket = { host.navMarket() },
        onNavAiResearch = { host.navAiResearch() },
        onNavTodo = { host.navTodo() },
        onSearch = { host.isSearchVisible = true },
        onTheme = { host.isThemePopoverVisible = !host.isThemePopoverVisible },
    )
    StockSearchOverlay(
        visible = { host.isSearchVisible },
        query = { host.searchQuery },
        recent = { host.searchRecent },
        hot = { host.searchHot },
        results = { host.searchResults },
        left = host.overlayLeft(host.searchOverlayWidth()),
        width = host.searchOverlayWidth(),
        topInset = host.safeTopInset(),
        onQueryChange = { t -> host.searchQuery = t; host.refreshSearchResults() },
        onPick = { q ->
            host.rememberSearch(q.symbol); host.searchQuery = ""; host.isSearchVisible = false
            host.openStock(q.symbol)
        },
        onClose = { host.isSearchVisible = false },
    )
    ThemePopover(
        visible = { host.isThemePopoverVisible },
        left = host.overlayLeft(host.settingsOverlayWidth()),
        width = host.settingsOverlayWidth(),
        topInset = host.safeTopInset(),
        gatewayOnline = { host.gatewayOnline },
        agentReady = { host.agentReady },
        onClose = { host.isThemePopoverVisible = false },
    )
}
