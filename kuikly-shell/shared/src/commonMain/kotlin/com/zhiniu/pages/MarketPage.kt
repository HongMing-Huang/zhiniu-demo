// 知牛 · 市场壳（单页 Shell：GlobalHeader + 市场/自选/AI研究/排行 + 个股详情覆盖层）
// 同一窗口内切换内容，永不新开页面；所有 UI 仅使用公共组件与 Theme Token。
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.velseif
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.base.BasePager
import com.zhiniu.data.mock.MockDataSource
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.*
import com.zhiniu.pages.components.chart.ChartIndicator
import com.zhiniu.pages.components.common.*
import com.zhiniu.pages.components.market.*

/** 市场首屏页（App 唯一入口：详情在壳内切换）。 */
@Page("MarketList", supportInLocal = true)
internal class MarketListPage : MarketShell()

/** 兼容旧 URL：同一壳内直接进入详情。 */
@Page("StockDetail", supportInLocal = true)
internal class StockDetailPage : MarketShell() {
    override fun initialSymbolParam(): String? =
        pageData.params.optString("symbol", "").ifBlank { null }
}

internal abstract class MarketShell : BasePager() {

    // ================= 响应式状态 =================
    var section by observable("市场")
    var openSymbol by observable<String?>(null)
    var detailFrom by observable("市场")
    var searchOpen by observable(false)
    var themeOpen by observable(false)
    var settingsOpen by observable(false)
    var filterOpen by observable(false)
    var sortOpen by observable(false)
    var aiOpen by observable(false)
    var marketTab by observable("全部")
    var sortBy by observable("默认排序")
    var filterDir by observable("全部")
    var filterGain by observable("不限")
    var filterAmount by observable("不限")
    var timeframe by observable("日K")
    var indicator by observable(ChartIndicator.MA)
    var detailTab by observable("概览")
    var crossX by observable(-1f)
    var crossY by observable(-1f)
    var aiDraft by observable("")
    var queryText by observable("")
    var aiChat by observableList<AiChatLine>()
    var watchlist by observableList<String>()
    var recentSearch by observableList<String>()
    var marketRows by observableList<Quote>()
    var searchResults by observableList<Quote>()
    var recentRows by observableList<Quote>()
    var hotRows by observableList<Quote>()
    var marketLoading by observable(true)
    var detailLoading by observable(false)
    var aiAnalyzing by observable(false)

    // ================= 数据 =================
    protected val mock = MockDataSource()
    private val klineCache = mutableMapOf<String, List<KLineBar>>()

    fun universe(): List<Quote> = mock.stocks()
    fun indicesOf(): List<Quote> = mock.indices()
    fun quoteOf(sym: String): Quote? = mock.quoteOf(sym)
    fun watchlisted(sym: String): Boolean = watchlist.contains(sym)
    fun hotStocks(): List<Quote> =
        listOf("sh600519", "sz300750", "sz000858").mapNotNull { quoteOf(it) }
    fun upCount(): Int = universe().count { it.isUp }
    fun downCount(): Int = universe().count { !it.isUp }
    fun totalAmount(): Double = universe().sumOf { it.amount }
    fun rankRows(): List<Quote> = universe().sortedByDescending { it.changePercent }

    fun sparkOf(q: Quote): List<Double> {
        val seed = stableHash(q.symbol) % 997
        val pts = ArrayList<Double>(14)
        var v = q.open
        repeat(14) { i ->
            val wave = kotlin.math.sin(i * 1.6 + seed % 11) * q.high * 0.002
            v += (q.price - q.open) / 13 + wave
            pts.add(v)
        }
        pts[pts.size - 1] = q.price
        return pts
    }

    fun toggleWatch(sym: String) {
        if (watchlist.contains(sym)) watchlist.remove(sym) else watchlist.add(sym)
    }

    fun rememberSearch(sym: String) {
        recentSearch.remove(sym)
        recentSearch.add(0, sym)
        while (recentSearch.size > 4) recentSearch.removeAt(recentSearch.size - 1)
        recentRows.diffUpdate(recentSearch.mapNotNull { quoteOf(it) })
    }

    fun sendAiDraft() {
        val text = aiDraft.trim()
        if (text.isEmpty()) return
        aiChat.add(AiChatLine("user", text))
        val q = openSymbol?.let { quoteOf(it) }
        aiDraft = ""
        val view = q?.let { aiViewOf(it) }
        if (q != null && view != null) {
            aiChat.add(
                AiChatLine(
                    "ai",
                    "${q.name}（${fmtSymbol(q.symbol)}）综合观点「${view.verdict}」。${view.observe}${view.risk}"
                )
            )
        } else {
            aiChat.add(AiChatLine("ai", "AI 分析已开启，请先选择一只股票。"))
        }
    }

