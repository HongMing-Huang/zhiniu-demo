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
import com.zhiniu.base.openComparePage
import com.zhiniu.base.openMarketPage
import com.zhiniu.base.openProfilePage
import com.zhiniu.base.openStockDetail
import com.zhiniu.base.openWatchlistPage
import com.zhiniu.data.mock.MarketStore
import com.zhiniu.data.remote.GatewayMarketClient
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.domain.repository.MarketRepository
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.common.AppHeader
import com.zhiniu.pages.components.common.BottomTabBar
import com.zhiniu.pages.components.common.StockSearchOverlay
import com.tencent.kuikly.core.coroutines.launch

/** 页面公共基类（所有一级页面继承）。 */
internal abstract class AppBasePage : BasePager() {

    internal val repo: MarketRepository = MarketStore.repository

    internal var isSearchVisible by observable(false)
    internal var searchQuery by observable("")
    internal val searchRecent by observableList<StockQuote>()
    internal val searchHot by observableList<StockQuote>()
    internal val searchResults by observableList<StockQuote>()
    internal var gatewayOnline by observable(false)
    internal var agentReady by observable(false)

    /**
     * 响应式宽度统一用官方 root view 尺寸（逻辑单位：Android=dp / iOS=pt / ohos=vp / H5=CSS px，
     * 已实测各端桥一致传递）。H5 的 root view 由宿主画布决定，浏览器响应式宽度改用 activity。
     */
    internal fun viewportWidth(): Float =
        if (pageData.isWeb) pageData.activityWidth else pageData.pageViewWidth

    internal fun viewportHeight(): Float =
        if (pageData.isWeb) pageData.activityHeight else pageData.pageViewHeight

    /** 内容宽（全部显式 Float，避开 Comparable 重载歧义）。 */
    internal fun contentWidth(): Float {
        val vw: Float = viewportWidth()
        val cw: Float = if (isCompact()) vw else 1360f   // 手机用满屏宽，桌面限 1360 居中
        val m = kotlin.math.min(vw, cw)
        return if (m.compareTo(0f) < 0) 0f else m
    }
    internal fun isNarrow(): Boolean {
        val vw: Float = viewportWidth()
        return vw <= 1280f
    }

    /**
     * 响应式布局基于 Kuikly 官方 root view 尺寸（逻辑单位，三端桥一致，见 viewportWidth 注释）。
     */
    internal fun isCompact(): Boolean {
        val vw: Float = viewportWidth()
        val vh: Float = viewportHeight()
        if (vw <= 0f || vh <= 0f) return false
        return vw <= 760f
    }
    internal fun isMedium(): Boolean = viewportWidth() <= 1024f
    internal fun safeTopInset(): Float = pageData.safeAreaInsets.top.coerceAtLeast(0f)
    internal fun safeBottomInset(): Float = pageData.safeAreaInsets.bottom.coerceAtLeast(0f)
    internal fun searchOverlayWidth(): Float = if (isCompact()) (viewportWidth() - 32f).coerceAtLeast(280f) else 520f
    internal fun overlayLeft(width: Float): Float {
        val vw: Float = viewportWidth()
        val cw: Float = kotlin.math.min(vw, 1360f)
        val pad: Float = if (vw <= 760f) 16f else 32f
        val contentLeft: Float = (vw - cw) / 2f
        return (contentLeft + cw - pad - width).coerceAtLeast(pad)
    }

    private var searchSeq = 0

    /** 搜索 = 本地快照即时过滤 + 网关全市场搜索（东财 suggest）异步补全；序号防抖避免乱序覆盖。 */
    internal fun refreshSearchResults() {
        searchResults.diffUpdate(repo.search(searchQuery))
        val kw = searchQuery.trim()
        if (kw.length < 2) return
        val seq = ++searchSeq
        lifecycleScope.launch {
            val suggestions = runCatching { GatewayMarketClient.search(kw) }.getOrNull() ?: return@launch
            if (seq != searchSeq || suggestions.isEmpty()) return@launch
            val known = searchResults.map { it.symbol }.toSet()
            val fresh = suggestions.filter { it.symbol !in known }.take(10 - known.size)
            if (fresh.isEmpty()) return@launch
            // 建议命中 → 批量拉实时报价填充价格（拿不到价格的跳过，不显示 0.00 假价格）
            val quotes = runCatching { GatewayMarketClient.quotes(fresh.map { it.symbol }) }.getOrNull()
                ?.associateBy { it.symbol } ?: emptyMap()
            val rows = fresh.mapNotNull { s ->
                quotes[s.symbol]?.let { it.copy(name = s.name) }
                    ?: repo.quoteOf(s.symbol)?.copy(name = s.name)
            }
            if (rows.isNotEmpty() && seq == searchSeq) searchResults.diffUpdate(searchResults.toList() + rows)
        }
    }
    internal fun rememberSearch(symbol: String) {
        val idx = searchRecent.indexOfFirst { it.symbol == symbol }
        if (idx >= 0) searchRecent.removeAt(idx)
        repo.quoteOf(symbol)?.let { searchRecent.add(0, it) }
        while (searchRecent.size > 6) searchRecent.removeAt(searchRecent.size - 1)
    }
    internal fun initSearch() {
        if (searchHot.isEmpty()) repo.stockQuotes().take(5).forEach { searchHot.add(it) }
    }

