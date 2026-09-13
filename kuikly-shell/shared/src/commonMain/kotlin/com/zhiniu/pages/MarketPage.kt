/* 知牛 · MarketPage（首页）：Market Pulse + 行情 Table
 * 标题区 72px → Market Pulse 92px → 28px 间距 → 工具条（自选/全部/沪市/深市/创业板/科创板/人气榜/涨幅榜 + 排序/筛选/字段/搜索）
 * → Divider → 行情表（视觉中心）。
 * 课题映射：多列表切换（人气榜=东财真实排名，涨幅榜=全市场涨幅排序）；自定义字段选择（字段芯片，含总市值/成交量）。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.velseif
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.data.remote.GatewayMarketClient
import com.zhiniu.data.remote.SectorRow
import com.zhiniu.domain.model.MarketIndex
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppSpacing
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.PAD
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.common.EmptyState
import com.zhiniu.pages.components.common.IconButton
import com.zhiniu.pages.components.common.SecondaryButton
import com.zhiniu.pages.components.common.SkeletonBar
import com.zhiniu.pages.components.market.MarketPulse
import com.zhiniu.pages.components.market.RankRow
import com.zhiniu.pages.components.market.RankTable
import com.zhiniu.pages.components.market.SectorTable
import com.zhiniu.pages.components.market.StockColumns
import com.zhiniu.pages.components.market.StockTable
import com.tencent.kuikly.core.coroutines.launch

private const val TAB_POPULAR = "人气榜"
private const val TAB_GAINERS = "涨幅榜"
private const val TAB_SECTORS = "板块"
private val MARKET_TABS = listOf("自选", "全部", "沪市", "深市", "创业板", "科创板", TAB_POPULAR, TAB_GAINERS, TAB_SECTORS)
private val MARKET_COMPACT_TABS = listOf("自选", "全部", TAB_POPULAR, TAB_GAINERS, TAB_SECTORS)

@Page("MarketList", supportInLocal = true)
internal class MarketPage : AppBasePage() {

    // 命名按规范：marketQuotes / selectedMarket / isSearchVisible ...
    internal var selectedMarket by observable("全部")
    internal var sortMode by observable("默认排序") // 默认排序 / 涨幅 / 跌幅
    internal var filterUpOnly by observable(false)
    internal var marketLoading by observable(true)
    internal val marketQuotes by observableList<StockQuote>()
    internal val marketUniverse by observableList<StockQuote>()
    internal var marketSource by observable("本地快照")
    // 自定义字段：可见列集合；tableEpoch 翻转驱动表格重建（列结构在构建期决定）
    internal var visibleColumns by observable(StockColumns.DEFAULT)
    internal var tableEpoch by observable(0)
    internal var isColumnPickerVisible by observable(false)
    // 榜单 Tab（人气榜 / 涨幅榜）
    internal val rankRows by observableList<RankRow>()
    internal var rankLoading by observable(false)
    internal var rankSource by observable("")
    // 板块 Tab（东财行业板块）
    internal val sectorRows by observableList<SectorRow>()
    internal var sectorLoading by observable(false)
    internal var sectorSource by observable("")
    // 指数条（新浪真实指数；网关不可用回退 repo 本地快照）
    internal val liveIndices by observableList<MarketIndex>()

    override fun created() {
        super.created()
        marketUniverse.diffUpdate(repo.stockQuotes())
        setTimeout(280) {
            marketLoading = false
            refreshRows()
        }
        refreshLiveQuotes()
    }

    internal fun isRankTab(): Boolean = selectedMarket == TAB_POPULAR || selectedMarket == TAB_GAINERS

    internal fun isSectorTab(): Boolean = selectedMarket == TAB_SECTORS

    internal fun refreshLiveQuotes() {
        marketSource = "同步中…"
        lifecycleScope.launch {
            runCatching { GatewayMarketClient.quotes(repo.stockQuotes().map { it.symbol }) }
                .onSuccess { live ->
                    if (live.isNotEmpty()) {
                        com.zhiniu.data.mock.MarketStore.applyLiveQuotes(live)
                        val local = repo.stockQuotes().associateBy { it.symbol }
                        marketUniverse.diffUpdate(live.map { q -> q.copy(pinyin = local[q.symbol]?.pinyin.orEmpty()) })
                        marketSource = "实时行情"
                        marketLoading = false
                        refreshRows()
                        refreshHotQuotes()
                    } else {
                        marketSource = "本地快照 · 可重试"
                    }
                }
                .onFailure {
                    println("zn-net: quotes fail url=${GatewayMarketClient.baseUrl} err=$it")
                    marketSource = "本地快照 · 可重试"
                }
        }
        if (selectedMarket == TAB_POPULAR) loadPopularity()
        if (selectedMarket == TAB_SECTORS && sectorRows.isEmpty()) loadSectors()
        loadLiveIndices()
    }

    /** 新浪三大指数实时刷新：指数条是首页视觉锚点，必须显示真实值而非 mock 快照。 */
    internal fun loadLiveIndices() {
        lifecycleScope.launch {
            runCatching { GatewayMarketClient.indices() }
                .onSuccess { if (!it.isNullOrEmpty()) liveIndices.diffUpdate(it) }
        }
    }

    internal fun refreshRows() {
        when (selectedMarket) {
            TAB_GAINERS -> {
                rankRows.diffUpdate(
                    marketUniverse.sortedByDescending { it.changePercent }.take(20)
                        .mapIndexed { i, q -> RankRow(i + 1, q.symbol, q.name, q.price, q.changePercent) }
                )
                rankSource = if (marketSource == "实时行情") "涨幅榜 · 实时行情" else "涨幅榜 · 本地快照"
                return
            }
            TAB_POPULAR -> {
                if (rankRows.isEmpty() || !rankSource.startsWith(TAB_POPULAR)) loadPopularity()
                return
            }
            TAB_SECTORS -> {
                if (sectorRows.isEmpty()) loadSectors()
                return
            }
        }
        var rows = marketUniverse.filter { it.inTab(selectedMarket) }
        if (filterUpOnly) rows = rows.filter { it.isUp }
        rows = when (sortMode) {
            "涨幅" -> rows.sortedByDescending { it.changePercent }
            "跌幅" -> rows.sortedBy { it.changePercent }
            else -> rows
        }
        marketQuotes.diffUpdate(rows)
    }

    /** 东财人气榜：真实排名 + 新浪实时增强；网关不可用时保留空态提示，不伪造榜单。 */
    internal fun loadPopularity() {
        rankLoading = true
        rankSource = "人气榜 · 同步中…"
        lifecycleScope.launch {
            val result = runCatching { GatewayMarketClient.popularity(20) }.getOrNull()
            rankLoading = false
            if (result == null || result.stocks.isEmpty()) {
                rankRows.diffUpdate(emptyList())
                rankSource = "人气榜 · 网关不可用"
                return@launch
            }
            rankRows.diffUpdate(result.stocks.map { RankRow(it.rank, it.symbol, it.name, it.price, it.changePercent) })
            rankSource = if (result.isStale) "人气榜 · 本地快照" else "人气榜 · 东方财富"
        }
    }

    /** 东财行业板块：按当日涨跌幅取前 30；网关不可用保留空态提示。 */
    internal fun loadSectors() {
        sectorLoading = true
        sectorSource = "板块 · 同步中…"
        lifecycleScope.launch {
            val result = runCatching { GatewayMarketClient.sectors() }.getOrNull()
            sectorLoading = false
            if (result == null) {
                sectorRows.diffUpdate(emptyList())
                sectorSource = "板块 · 网关不可用"
                return@launch
            }
            sectorRows.diffUpdate(
                result.sectors.sortedByDescending { it.changePercent }.take(30)
                    .mapIndexed { i, s -> s.copy(rank = i + 1) }
            )
            sectorSource = if (result.isStale) "板块 · 本地快照" else "板块 · 东方财富"
        }
    }

    internal fun toggleColumn(key: String) {
        visibleColumns = if (key in visibleColumns) visibleColumns - key else visibleColumns + key
        tableEpoch += 1
    }

    private fun StockQuote.inTab(tab: String): Boolean = when (tab) {
        "自选" -> com.zhiniu.data.local.Watchlist.contains(symbol)
        "沪市" -> symbol.startsWith("sh")
        "深市" -> symbol.startsWith("sz")
        "创业板" -> symbol.startsWith("sz30")
        "科创板" -> symbol.startsWith("sh68")
        else -> true
    }

    override fun body(): ViewBuilder = {
        attr {
            flexDirectionColumn()
            backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        renderCommonOverlays(this@MarketPage, "市场")
        List {
            attr {
                flex(1f)
                backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            marketContent(this@MarketPage)
            View { attr { height(32f) } }
        }
        renderBottomTab(this@MarketPage, "市场")
    }
}

private fun ViewContainer<*, *>.marketContent(host: MarketPage) {
    val colors = AppTheme.colors
    val pad: Float = if (host.isCompact()) 16f else PAD
        val aw: Float = host.viewportWidth()
        val extra: Float = if (aw > 1360f) (aw - 1360f) / 2f else 0f
        val sidePad: Float = pad + extra
    View {
        attr { padding(left = sidePad, right = sidePad) }
        // ---- 顶部搜索栏（手机布局：对标移动端行情 App 首页大搜索框） ----
        if (host.isCompact()) {
            View { attr { height(12f) } }
            MarketSearchBar { host.isSearchVisible = true }
        }
        // ---- 标题区（72px） ----
        View { attr { marginTop(if (host.isCompact()) 16f else 28f) } }
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            Text {
                attr {
                    fontSize(AppTypography.fs24); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary))
                    text("市场")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            if (!host.isCompact()) {
                View { attr { width(12f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs13)
                        color(colors.c(colors.textSecondary))
                        text("沪深 A 股行情")
                    }
                }
            }
            View { attr { flex(1f) } }
            // 数据源状态 chip：色点 + 短标签（实时=绿 / 快照=琥珀 / 同步中=灰），替代长拼接文本
            SourceChip(
                label = when {
                    host.isRankTab() -> host.rankSource.substringAfter(" · ")
                    host.isSectorTab() -> host.sectorSource.substringAfter(" · ")
                    host.marketSource == "实时行情" -> "实时行情"
                    host.marketSource == "同步中…" -> "同步中…"
                    else -> "本地快照"
                },
                state = when {
                    host.marketSource == "实时行情" || host.rankSource.endsWith("东方财富") || host.sectorSource.endsWith("东方财富") -> "live"
                    host.marketSource == "同步中…" || host.rankSource.contains("同步") || host.sectorSource.contains("同步") -> "syncing"
                    else -> "stale"
                },
            )
            View { attr { width(8f) } }
            IconButton(
                IconKind.REFRESH, size = 15f, box = 32f,
                accessibilityLabel = "刷新行情",
            ) { host.refreshLiveQuotes() }
        }
        // ---- Market Pulse（92px） ----
        View { attr { height(if (host.isCompact()) 16f else 28f) } }
        MarketPulse(
            indices = if (host.liveIndices.isNotEmpty()) { host.liveIndices.toList() } else host.repo.indices(),
            breadth = host.repo.breadth(),
            narrow = host.isNarrow(),
            compact = host.isCompact(),
        )
        // ---- 工具条 28px 间距 ----
        View { attr { height(if (host.isCompact()) 16f else 28f) } }
        View {
            attr { flexDirectionColumn() }
            View {
                attr { flexDirectionRow(); alignItemsCenter(); flexWrapWrap() }
                (if (host.isCompact()) MARKET_COMPACT_TABS else MARKET_TABS).forEach { t ->
                    MarketTab(t, { host.selectedMarket == t }, host.isCompact()) {
                        host.selectedMarket = t
                        host.isColumnPickerVisible = false
                        host.refreshRows()
                    }
                }
            }
            if (host.isCompact()) View { attr { height(8f) } }
            View {
                attr { flexDirectionRow(); alignItemsCenter() }
                if (!host.isCompact()) View { attr { flex(1f) } }
                // 排序 / 只看上涨 / 字段：榜单/板块 Tab 下隐藏（排名为榜单固有顺序）
                vif({ !host.isRankTab() && !host.isSectorTab() }) {
                    View {
                        attr { flexDirectionRow(); alignItemsCenter() }
                        View {
                            attr {
                                height(32f); padding(left = 10f, right = 8f)
                                borderRadius(16f); flexDirectionRow(); alignItemsCenter()
                                backgroundColor(colors.c(if (host.sortMode != "默认排序") colors.surfaceHover else colors.surface))
                                border(Border(1f, BorderStyle.SOLID, colors.c(if (host.sortMode != "默认排序") colors.borderStrong else colors.border)))
                                accessibility("排序方式：${host.sortMode}")
                                accessibilityRole(AccessibilityRole.BUTTON)
                                accessibilityInfo(clickable = true, longClickable = false)
                                cssClass("zn-click")
                                animate(ANIM_THEME, value = AppTheme.isDark)
                            }
                            event { click {
                                host.sortMode = when (host.sortMode) {
                                    "默认排序" -> "涨幅"
                                    "涨幅" -> "跌幅"
                                    else -> "默认排序"
                                }
                                host.refreshRows()
                            } }
                            Icon(IconKind.FILTER_SORT, 12f)
                            View { attr { width(4f) } }
                            Text {
                                attr {
                                    fontSize(AppTypography.fs13); lines(1)
                                    color(colors.c(if (host.sortMode != "默认排序") colors.textPrimary else colors.textSecondary))
                                    text(if (host.isCompact() && host.sortMode == "默认排序") "排序" else host.sortMode)
                                }
                            }
                            if (!host.isCompact()) {
                                Icon(if (host.sortMode == "默认排序") IconKind.CHEVRON_DOWN else IconKind.CHEVRON_UP, 10f)
                            }
                        }
                        View { attr { width(8f) } }
                        IconButton(
                            IconKind.FILTER, size = 14f, box = 32f, active = host.filterUpOnly,
                            accessibilityLabel = if (host.filterUpOnly) "取消只看上涨" else "只看上涨",
                        ) {
                            host.filterUpOnly = !host.filterUpOnly
                            host.refreshRows()
                        }
                        View { attr { width(8f) } }
                        if (!host.isCompact()) {
                            SecondaryButton("字段", height = 32f, icon = IconKind.MORE) {
                                host.isColumnPickerVisible = !host.isColumnPickerVisible
                            }
                            View { attr { width(8f) } }
                        }
                    }
                }
                if (!host.isCompact()) {
                    IconButton(IconKind.SEARCH, size = 14f, box = 32f, accessibilityLabel = "搜索股票") {
                        host.isSearchVisible = true
                    }
                    View { attr { width(8f) } }
                    SecondaryButton("刷新", height = 32f) { host.refreshLiveQuotes() }
                }
            }
            // 字段选择器：内联芯片行（不是新浮层），勾选即生效（榜单/板块 Tab 不适用）
            vif({ !host.isCompact() && host.isColumnPickerVisible && !host.isRankTab() && !host.isSectorTab() }) {
                View {
                    attr {
                        marginTop(10f); padding(top = 8f, bottom = 8f, left = 10f, right = 10f)
                        flexDirectionRow(); alignItemsCenter(); flexWrapWrap()
                        borderRadius(6f)
                        backgroundColor(colors.c(colors.surface))
                        border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                    Text {
                        attr {
                            fontSize(AppTypography.fs12); color(colors.c(colors.textTertiary))
                            marginRight(10f); text("显示字段")
                        }
                    }
                    StockColumns.ALL.forEach { key ->
                        ColumnChip(key, { key in host.visibleColumns }) { host.toggleColumn(key) }
                    }
                }
            }
        }
        View { attr { height(12f) } }
        // Divider
        View {
            attr {
                height(1f)
                backgroundColor(colors.c(colors.border))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        // ---- 行情表 / 榜单表 / 板块表 ----
        vif({ host.isRankTab() }) {
            RankTable(rows = { host.rankRows }, compact = host.isCompact()) { host.openStock(it.symbol) }
            vif({ host.rankLoading }) {
                MarketSkeleton(rows = 6)
            }
            vif({ !host.rankLoading && host.rankRows.isEmpty() }) {
                EmptyState(
                    title = "榜单暂不可用",
                    desc = "请启动本地网关后点击「刷新」重试",
                )
            }
        }
        velseif({ host.isSectorTab() }) {
            SectorTable(rows = { host.sectorRows }, compact = host.isCompact()) { row ->
                // 板块行点击 → 领涨股详情（有真实 symbol 才跳转）
                if (row.leadSymbol.isNotBlank()) host.openStock(row.leadSymbol)
            }
            vif({ host.sectorLoading }) {
                MarketSkeleton(rows = 6)
            }
            vif({ !host.sectorLoading && host.sectorRows.isEmpty() }) {
                EmptyState(
                    title = "板块数据暂不可用",
                    desc = "请启动本地网关后点击「刷新」重试",
                )
            }
        }
        velseif({ host.marketLoading }) {
            MarketSkeleton()
        }
        velse {
            // 列结构在构建期决定：epoch 奇偶切换让表格随字段选择重建
            vif({ host.tableEpoch % 2 == 0 }) { stockTable(host) }
            velse { stockTable(host) }
            vif({ host.marketQuotes.isEmpty() }) {
                EmptyState(
                    title = "没有符合条件的股票",
                    desc = "切换上方 Tab 或调整筛选条件",
                )
            }
        }
    }
}

/** 手机顶部搜索栏：大圆角胶囊（点击打开全屏搜索 Overlay，对标同花顺/雪球首页）。 */
private fun ViewContainer<*, *>.MarketSearchBar(onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(38f); borderRadius(19f)
            flexDirectionRow(); alignItemsCenter()
            paddingLeft(14f); paddingRight(14f)
            backgroundColor(colors.c(colors.surfaceSecondary))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            accessibility("搜索股票名称、代码或拼音")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        Icon(IconKind.SEARCH, 15f)
        View { attr { width(8f) } }
        Text {
            attr {
                flex(1f)
                fontSize(AppTypography.fs13)
                color(colors.c(colors.textTertiary))
                text("搜索股票名称 / 代码 / 拼音")
            }
        }
    }
}

/** 数据源状态 chip：色点 + 短标签（解决原长文本在手机上截断的问题）。 */
private fun ViewContainer<*, *>.SourceChip(label: String, state: String) {
    val colors = AppTheme.colors
    val dotColor = when (state) {
        "live" -> colors.down      // 绿 = 实时（跌色系=绿，A股语义绿=健康在线）
        "syncing" -> colors.textTertiary
        else -> colors.ma5         // 琥珀 = 本地快照
    }
    View {
        attr {
            height(24f); paddingLeft(8f); paddingRight(9f)
            borderRadius(12f); flexDirectionRow(); alignItemsCenter()
            backgroundColor(colors.c(colors.surfaceSecondary))
            accessibility("数据源：$label")
            accessibilityRole(AccessibilityRole.TEXT)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr {
                width(6f); height(6f); borderRadius(3f)
                backgroundColor(colors.c(dotColor))
            }
        }
        View { attr { width(5f) } }
        Text {
            attr {
                fontSize(AppTypography.fs11); lines(1)
                color(colors.c(colors.textSecondary)); text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

private fun ViewContainer<*, *>.stockTable(host: MarketPage) {
    StockTable(
        marketQuotes = { host.marketQuotes },
        sparkOf = { host.repo.spark(it.symbol) },
        narrow = host.isNarrow(),
        compact = host.isCompact(),
        columns = host.visibleColumns,
        onRowClick = { host.openStock(it.symbol) },
    )
}

private fun ViewContainer<*, *>.MarketTab(label: String, active: () -> Boolean, compact: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    // 手机布局：胶囊选中态（欧易式），占位小、触控友好；桌面保留下划线
    if (compact) {
        View {
            attr {
                height(32f); padding(left = 12f, right = 12f); marginRight(8f)
                borderRadius(16f)
                backgroundColor(colors.c(if (active()) colors.surfaceHover else colors.surface))
                animate(ANIM_THEME, value = AppTheme.isDark)
                accessibility(label)
                accessibilityRole(AccessibilityRole.BUTTON)
                accessibilityInfo(clickable = true, longClickable = false)
                cssClass("zn-click")
                highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
            }
            event { click { onClick() } }
            Text {
                attr {
                    fontSize(AppTypography.fs13)
                    color(colors.c(if (active()) colors.textPrimary else colors.textSecondary))
                    fontWeight600(); lines(1)
                    text(label)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        return
    }
    View {
        attr {
            height(34f); padding(left = 4f, right = 4f); marginRight(20f)
            flexDirectionColumn(); alignItemsCenter(); justifyContentCenter()
            accessibility(label)
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(AppTypography.fs14)
                color(colors.c(if (active()) colors.textPrimary else colors.textSecondary))
                fontWeight600(); lines(1)
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(4f) } }
        View {
            attr {
                height(2f); width(16f)
                borderRadius(allBorderRadius = 1f)
                backgroundColor(colors.c(colors.textPrimary))
                opacity(if (active()) 1f else 0f)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

/** 字段芯片：26px 高，选中为深色实底 + 勾图标，未选为描边。 */
private fun ViewContainer<*, *>.ColumnChip(label: String, active: () -> Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(26f); padding(left = 10f, right = 10f); marginRight(8f); marginTop(2f); marginBottom(2f)
            borderRadius(13f); flexDirectionRow(); alignItemsCenter()
            backgroundColor(colors.c(if (active()) colors.textPrimary else colors.surfaceSecondary))
            border(Border(1f, BorderStyle.SOLID, colors.c(if (active()) colors.textPrimary else colors.border)))
            accessibility(if (active()) "隐藏列 $label" else "显示列 $label")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(AppTypography.fs12); lines(1)
                color(colors.c(if (active()) colors.surface else colors.textSecondary))
                text(label)
            }
        }
    }
}

private fun ViewContainer<*, *>.MarketSkeleton(rows: Int = 8) {
    val colors = AppTheme.colors
    repeat(rows) {
        View {
            attr { flexDirectionRow(); alignItemsCenter(); height(64f) }
            View { attr { flex(2.4f) }; SkeletonBar(140f, 12f) }
            View { attr { width(120f) }; SkeletonBar(80f, 12f) }
            View { attr { width(110f) }; SkeletonBar(70f, 12f) }
            View { attr { width(110f) }; SkeletonBar(70f, 12f) }
            View { attr { width(170f) }; SkeletonBar(80f, 12f) }
            View { attr { width(150f) }; SkeletonBar(80f, 12f) }
            View { attr { width(110f) }; SkeletonBar(80f, 12f) }
        }
        View {
            attr { height(1f); backgroundColor(colors.c(colors.border)) }
        }
    }
}