    // ================= K 线数据（按 symbol+timeframe 缓存，确定性） =================
    fun klineFor(sym: String, tf: String): List<KLineBar> {
        val key = "$sym|$tf"
        klineCache[key]?.let { return it }
        val bars = when (tf) {
            "分时" -> intraday(sym, 48)
            "5分" -> intraday(sym, 96)
            "15分" -> intraday(sym, 48)
            "30分" -> intraday(sym, 40)
            "60分" -> intraday(sym, 20)
            "周K" -> resample(sym, 52)
            "月K" -> resample(sym, 36)
            else -> mock.kline(sym)
        }
        klineCache[key] = bars
        return bars
    }

    /** 分钟级 K 线（演示：5 分钟一根，多日拼接）。 */
    private fun intraday(sym: String, n: Int): List<KLineBar> {
        val q = quoteOf(sym) ?: return mock.kline(sym)
        val seed = stableHash(sym) % 37
        var price = q.open
        val bars = ArrayList<KLineBar>(n)
        val slotsPerDay = 48
        for (i in 0 until n) {
            val wave = kotlin.math.sin(i * 0.55 + seed) * q.high * 0.0022
            val open = price
            val close = open + (q.price - q.open) / (n - 1) + wave
            val hi = maxOf(open, close) + q.high * 0.001
            val lo = minOf(open, close) - q.high * 0.001
            val slot = i % slotsPerDay
            val label = when {
                slot == 0 -> "09:30"
                slot == slotsPerDay / 2 -> "13:00"
                slot % 12 == 0 -> "${10 + slot / 12}:00"
                else -> "·"
            }
            bars.add(KLineBar(label, open, hi, lo, close, 1000L * (i + 1)))
            price = close
        }
        return bars
    }

    private fun resample(sym: String, n: Int): List<KLineBar> {
        val src = mock.kline(sym)
        if (src.size <= n) return src
        val out = ArrayList<KLineBar>(n)
        val step = src.size / n
        var i = 0
        while (i < src.size && out.size < n) {
            out.add(src[i])
            i += step
        }
        return out
    }

    // ================= 几何 =================
    fun contentRightEdge(): Float {
        val vw = pageData.activityWidth
        return if (vw > CONTENT_W) (vw - CONTENT_W) / 2f + CONTENT_W - PAD else vw - PAD
    }

    /** 内容宽（Header 与正文共用）。 */
    fun contentWidth(): Float = minOf(pageData.activityWidth.coerceAtLeast(0f), CONTENT_W)

    fun popoverLeft(width: Float): Float = contentRightEdge() - width

    /** 响应式：<1280 隐藏部分表格列。 */
    fun isNarrow(): Boolean = pageData.activityWidth < 1280f

    // ================= 过滤 / 排序 =================
    fun visibleRows(): List<Quote> {
        var rows = universe()
        rows = when (marketTab) {
            "沪市" -> rows.filter { it.symbol.startsWith("sh") }
            "深市" -> rows.filter { it.symbol.startsWith("sz") }
            "创业板" -> rows.filter { it.symbol.startsWith("sz30") }
            "科创板" -> rows.filter { it.symbol.startsWith("sh68") }
            else -> rows
        }
        rows = when (filterDir) {
            "上涨" -> rows.filter { it.isUp }
            "下跌" -> rows.filter { !it.isUp }
            else -> rows
        }
        rows = when (filterGain) {
            "≥5%" -> rows.filter { it.changePercent >= 5.0 }
            "≤-5%" -> rows.filter { it.changePercent <= -5.0 }
            else -> rows
        }
        rows = when (filterAmount) {
            "≥20亿" -> rows.filter { it.amount >= 20e8 }
            "≥50亿" -> rows.filter { it.amount >= 50e8 }
            else -> rows
        }
        rows = when (sortBy) {
            "涨幅" -> rows.sortedByDescending { it.changePercent }
            "跌幅" -> rows.sortedBy { it.changePercent }
            "成交额" -> rows.sortedByDescending { it.amount }
            "换手率" -> rows.sortedByDescending { factsOf(it).turnover }
            else -> rows
        }
        return rows
    }

    fun refreshMarketRows() {
        marketRows.diffUpdate(visibleRows())
    }

    fun refreshSearchResults() {
        val q = queryText.trim()
        searchResults.diffUpdate(
            if (q.isEmpty()) emptyList()
            else universe().filter { it.name.contains(q) || it.symbol.contains(q.lowercase()) }
        )
    }