    /** 实时行情到达后刷新热门列表价格（避免搜索热门展示过期 mock 价）。 */
    internal fun refreshHotQuotes() {
        searchHot.diffUpdate(repo.stockQuotes().take(5))
    }

    internal fun navMarket() = openMarketPage()
    internal fun navAiResearch() = openAiResearchPage()
    internal fun navWatchlist() = openWatchlistPage()
    internal fun navProfile() = openProfilePage()
    internal fun openStock(symbol: String) = openStockDetail(symbol)
    internal fun goBack() = closeCurrentPage()

    override fun created() {
        super.created(); AppTheme.start(); initSearch()
        // 外观持久化钩子：任意页面切换外观（我的页三段式/快捷切换、AI 指令 set_appearance）统一落盘
        AppTheme.persistHook = { mode ->
            acquireModule<com.tencent.kuikly.core.module.SharedPreferencesModule>(
                com.tencent.kuikly.core.module.SharedPreferencesModule.MODULE_NAME,
            ).setString(com.zhiniu.pages.components.THEME_MODE_SP_KEY, mode.name)
        }
        // 网关地址优先级：用户在「我的」页保存的地址 > pageData 联调参数 > 平台默认
        // （真机 10.0.2.2 无效，必须用电脑局域网 IP——无用户设置时靠联调参数兜底）
        acquireModule<com.tencent.kuikly.core.module.SharedPreferencesModule>(
            com.tencent.kuikly.core.module.SharedPreferencesModule.MODULE_NAME,
        ).getString(com.zhiniu.data.remote.GatewayMarketClient.GATEWAY_SP_KEY)
            .takeIf { it.isNotBlank() }
            ?.let { GatewayMarketClient.baseUrl = it }
        // 真机/局域网联调：?gateway=http://192.168.x.x:8000 覆盖默认本机网关（非法值忽略）
        pageData.params.optString("gateway", "").takeIf { it.isNotBlank() }?.let { GatewayMarketClient.baseUrl = it }
        lifecycleScope.launch {
            runCatching { GatewayMarketClient.health() }
                .onSuccess { status ->
                    gatewayOnline = status.online
                    agentReady = status.agentReady
                }
                .onFailure { println("zn-net: health fail url=${GatewayMarketClient.baseUrl} err=$it") }
        }
    }
}

/** 手机布局底部 Tab（页面 body 末尾调用；流式布局，ohos 渲染器兼容）。 */
internal fun ViewContainer<*, *>.renderBottomTab(host: AppBasePage, activeNav: String) {
    if (!host.isCompact()) return
    BottomTabBar(
        activeNav = activeNav,
        bottomInset = host.safeBottomInset(),
        onNavMarket = { host.navMarket() },
        onNavWatchlist = { host.navWatchlist() },
        onNavAiResearch = { host.navAiResearch() },
        onNavProfile = { host.navProfile() },
    )
}

/** 公共浮层渲染（顶级扩展，body lambda 可隐式 ViewContainer receiver 调用）。 */
internal fun ViewContainer<*, *>.renderCommonOverlays(host: AppBasePage, activeNav: String, showBack: Boolean = false) {
    AppHeader(
        activeNav = activeNav,
        contentWidth = host.contentWidth(),
        topInset = host.safeTopInset(),
        onNavMarket = { host.navMarket() },
        onNavAiResearch = { host.navAiResearch() },
        onNavWatchlist = { host.navWatchlist() },
        onNavProfile = { host.navProfile() },
        onSearch = { host.isSearchVisible = true },
        onBack = if (showBack) { { host.goBack() } } else null,
    )
    // 手机布局的底部 Tab 由各页面在 body 末尾经 renderBottomTab 渲染（流式布局，ohos 兼容）
    StockSearchOverlay(
        visible = { host.isSearchVisible },
        query = { host.searchQuery },
        recent = { host.searchRecent },
        hot = { host.searchHot },
        results = { host.searchResults },
        left = host.overlayLeft(host.searchOverlayWidth()),
        width = host.searchOverlayWidth(),
        topInset = host.safeTopInset(),
        compact = host.isCompact(),
        onQueryChange = { t -> host.searchQuery = t; host.refreshSearchResults() },
        onPick = { q ->
            host.rememberSearch(q.symbol); host.searchQuery = ""; host.isSearchVisible = false
            host.openStock(q.symbol)
        },
        onClose = { host.isSearchVisible = false },
    )
}
