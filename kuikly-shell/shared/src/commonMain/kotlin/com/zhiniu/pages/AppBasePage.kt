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
import com.zhiniu.pages.components.common.BottomTabBar
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
        val cw: Float = if (isCompact()) vw else 1360f   // 手机用满屏宽，桌面限 1360 居中
        val m = kotlin.math.min(vw, cw)
        return if (m.compareTo(0f) < 0) 0f else m
    }
    internal fun isNarrow(): Boolean {
        val vw: Float = pageData.activityWidth
        return vw <= 1280f
    }

    /**
     * 手机布局判定。Kuikly Android/iOS 的 activityWidth 传的是物理像素
     * （模拟器实测 1080/1179，logcat remeasure 可证），固定 760 阈值在手机上永远
     * 为 false、整页渲染桌面布局。改用「竖屏宽高比 + 宽度上限」判定：
     * 手机竖屏（1080x2337、1179x2556）ratio≈2.2 命中；桌面/平板横屏
     * （1440x900、1024x768）ratio<1.35 不命中；手机横屏按宽屏处理（合理）。
     */
    internal fun isCompact(): Boolean {
        val vw: Float = pageData.activityWidth
        val vh: Float = pageData.activityHeight
        if (vw <= 0f || vh <= 0f) return false
        val portrait = vh / vw > 1.35f
        return portrait && vw <= 1280f
    }
    internal fun isMedium(): Boolean = pageData.activityWidth <= 1024f
    internal fun safeTopInset(): Float = pageData.safeAreaInsets.top.coerceAtLeast(0f)
    internal fun safeBottomInset(): Float = pageData.safeAreaInsets.bottom.coerceAtLeast(0f)

    /** 页面滚动内容底部避让：手机布局加底部 Tab 高度，桌面只避安全区。 */
    internal fun bottomNavInset(): Float =
        if (isCompact()) com.zhiniu.pages.components.common.BOTTOM_TAB_HEIGHT + safeBottomInset() else safeBottomInset()
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
    // 手机布局：底部 Tab 导航（Android/iOS App 标配）；桌面不渲染
    if (host.isCompact()) {
        BottomTabBar(
            activeNav = activeNav,
            pageWidth = host.pageData.activityWidth,
            bottomInset = host.safeBottomInset(),
            onNavMarket = { host.navMarket() },
            onNavAiResearch = { host.navAiResearch() },
            onNavTodo = { host.navTodo() },
        )
    }
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