    // ================= 生命周期 =================
    override fun created() {
        super.created()
        AppTheme.start()
        if (watchlist.size < 2) {
            watchlist.clear()
            watchlist.add("sh600519")
            watchlist.add("sz300750")
        }
        hotRows.diffUpdate(hotStocks())
        // 模拟行情加载（骨架屏 → 数据）
        marketLoading = true
        setTimeout(420) {
            marketLoading = false
            refreshMarketRows()
        }
    }

    override fun willInit() {
        super.willInit()
        initialSymbolParam()?.let { sym ->
            openSymbol = sym
            detailFrom = "市场"
            section = "市场"
        }
    }

    protected open fun initialSymbolParam(): String? = null

    // ================= Body =================
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            GlobalHeader(
                navs = listOf("市场", "自选", "AI研究", "排行"),
                activeNav = { this@MarketShell.section },
                contentWidth = this@MarketShell.contentWidth(),
                onNav = { nav ->
                    this@MarketShell.section = nav
                    this@MarketShell.openSymbol = null
                    this@MarketShell.aiOpen = false
                    this@MarketShell.closePopovers()
                },
                onSearchFocus = { this@MarketShell.searchOpen = true },
                onSearchChange = { t ->
                    this@MarketShell.queryText = t
                    this@MarketShell.refreshSearchResults()
                    this@MarketShell.searchOpen = true
                },
                onTheme = { this@MarketShell.themeOpen = !this@MarketShell.themeOpen },
                onSettings = { this@MarketShell.settingsOpen = !this@MarketShell.settingsOpen },
            )
            Divider()
            List {
                attr {
                    flex(1f)
                    backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                contentInner(this@MarketShell)
                View { attr { height(1f) } }
            }
            // ---- 浮层（不跳页）----
            Backdrop({ this@MarketShell.searchOpen || this@MarketShell.themeOpen ||
                this@MarketShell.settingsOpen || this@MarketShell.filterOpen ||
                this@MarketShell.sortOpen }) { this@MarketShell.closePopovers() }
            StockSearchOverlay(
                visible = { this@MarketShell.searchOpen },
                query = { this@MarketShell.queryText },
                recent = { this@MarketShell.recentRows },
                hot = { this@MarketShell.hotRows },
                results = { this@MarketShell.searchResults },
                left = this@MarketShell.popoverLeft(460f),
                onQueryChange = { t ->
                    this@MarketShell.queryText = t
                    this@MarketShell.refreshSearchResults()
                },
                onPick = { q ->
                    this@MarketShell.rememberSearch(q.symbol)
                    this@MarketShell.queryText = ""
                    this@MarketShell.searchOpen = false
                    this@MarketShell.openDetail(q.symbol, this@MarketShell.section)
                },
                onClose = { this@MarketShell.searchOpen = false },
            )
            themePopover(this@MarketShell)
            settingsPopover(this@MarketShell)
            filterPopover(this@MarketShell)
            sortPopover(this@MarketShell)
            aiDrawer(this@MarketShell)
        }
    }

    // ================= 状态切换 =================
    fun openDetail(sym: String, from: String) {
        detailFrom = from
        openSymbol = sym
        timeframe = "日K"
        indicator = ChartIndicator.MA
        detailTab = "概览"
        crossX = -1f; crossY = -1f
        detailLoading = true
        setTimeout(320) { detailLoading = false }
        closePopovers()
    }

    fun closeDetail() {
        openSymbol = null
        aiOpen = false
    }

    fun closePopovers() {
        searchOpen = false
        themeOpen = false
        settingsOpen = false
        filterOpen = false
        sortOpen = false
    }
}

/** AI 抽屉内聊天行。 */
data class AiChatLine(val role: String, val text: String)

// =====================================================================
// 视图构建（顶层扩展函数 + host 显式传参）
// =====================================================================

/** 内容容器：宽屏 1320 居中 + 左右 32 padding。 */
internal fun ViewContainer<*, *>.contentInner(host: MarketShell) {
    val sidePad = 32f + maxOf(0f, (host.pageData.activityWidth - CONTENT_W) / 2f)
    View {
        attr {
            padding(left = sidePad, right = sidePad)
        }
        vif({ host.openSymbol != null }) { detailSection(host) }
        velseif({ host.section == "市场" }) { marketSection(host) }
        velseif({ host.section == "自选" }) { watchlistSection(host) }
        velseif({ host.section == "AI研究" }) { aiSection(host) }
        velse { rankSection(host) }
        View { attr { height(48f) } }
    }
}

