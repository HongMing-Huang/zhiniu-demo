// 知牛 · 市场壳（单页 Shell：Header + 四个分区 + 个股详情覆盖层，同一窗口内切换，永不新开页面）
// 布局：背景铺满浏览器；Header 与内容共用 1320 内容宽 + 24 边距，大屏居中，小屏自适应。
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.velseif
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuikly.core.views.View
import com.zhiniu.base.BasePager
import com.zhiniu.data.mock.MockDataSource
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.*

/** 市场首屏页（也是 App 的唯一入口页：详情在壳内切换）。 */
@Page("MarketList", supportInLocal = true)
internal class MarketListPage : MarketShell()

/** 兼容旧 URL（直接打开某只股票）：同一壳内直接进入详情，仍不新开窗口。 */
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
    var aiOpen by observable(false)
    var marketTab by observable("全部")
    var filterDir by observable("全部")
    var filterGain by observable("不限")
    var filterAmount by observable("不限")
    var klineTab by observable("日K")
    var crossX by observable(-1f)
    var crossY by observable(-1f)
    var aiDraft by observable("")
    var queryText by observable("")
    var aiChat by observableList<AiChatLine>()

    var watchlist by observableList<String>()
    var recentSearch by observableList<String>()
    var marketRows by observableList<Quote>()
    var searchResults by observableList<Quote>()

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

    /** 由 OHLC 确定性生成 14 点迷你走势。 */
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
    }

    fun openFromSearch() {
        val q = queryText.trim()
        if (q.isEmpty()) return
        val hit = universe().firstOrNull { it.name.contains(q) || it.symbol.contains(q.lowercase()) } ?: return
        rememberSearch(hit.symbol)
        queryText = ""
        searchOpen = false
        openDetail(hit.symbol, section)
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

    /** 详情 K 线（按 symbol+tab 缓存，确定性）。 */
    fun klineFor(sym: String, tab: String): List<KLineBar> {
        val key = "$sym|$tab"
        klineCache[key]?.let { return it }
        val bars = when (tab) {
            "分时" -> intraday(sym)
            "周K" -> resample(sym, 52)
            "月K" -> resample(sym, 36)
            else -> mock.kline(sym)
        }
        klineCache[key] = bars
        return bars
    }

    private fun intraday(sym: String): List<KLineBar> {
        val q = quoteOf(sym) ?: return mock.kline(sym)
        val n = 48
        val seed = stableHash(sym) % 37
        var price = q.open
        val bars = ArrayList<KLineBar>(n)
        for (i in 0 until n) {
            val wave = kotlin.math.sin(i * 0.55 + seed) * q.high * 0.0018
            val open = price
            val close = open + (q.price - q.open) / (n - 1) + wave
            val hi = maxOf(open, close) + q.high * 0.0008
            val lo = minOf(open, close) - q.high * 0.0008
            val label = when {
                i == 0 -> "09:30"
                i == n - 1 -> "15:00"
                i % 8 == 0 -> "${10 + i / 8}:30"
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
    /** Header/内容共用内容宽右边缘（绝对坐标）。 */
    fun contentRightEdge(): Float {
        val vw = pageData.activityWidth
        return if (vw > CONTENT_W) (vw - CONTENT_W) / 2f + CONTENT_W - PAD else vw - PAD
    }

    fun popoverLeft(width: Float): Float = contentRightEdge() - width

    /** 屏幕可用内容宽（小于 1320 时铺满，否则 1320 居中）。 */
    fun contentWidth(): Float = minOf(pageData.activityWidth.coerceAtLeast(0f), CONTENT_W)

    // ================= 生命周期 =================
    override fun created() {
        super.created()
        ThemeState.start()
        if (watchlist.size < 2) {
            watchlist.clear()
            watchlist.add("sh600519")
            watchlist.add("sz300750")
        }
        refreshMarketRows()
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

    // ================= 过滤 =================
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

    // ================= Body =================
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(ThemeState.palette.c(ThemeState.palette.pageBg))
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
            header(this@MarketShell)
            Hdiv1()
            List {
                attr {
                    flex(1f)
                    backgroundColor(ThemeState.palette.c(ThemeState.palette.pageBg))
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
                contentInner(this@MarketShell)
                View { attr { height(1f) } }
            }
            // ---- 浮层（最后渲染，不跳页）----
            backdropLayer(this@MarketShell)
            searchPopover(this@MarketShell)
            themePopover(this@MarketShell)
            settingsPopover(this@MarketShell)
            filterPopover(this@MarketShell)
            aiDrawer(this@MarketShell)
        }
    }

    // ================= 状态切换 =================
    fun openDetail(sym: String, from: String) {
        detailFrom = from
        openSymbol = sym
        klineTab = "日K"
        crossX = -1f; crossY = -1f
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
    }
}

/** AI 抽屉内聊天行。 */
data class AiChatLine(val role: String, val text: String)

// =====================================================================
// 视图构建（顶层扩展函数 + host 显式传参；Kuikly 编译器插件限制下最稳形态）
// =====================================================================

internal fun ViewContainer<*, *>.Hdiv1() {
    View {
        attr {
            height(1f)
            backgroundColor(ThemeState.palette.c(ThemeState.palette.border))
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
    }
}

// ================= 顶部导航 =================
internal fun ViewContainer<*, *>.header(host: MarketShell) {
    val pal = { ThemeState.palette }
    View {
        attr {
            height(60f)
            flexDirectionRow()
            justifyContentCenter()
            backgroundColor(pal().c(pal().surface))
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
        View {
            attr {
                width(host.contentWidth())
                flexDirectionRow()
                alignItemsCenter()
                padding(left = PAD, right = PAD)
            }
            Text {
                attr {
                    fontSize(19f); fontWeightSemiBold()
                    color(pal().c(pal().textPrimary))
                    text("知牛")
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            View { attr { width(26f) } }
            listOf("市场", "自选", "AI研究", "排行").forEach { nav -> navItem(host, nav) }
            View { attr { flex(1f) } }
            searchEntry(host)
            IconButton(IconKind.SUN, 17f, onClick = {
                host.themeOpen = !host.themeOpen
                host.searchOpen = false; host.settingsOpen = false; host.filterOpen = false
            })
            IconButton(IconKind.SETTINGS, 17f, onClick = {
                host.settingsOpen = !host.settingsOpen
                host.searchOpen = false; host.themeOpen = false; host.filterOpen = false
            })
        }
    }
}

internal fun ViewContainer<*, *>.navItem(host: MarketShell, nav: String) {
    val pal = { ThemeState.palette }
    View {
        attr {
            padding(left = 12f, right = 12f)
            alignSelfStretch()
            cssClass("zn-nav zn-click")
            highlightBackgroundColor(pal().ca(pal().textSecondary, 8))
        }
        event { click {
            host.section = nav
            host.openSymbol = null
            host.aiOpen = false
            host.closePopovers()
        } }
        Text {
            attr {
                fontSize(14f)
                color(pal().c(if (host.section == nav) pal().textPrimary else pal().textSecondary))
                text(nav)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        View {
            attr {
                absolutePosition(top = 57f, left = 12f, right = 12f)
                height(3f)
                borderRadius(allBorderRadius = 2f)
                backgroundColor(pal().c(pal().textPrimary))
                opacity(if (host.section == nav) 1f else 0f)
                animate(Animation.easeOut(0.16f), value = host.section)
            }
        }
    }
}

internal fun ViewContainer<*, *>.searchEntry(host: MarketShell) {
    val pal = { ThemeState.palette }
    View {
        attr {
            width(288f); height(36f)
            borderRadius(8f)
            flexDirectionRow()
            alignItemsCenter()
            padding(left = 10f, right = 10f)
            backgroundColor(pal().c(pal().surfaceSecondary))
            highlightBackgroundColor(pal().ca(pal().textSecondary, 10))
            cssClass("zn-nav zn-click")
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
        event { click {
            host.searchOpen = !host.searchOpen
            host.themeOpen = false; host.settingsOpen = false; host.filterOpen = false
        } }
        Icon(IconKind.SEARCH, 15f, pal().textTertiary)
        Text {
            attr {
                marginLeft(8f)
                fontSize(13f)
                color(pal().c(pal().textTertiary))
                text("搜索股票 / 代码")
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
    View { attr { width(6f) } }
}

// ================= 内容 =================
internal fun ViewContainer<*, *>.contentInner(host: MarketShell) {
    val sidePad = 24f + maxOf(0f, (host.pageData.activityWidth - CONTENT_W) / 2f)
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

internal fun ViewContainer<*, *>.sectionTitle(title: String, sub: String, rightHint: String) {
    val pal = { ThemeState.palette }
    View { attr { marginTop(32f) } }
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(24f); fontWeightSemiBold()
                color(pal().c(pal().textPrimary))
                text(title)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(12f)
                color(pal().c(pal().textTertiary))
                text(rightHint)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
    View { attr { marginTop(4f) } }
    Text {
        attr {
            fontSize(13f)
            color(pal().c(pal().textSecondary))
            text(sub)
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
    }
}

// ================= 市场区 =================
internal fun ViewContainer<*, *>.marketSection(host: MarketShell) {
    sectionTitle("市场", "沪深 A 股行情", "今日 · Demo 数据")
    View { attr { height(16f) } }
    overviewCards(host)
    View { attr { height(24f) } }
    marketToolbar(host)
    Hdiv1()
    View { attr { height(8f) } }
    tableHeader()
    vif({ host.marketRows.isEmpty() }) { emptyTable() }
    vfor({ host.marketRows }) { q -> stockRow(host, q) }
}

internal fun ViewContainer<*, *>.overviewCards(host: MarketShell) {
    View {
        attr { flexDirectionRow(); height(166f) }
        cardShell("主要指数") { indexCard(host) }
        View { attr { width(12f) } }
        cardShell("热门股票") { hotCard(host) }
        View { attr { width(12f) } }
        cardShell("市场状态") { statusCard(host) }
    }
}

internal fun ViewContainer<*, *>.cardShell(title: String, content: ViewContainer<*, *>.() -> Unit) {
    val pal = { ThemeState.palette }
    View {
        attr {
            flex(1f)
            backgroundColor(pal().c(pal().surface))
            border(Border(1f, BorderStyle.SOLID, pal().c(pal().borderStrong)))
            borderRadius(10f)
            cssClass("zn-card")
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
        View {
            attr { padding(top = 14f, left = 16f, right = 16f) }
            CardTitleRow(title)
            View { attr { height(9f) } }
            content()
        }
    }
}

internal fun ViewContainer<*, *>.indexCard(host: MarketShell) {
    val pal = { ThemeState.palette }
    host.indicesOf().forEach { idx ->
        View {
            attr { flexDirectionRow(); alignItemsCenter(); height(31f) }
            Text {
                attr {
                    width(76f)
                    fontSize(12f)
                    color(pal().c(pal().textSecondary))
                    text(idx.name)
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            View { attr { flex(1f) } }
            Text {
                attr {
                    fontSize(14f); fontWeightSemiBold()
                    color(pal().c(pal().textPrimary))
                    text(fmt2(idx.price))
                    fontFamily(NUM_FONT)
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            Text {
                attr {
                    width(66f)
                    fontSize(13f); fontWeightSemiBold()
                    color(pal().c(if (idx.isUp) pal().up else pal().down))
                    text(fmtPct(idx.changePercent))
                    fontFamily(NUM_FONT)
                    textAlignRight()
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            sparklineView(host.sparkOf(idx), idx.isUp, 52f, 20f)
        }
    }
}

internal fun ViewContainer<*, *>.hotCard(host: MarketShell) {
    val pal = { ThemeState.palette }
    host.hotStocks().forEach { st ->
        View {
            attr {
                flexDirectionRow(); alignItemsCenter(); height(31f)
                cssClass("zn-nav zn-click")
            }
            event { click { host.openDetail(st.symbol, "市场") } }
            Text {
                attr {
                    width(112f)
                    fontSize(13f)
                    color(pal().c(pal().textPrimary))
                    text(st.name)
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            View { attr { flex(1f) } }
            Text {
                attr {
                    fontSize(14f); fontWeightSemiBold()
                    color(pal().c(pal().textPrimary))
                    text(fmt2(st.price))
                    fontFamily(NUM_FONT)
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            Text {
                attr {
                    width(66f)
                    fontSize(13f); fontWeightSemiBold()
                    color(pal().c(if (st.isUp) pal().up else pal().down))
                    text(fmtPct(st.changePercent))
                    fontFamily(NUM_FONT)
                    textAlignRight()
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
        }
    }
}

internal fun ViewContainer<*, *>.statusCard(host: MarketShell) {
    val pal = { ThemeState.palette }
    metricRow("上涨", host.upCount().toString(), pal().up)
    metricRow("下跌", host.downCount().toString(), pal().down)
    metricRow("成交额", fmtAmount(host.totalAmount()), pal().textPrimary)
    View { attr { flex(1f) } }
}

internal fun ViewContainer<*, *>.metricRow(label: String, value: String, valueHex: String) {
    val pal = { ThemeState.palette }
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(31f) }
        Text {
            attr {
                width(64f)
                fontSize(12f)
                color(pal().c(pal().textSecondary))
                text(label)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(14f); fontWeightSemiBold()
                color(pal().c(valueHex))
                text(value)
                fontFamily(NUM_FONT)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
}

// ---------- 表格工具条 ----------
internal fun ViewContainer<*, *>.marketToolbar(host: MarketShell) {
    val pal = { ThemeState.palette }
    View {
        attr { flexDirectionRow(); alignItemsFlexEnd(); height(44f) }
        val tabs = listOf("全部", "沪市", "深市", "创业板", "科创板")
        tabs.forEach { t ->
            View {
                attr {
                    width(64f); height(44f)
                    alignItemsCenter(); justifyContentCenter()
                    cssClass("zn-nav zn-click")
                }
                event { click { host.marketTab = t; host.refreshMarketRows() } }
                Text {
                    attr {
                        fontSize(14f)
                        fontWeight600()
                        color(pal().c(if (host.marketTab == t) pal().textPrimary else pal().textTertiary))
                        text(t)
                        animate(ANIM_THEME, value = ThemeState.isDark)
                    }
                }
            }
        }
        View {
            attr {
                absolutePosition(top = 42f, left = 20f)
                width(24f); height(2f)
                borderRadius(allBorderRadius = 1f)
                backgroundColor(pal().c(pal().textPrimary))
                transform(translate = Translate((tabs.indexOf(host.marketTab) * 64f / 24f), 0f))
                animate(Animation.easeOut(0.16f), value = host.marketTab)
            }
        }
        View { attr { flex(1f) } }
        toolButton("筛选", IconKind.FILTER) {
            host.filterOpen = !host.filterOpen
            host.searchOpen = false; host.themeOpen = false; host.settingsOpen = false
        }
        View { attr { width(6f) } }
        toolButton("搜索", IconKind.SEARCH) {
            host.searchOpen = !host.searchOpen
            host.filterOpen = false; host.themeOpen = false; host.settingsOpen = false
        }
    }
}

internal fun ViewContainer<*, *>.toolButton(label: String, icon: IconKind, onClick: () -> Unit) {
    val pal = { ThemeState.palette }
    View {
        attr {
            height(32f)
            borderRadius(8f)
            flexDirectionRow()
            alignItemsCenter()
            padding(left = 10f, right = 10f)
            backgroundColor(pal().c(pal().surfaceSecondary))
            highlightBackgroundColor(pal().ca(pal().textSecondary, 10))
            cssClass("zn-nav zn-click")
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
        event { click { onClick() } }
        Icon(icon, 14f, pal().textSecondary)
        Text {
            attr {
                marginLeft(6f)
                fontSize(13f)
                color(pal().c(pal().textSecondary))
                text(label)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
}

// ---------- 表格 ----------
internal fun ViewContainer<*, *>.tableHeader() {
    val pal = { ThemeState.palette }
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(30f) }
        View { attr { flex(1f) } }
        Text {
            attr {
                width(220f); fontSize(11f)
                color(pal().c(pal().textTertiary))
                text("股票")
            }
        }
        th("最新价", 150f, TextAlign.RIGHT)
        th("涨跌幅", 110f, TextAlign.RIGHT)
        th("今日走势", 130f, TextAlign.CENTER)
        th("最高 / 最低", 200f, TextAlign.RIGHT)
        th("成交额", 150f, TextAlign.RIGHT)
        View { attr { flex(1f) } }
    }
}

internal fun ViewContainer<*, *>.th(label: String, width: Float, align: TextAlign) {
    val pal = { ThemeState.palette }
    View {
        attr { width(width) }
        Text {
            attr {
                fontSize(11f)
                color(pal().c(pal().textTertiary))
                text(label)
                when (align) {
                    TextAlign.RIGHT -> textAlignRight()
                    TextAlign.CENTER -> textAlignCenter()
                    else -> textAlignLeft()
                }
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
}

internal fun ViewContainer<*, *>.stockRow(host: MarketShell, q: Quote) {
    val pal = { ThemeState.palette }
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            height(60f)
            cssClass("zn-row zn-click")
            highlightBackgroundColor(pal().ca(pal().textSecondary, 7))
        }
        event { click { host.openDetail(q.symbol, "市场") } }
        View { attr { flex(1f) } }
        View {
            attr { width(220f) }
            Text {
                attr {
                    fontSize(14f); fontWeightMedium()
                    color(pal().c(pal().textPrimary))
                    text(q.name)
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            Text {
                attr {
                    marginTop(3f)
                    fontSize(11f)
                    color(pal().c(pal().textTertiary))
                    text(fmtSymbol(q.symbol))
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
        }
        Text {
            attr {
                width(150f)
                fontSize(14f); fontWeightSemiBold()
                color(pal().c(pal().textPrimary))
                text(fmt2(q.price))
                fontFamily(NUM_FONT)
                textAlignRight()
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        Text {
            attr {
                width(110f)
                fontSize(14f); fontWeightSemiBold()
                color(pal().c(if (q.isUp) pal().up else pal().down))
                text(fmtPct(q.changePercent))
                fontFamily(NUM_FONT)
                textAlignRight()
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        View {
            attr { width(130f); allCenter() }
            sparklineView(host.sparkOf(q), q.isUp, 92f, 30f)
        }
        View {
            attr { width(200f); flexDirectionRow() }
            Text {
                attr {
                    width(88f)
                    fontSize(12f)
                    color(pal().c(pal().up))
                    text(fmt2(q.high))
                    fontFamily(NUM_FONT)
                    textAlignRight()
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            Text {
                attr {
                    width(18f)
                    fontSize(11f)
                    color(pal().c(pal().textTertiary))
                    text("/")
                    textAlignCenter()
                    lineHeight(16f)
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            Text {
                attr {
                    width(88f)
                    fontSize(12f)
                    color(pal().c(pal().down))
                    text(fmt2(q.low))
                    fontFamily(NUM_FONT)
                    textAlignLeft()
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
        }
        Text {
            attr {
                width(150f)
                fontSize(13f)
                color(pal().c(pal().textSecondary))
                text(fmtAmount(q.amount))
                fontFamily(NUM_FONT)
                textAlignRight()
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        View { attr { flex(1f) } }
        // 行底分隔线（内嵌，保证 vfor 单一孩子约束）
        View {
            attr {
                absolutePosition(top = 59f, left = 0f, right = 0f)
                height(1f)
                backgroundColor(ThemeState.palette.c(ThemeState.palette.border))
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
}

internal fun ViewContainer<*, *>.emptyTable() {
    val pal = { ThemeState.palette }
    View {
        attr { height(120f); allCenter() }
        Text {
            attr {
                fontSize(13f)
                color(pal().c(pal().textTertiary))
                text("没有符合条件的股票")
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
}

internal fun ViewContainer<*, *>.sparklineView(values: List<Double>, isUp: Boolean, w: Float, h: Float) {
    Canvas({
        attr { width(w); height(h) }
    }) { context, cw, ch ->
        val pal = { ThemeState.palette }
        drawSparkLine(context, values, pal().c(if (isUp) pal().up else pal().down), cw, ch)
    }
}

// ================= 自选 =================
internal fun ViewContainer<*, *>.watchlistSection(host: MarketShell) {
    sectionTitle(
        "自选", "关注的股票，随时查看行情",
        if (host.watchlist.isEmpty()) "" else "${host.watchlist.size} 只 · Demo 数据"
    )
    View { attr { height(16f) } }
    vif({ host.watchlist.isEmpty() }) {
        View {
            attr { height(240f); allCenter() }
            Icon(IconKind.STAR, 26f, ThemeState.palette.textTertiary)
            Text {
                attr {
                    marginTop(12f)
                    fontSize(14f)
                    color(ThemeState.palette.c(ThemeState.palette.textSecondary))
                    text("还没有自选股票")
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            Text {
                attr {
                    marginTop(6f)
                    fontSize(12f)
                    color(ThemeState.palette.c(ThemeState.palette.textTertiary))
                    text("在个股详情页点击星标即可加入自选")
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
            View { attr { height(18f) } }
            OutlineButton("去市场看看", 34f, onClick = { host.section = "市场" })
        }
    }
    velse {
        Hdiv1()
        View { attr { height(16f) } }
        tableHeader()
        vfor({ host.watchlist }) { sym ->
            host.quoteOf(sym)?.let { q -> stockRow(host, q) }
        }
    }
}

// ================= 排行 =================
internal fun ViewContainer<*, *>.rankSection(host: MarketShell) {
    sectionTitle("排行", "按今日涨跌幅排序 · 沪深 A 股", "今日 · Demo 数据")
    View { attr { height(16f) } }
    Hdiv1()
    View { attr { height(8f) } }
    tableHeader()
    host.rankRows().forEach { q -> stockRow(host, q) }
    View { attr { height(20f) } }
    Text {
        attr {
            fontSize(11f)
            color(ThemeState.palette.c(ThemeState.palette.textTertiary))
            text("排行数据为演示数据，仅用于说明产品形态。")
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
    }
}

// ================= AI 研究 =================
internal fun ViewContainer<*, *>.aiSection(host: MarketShell) {
    val pal = { ThemeState.palette }
    sectionTitle("AI 研究", "知牛多智能体 · 每日市场研判", "今日 · Demo 数据")
    View { attr { height(16f) } }
    View {
        attr {
            height(124f)
            borderRadius(10f)
            border(Border(1f, BorderStyle.SOLID, pal().c(pal().borderStrong)))
            backgroundColor(pal().c(pal().surface))
            cssClass("zn-card")
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
        View {
            attr { padding(top = 16f, left = 18f, right = 18f) }
            View {
                attr { flexDirectionRow(); alignItemsCenter() }
                Icon(IconKind.SPARKLES, 16f, pal().textPrimary)
                Text {
                    attr {
                        marginLeft(8f)
                        fontSize(15f); fontWeightSemiBold()
                        color(pal().c(pal().textPrimary))
                        text("知牛 AI · 市场观点")
                        animate(ANIM_THEME, value = ThemeState.isDark)
                    }
                }
                View { attr { flex(1f) } }
                Text {
                    attr {
                        fontSize(12f)
                        color(pal().c(pal().textTertiary))
                        text("综合情绪 62 · 偏暖")
                        animate(ANIM_THEME, value = ThemeState.isDark)
                    }
                }
            }
            View { attr { height(10f) } }
            Text {
                attr {
                    fontSize(13f)
                    lineHeight(21f)
                    color(pal().c(pal().textSecondary))
                    text("指数温和放量上行，主线集中在消费与新能源；短线情绪回暖，但需留意量能持续性。")
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
        }
    }
    View { attr { height(24f) } }
    Text {
        attr {
            fontSize(13f); fontWeightMedium()
            color(pal().c(pal().textPrimary))
            text("今日 AI 观点")
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
    }
    View { attr { height(6f) } }
    Hdiv1()
    marketInsights().forEach { ins ->
        View {
            attr {
                height(56f)
                flexDirectionRow()
                alignItemsCenter()
                cssClass("zn-row")
            }
            View {
                attr {
                    padding(left = 8f, right = 8f)
                    borderRadius(6f)
                    backgroundColor(pal().ca(ins.tagHex, 12))
                }
                Text {
                    attr {
                        fontSize(11f); fontWeightSemiBold()
                        color(pal().c(ins.tagHex))
                        text(ins.tag)
                    }
                }
            }
            Text {
                attr {
                    marginLeft(12f)
                    fontSize(13f)
                    color(pal().c(pal().textSecondary))
                    text(ins.text)
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
            }
        }
        Hdiv1()
    }
}