// ================= 市场区 =================
internal fun ViewContainer<*, *>.marketSection(host: MarketShell) {
    val colors = { AppTheme.colors }
    // 标题区
    View { attr { marginTop(32f) } }
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs24); fontWeightSemiBold()
                color(colors().c(colors().textPrimary))
                text("市场")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textTertiary))
                text("Demo 行情 · 14:32 更新")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
    View { attr { marginTop(6f) } }
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs13); fontWeightMedium()
                color(colors().c(colors().textPrimary))
                text("沪深 A 股")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        Text {
            attr {
                marginLeft(10f)
                fontSize(AppTypography.fs13)
                color(colors().c(colors().textSecondary))
                text("实时了解主要指数、市场热度与个股行情")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
    // 市场快照：3 卡
    View { attr { height(20f) } }
    View {
        attr { flexDirectionRow() }
        MarketOverviewCard("主要指数") { indexCard(host) }
        View { attr { width(14f) } }
        MarketOverviewCard("市场热门") { hotCard(host) }
        View { attr { width(14f) } }
        MarketOverviewCard("市场宽度") {
            MarketBreadthContent(
                upCount = 3128,
                downCount = 1932,
                upRatio = 3128.0 / (3128 + 1932),
                amountLabel = "9,864 亿",
                status = "偏强",
            )
        }
    }
    // 股票筛选
    View { attr { height(26f) } }
    marketToolbar(host)
    Divider()
    // 行情 Table
    vif({ host.marketLoading }) { TableSkeleton(7) }
    velse {
        StockTableHeader(host.isNarrow())
        vif({ host.marketRows.isEmpty() }) { emptyTable() }
        vfor({ host.marketRows }) { q ->
            StockRow(q, host.sparkOf(q), host.isNarrow()) { host.openDetail(q.symbol, "市场") }
        }
    }
}

internal fun ViewContainer<*, *>.indexCard(host: MarketShell) {
    host.indicesOf().forEach { idx -> IndexRow(idx, host.sparkOf(idx)) }
}

internal fun ViewContainer<*, *>.hotCard(host: MarketShell) {
    host.hotStocks().forEach { st -> HotStockRow(st) { host.openDetail(st.symbol, "市场") } }
}

internal fun ViewContainer<*, *>.emptyTable() {
    val colors = { AppTheme.colors }
    View {
        attr { height(120f); allCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs13)
                color(colors().c(colors().textTertiary))
                text("没有符合条件的股票")
            }
        }
    }
}

// ---------- 筛选工具条 ----------
internal fun ViewContainer<*, *>.marketToolbar(host: MarketShell) {
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        AppTabs(listOf("全部", "沪市", "深市", "创业板", "科创板"), { host.marketTab }, 64f, 44f) { t ->
            host.marketTab = t
            host.refreshMarketRows()
        }
        View { attr { flex(1f) } }
        toolbarButton(host, "排序") {
            // 排序按钮文案由下方 Text 渲染
        }
        View { attr { width(8f) } }
        toolbarButton(host, "筛选") { }
        View { attr { width(8f) } }
        toolbarButton(host, "搜索") { }
    }
}

/** 工具条按钮：底+描边+图标+文字。 */
internal fun ViewContainer<*, *>.toolbarButton(
    host: MarketShell,
    label: String,
    icon: ViewContainer<*, *>.() -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            height(32f); borderRadius(AppRadius.r8)
            flexDirectionRow(); alignItemsCenter()
            padding(left = 10f, right = 10f)
            backgroundColor(colors().c(colors().surfaceSecondary))
            border(Border(1f, BorderStyle.SOLID, colors().c(colors().border)))
            highlightBackgroundColor(colors().ca(colors().textSecondary, 10))
            cssClass("zn-nav zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click {
            when (label) {
                "筛选" -> {
                    host.filterOpen = !host.filterOpen
                    host.searchOpen = false; host.sortOpen = false; host.themeOpen = false; host.settingsOpen = false
                }
                "搜索" -> {
                    host.searchOpen = !host.searchOpen
                    host.filterOpen = false; host.sortOpen = false; host.themeOpen = false; host.settingsOpen = false
                }
                else -> {
                    host.sortOpen = !host.sortOpen
                    host.searchOpen = false; host.filterOpen = false; host.themeOpen = false; host.settingsOpen = false
                }
            }
        } }
        icon()
        if (label == "排序") {
            Text {
                attr {
                    marginLeft(4f)
                    fontSize(AppTypography.fs13)
                    color(colors().c(colors().textPrimary))
                    text(host.sortBy)
                }
            }
            Icon(IconKind.CHEVRON_DOWN, 12f, { colors().textTertiary })
        } else {
            Icon(if (label == "筛选") IconKind.FILTER else IconKind.SEARCH, 13f, { colors().textSecondary })
            Text {
                attr {
                    marginLeft(6f)
                    fontSize(AppTypography.fs13)
                    color(colors().c(colors().textSecondary))
                    text(label)
                }
            }
        }
    }
}

// ================= 自选 =================
internal fun ViewContainer<*, *>.watchlistSection(host: MarketShell) {
    val colors = { AppTheme.colors }
    View { attr { marginTop(32f) } }
    Text {
        attr {
            fontSize(AppTypography.fs24); fontWeightSemiBold()
            color(colors().c(colors().textPrimary))
            text("自选")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
    View { attr { marginTop(6f) } }
    Text {
        attr {
            fontSize(AppTypography.fs13)
            color(colors().c(colors().textSecondary))
            text(if (host.watchlist.isEmpty()) "关注的股票会出现在这里" else "关注的股票，随时查看行情")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
    View { attr { height(20f) } }
    vif({ host.watchlist.isEmpty() }) {
        EmptyState(
            IconKind.STAR, "暂无自选股票",
            "将感兴趣的股票加入自选，\n可以在这里快速查看行情。",
            "搜索股票",
        ) { host.searchOpen = true }
    }
    velse {
        Divider()
        View { attr { height(8f) } }
        StockTableHeader(host.isNarrow())
        vfor({ host.watchlist }) { sym ->
            host.quoteOf(sym)?.let { q ->
                StockRow(q, host.sparkOf(q), host.isNarrow()) { host.openDetail(q.symbol, "自选") }
            }
        }
    }
}

// ================= 排行 =================
internal fun ViewContainer<*, *>.rankSection(host: MarketShell) {
    val colors = { AppTheme.colors }
    View { attr { marginTop(32f) } }
    Text {
        attr {
            fontSize(AppTypography.fs24); fontWeightSemiBold()
            color(colors().c(colors().textPrimary))
            text("排行")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
    View { attr { marginTop(6f) } }
    Text {
        attr {
            fontSize(AppTypography.fs13)
            color(colors().c(colors().textSecondary))
            text("按今日涨跌幅排序 · 沪深 A 股")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
    View { attr { height(20f) } }
    Divider()
    View { attr { height(8f) } }
    StockTableHeader(host.isNarrow())
    host.rankRows().forEach { q ->
        StockRow(q, host.sparkOf(q), host.isNarrow()) { host.openDetail(q.symbol, "排行") }
    }
    View { attr { height(20f) } }
    Text {
        attr {
            fontSize(AppTypography.fs11)
            color(colors().c(colors().textTertiary))
            text("排行数据为演示数据，仅用于说明产品形态。")
        }
    }
}

// ================= AI 研究 =================
internal fun ViewContainer<*, *>.aiSection(host: MarketShell) {
    val colors = { AppTheme.colors }
    View { attr { marginTop(32f) } }
    Text {
        attr {
            fontSize(AppTypography.fs24); fontWeightSemiBold()
            color(colors().c(colors().textPrimary))
            text("AI 研究")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
    View { attr { marginTop(6f) } }
    Text {
        attr {
            fontSize(AppTypography.fs13)
            color(colors().c(colors().textSecondary))
            text("知牛多智能体 · 每日市场研判")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
    View { attr { height(20f) } }
    AppCard {
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            Icon(IconKind.SPARKLES, 16f, { colors().textPrimary })
            Text {
                attr {
                    marginLeft(8f)
                    fontSize(AppTypography.fs15); fontWeightSemiBold()
                    color(colors().c(colors().textPrimary))
                    text("知牛 AI · 市场观点")
                }
            }
            View { attr { flex(1f) } }
            StatusBadge("综合情绪 62 · 偏暖", { colors().up })
        }
        View { attr { height(12f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13); lineHeight(21f)
                color(colors().c(colors().textSecondary))
                text("指数温和放量上行，主线集中在消费与新能源；短线情绪回暖，但需留意量能持续性。")
            }
        }
    }
    View { attr { height(24f) } }
    SectionHeader("今日 AI 观点")
    View { attr { height(4f) } }
    Divider()
    marketInsights().forEach { ins ->
        View {
            attr {
                height(56f)
                flexDirectionRow()
                alignItemsCenter()
                cssClass("zn-row")
            }
            StatusBadge(ins.tag, { ins.tagHex })
            Text {
                attr {
                    marginLeft(12f)
                    fontSize(AppTypography.fs13)
                    color(colors().c(colors().textSecondary))
                    text(ins.text)
                }
            }
        }
        Divider()
    }
}
