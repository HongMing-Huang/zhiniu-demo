/* 知牛 · StockDetailPage（个股详情）
 * 布局：Header(64) → QuoteHeader(132~148) → Chart Workspace(560) → 底部 概览/AI解读 Tab。
 * Chart Workspace：Chart 72% + Right Rail 28%（1px border radius 6，无外层大 Card）。
 * Rail 默认：KeyData + AiQuickInsight；点击「查看完整分析」后原地替换为 AiInsightPanel。
 * K线视口：默认显示最新 80 根，支持拖动、滚轮与捏合缩放。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.data.mock.MarketStore
import com.zhiniu.data.remote.GatewayMarketClient
import com.zhiniu.data.remote.MarketNewsItem
import com.zhiniu.domain.model.Candle
import com.zhiniu.base.openAiResearchPage
import com.zhiniu.domain.model.StockFundamentals
import com.zhiniu.domain.repository.AiInsightFundamentals
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.domain.model.Timeframe
import com.zhiniu.pages.components.AiMessageHeader
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppSpacing
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.PAD
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.chart.ChartIndicator
import com.zhiniu.pages.components.chart.ChartToolbar
import com.zhiniu.pages.components.chart.clampViewStart
import com.zhiniu.pages.components.chart.drawKLineChart
import com.zhiniu.pages.components.common.Divider
import com.zhiniu.pages.components.common.FavoriteButton
import com.zhiniu.pages.components.common.IconButton
import com.zhiniu.pages.components.common.PrimaryButton
import com.zhiniu.pages.components.common.SecondaryButton
import com.zhiniu.pages.components.common.SkeletonBar
import com.zhiniu.pages.components.common.QuoteMetric
import com.zhiniu.pages.components.common.SectionHeader
import com.zhiniu.pages.components.factsOf
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtAmount
import com.zhiniu.pages.components.fmtChangeSigned
import com.zhiniu.pages.components.fmtOptional
import com.zhiniu.pages.components.fmtOptionalAmount
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtSymbol
import com.zhiniu.pages.components.fmtVolHand
import com.zhiniu.pages.components.ai.AiInsightPanel
import com.zhiniu.pages.components.ai.AiPanelChatLine
import com.zhiniu.pages.components.market.Level2Panel
import com.zhiniu.data.local.Watchlist
import com.tencent.kuikly.core.coroutines.launch

@Page("StockDetail", supportInLocal = true)
internal class StockDetailPage : AppBasePage() {

    // ---- 状态（命名按规范） ----
    internal var selectedTimeframe by observable("日K")
    internal var selectedIndicator by observable("MA")
    internal var detailTab by observable("概览")
    internal var crossX by observable(-1f)
    internal var crossY by observable(-1f)
    internal var detailLoading by observable(true)
    internal var isAiPanelVisible by observable(false)
    internal var railTab by observable("盘口")
    internal var aiDraft by observable("")
    /** 自选状态（响应式，点击后按钮即反馈）。 */
    internal var watchlisted by observable(false)
    internal val aiChat by observableList<AiPanelChatLine>()
    internal var liveQuote by observable<StockQuote?>(null)
    internal var liveFundamentals by observable<StockFundamentals?>(null)

    /** 网关 LLM 诊股（/agent/insight）：null = 网关不可用，UI 回退本地规则。 */
    internal var liveInsight by observable<com.zhiniu.domain.model.AiInsight?>(null)

    /** 财务/估值快照：行情快照字段优先、基本面接口补充；在 attr 内调用即随 live 数据到达刷新。 */
    internal fun facts(q: StockQuote): com.zhiniu.pages.components.StockFacts =
        com.zhiniu.pages.components.factsOf(liveQuote ?: q, liveFundamentals)
    internal var klineSource by observable("离线快照")
    internal val liveBars by observableList<Candle>()
    internal val liveNews by observableList<MarketNewsItem>()
    /** K线视口起点（用于 Pan）。 */
    internal var klineOffset by observable(Int.MAX_VALUE)
    internal var klineVisibleCount by observable(80)
    internal var panStartX = 0f
    internal var panStartOffset = 0
    internal var pinchStartCount = 80

    internal fun quote() = liveQuote ?: selectedSymbol()?.let { repo.quoteOf(it) }

    /** AI 面板洞察源：网关诊股（LLM / 服务端规则）优先，离线回退本地规则（来源如实标注）。 */
    internal fun detailInsight(): com.zhiniu.domain.model.AiInsight? =
        liveInsight ?: quote()?.let {
            MarketStore.aiService.insightFor(it.symbol, liveFundamentals.toAiFundamentals())
        }
    internal fun selectedSymbol() = pageData.params.optString("symbol", "").ifBlank { null }
    /** 当前十字线选中的 K 线（与 drawKLineChart 相同的坐标换算）；无选点返回 null。 */
    internal fun selectedBar(): Candle? {
        if (crossX < 0f) return null
        val all = bars()
        val vc = viewCountFor(all)
        if (all.isEmpty() || vc <= 0) return null
        val chartW = if (isMedium()) viewportWidth() - 76f else viewportWidth() * 0.72f
        val slot = (chartW - 58f) / vc
        if (slot <= 0f) return null
        val vs = clampViewStart(klineOffset, all.size, vc)
        val idx = (((crossX - 44f) / slot).toInt()).coerceIn(0, vc - 1)
        return all[vs + idx]
    }

    /** 选点信息条文案：已选 09-03 · 收 14.19（+1.50%）。 */
    internal fun selectedBarSummary(): String {
        val bar = selectedBar() ?: return ""
        val all = bars()
        val prev = all.getOrNull((all.indexOf(bar) - 1).coerceAtLeast(0))
        val pct = if (prev != null && prev.close != 0.0) (bar.close - prev.close) / prev.close * 100.0 else 0.0
        return "已选 " + bar.day + " · 收 " + com.zhiniu.pages.components.fmt2(bar.close) +
            "（" + com.zhiniu.pages.components.fmtPct(pct) + "）"
    }

    internal fun askSelectedBar() {
        val q = selectedBarQuestion()
        if (q.isNotBlank()) {
            com.zhiniu.base.PendingAsk.question = q
            openAiResearchPage()
        }
    }

    /** 选点追问问题（跳转 AI 研究自动发送）。 */
    internal fun selectedBarQuestion(): String {
        val bar = selectedBar() ?: return ""
        val name = quote()?.name ?: selectedSymbol() ?: ""
        return "分析 " + name + " 在 " + bar.day + " 这根K线的走势：当日收 " +
            com.zhiniu.pages.components.fmt2(bar.close) + "，结合前后走势与量价解读发生了什么"
    }

    internal fun bars(): List<Candle> {
        val sym = selectedSymbol() ?: return emptyList()
        return if (liveBars.isNotEmpty() && (selectedTimeframe == "日K" || selectedTimeframe == "分时")) liveBars
        else repo.candles(sym, timeframeOf())
    }
    private fun timeframeOf(): Timeframe = when (selectedTimeframe) {
        "分时" -> Timeframe.INTRADAY
        "周K" -> Timeframe.WEEK
        "月K" -> Timeframe.MONTH
        else -> Timeframe.DAY
    }
    internal fun indicatorOf(): ChartIndicator = when (selectedIndicator) {
        "MACD" -> ChartIndicator.MACD
        "RSI" -> ChartIndicator.RSI
        else -> ChartIndicator.MA
    }
    internal fun viewCountFor(b: List<Candle>): Int =
        if (b.isEmpty()) 0 else klineVisibleCount.coerceIn(24, b.size)

    internal fun zoomChart(deltaBars: Int) {
        val all = bars()
        if (all.isEmpty()) return
        val oldCount = viewCountFor(all)
        val nextCount = (oldCount + deltaBars).coerceIn(24, all.size)
        if (nextCount == oldCount) return
        val oldStart = clampViewStart(klineOffset, all.size, oldCount)
        val oldEnd = oldStart + oldCount
        klineVisibleCount = nextCount
        klineOffset = clampViewStart(oldEnd - nextCount, all.size, nextCount)
    }

    internal fun resetChartViewport() {
        klineVisibleCount = 80
        klineOffset = Int.MAX_VALUE
        crossX = -1f; crossY = -1f
    }

    internal fun refreshLiveBars() {
        val symbol = selectedSymbol() ?: return
        val requested = selectedTimeframe
        if (requested != "日K" && requested != "分时") {
            liveBars.diffUpdate(emptyList())
            return
        }
        val scale = if (requested == "分时") 5 else 240
        lifecycleScope.launch {
            runCatching { GatewayMarketClient.candleSeries(symbol, scale, if (scale == 5) 240 else 320) }
                .onSuccess { series ->
                    if (requested == selectedTimeframe && series.bars.size >= 24) {
                        liveBars.diffUpdate(series.bars)
                        klineSource = if (series.isStale) "${series.source.ifBlank { "离线快照" }} · 缓存"
                        else series.source.ifBlank { "行情网关" }
                    }
                }
        }
    }

    internal fun sendFollowUp(question: String) {
        val text = question.trim()
        if (text.isEmpty()) return
        val q = quote() ?: return
        aiChat.add(AiPanelChatLine("user", text))
        aiDraft = ""
        aiChat.add(AiPanelChatLine("ai", "正在调用模型网关…"))
        val pendingIndex = aiChat.size - 1
        lifecycleScope.launch {
            // 追问多为概念/指标问题：走 /agent/chat（带标的行情快照上下文，价格锚定防编造）；
            // 完整多 Agent 研究由「AI 分析」按钮触发，两者职责分离。
            val history = aiChat.takeLast(9).dropLast(1).map { (it.role to it.text) }
            val reply = runCatching {
                GatewayMarketClient.agentChat(text, history, q.symbol)
            }.getOrNull()
            val answer = reply?.content?.ifBlank { null }
                ?: "模型网关暂不可用，请稍后重试，或点击「AI 分析」查看规则生成的结构化解读。"
            val mode = when {
                reply == null -> ""
                reply.isLlm -> " · ${reply.provider}"
                else -> " · 规则降级"
            }
            if (pendingIndex < aiChat.size) aiChat[pendingIndex] = AiPanelChatLine("ai", answer + mode)
        }
    }

    override fun created() {
        super.created()
        watchlisted = selectedSymbol()?.let { Watchlist.contains(it) } ?: false
        selectedSymbol()?.let { symbol ->
            lifecycleScope.launch {
                runCatching { GatewayMarketClient.quotes(listOf(symbol)).firstOrNull() }
                    .onSuccess {
                        if (it != null) {
                            MarketStore.applyLiveQuotes(listOf(it))
                            liveQuote = it
                        }
                    }
            }
            lifecycleScope.launch {
                runCatching { GatewayMarketClient.news(symbol) }
                    .onSuccess { items -> if (items.isNotEmpty()) liveNews.diffUpdate(items) }
            }
            lifecycleScope.launch {
                runCatching { GatewayMarketClient.fundamentals(symbol) }
                    .onSuccess { if (it != null) liveFundamentals = it }
            }
            lifecycleScope.launch {
                // 服务端 LLM 诊股（缓存命中秒回；失败保持 null，面板回退本地规则并如实标注）
                runCatching { GatewayMarketClient.agentInsight(symbol) }
                    .onSuccess { if (it != null) liveInsight = it }
            }
        }
        refreshLiveBars()
        setTimeout(280) { detailLoading = false }
    }

    override fun body(): ViewBuilder = {
        attr {
            flexDirectionColumn()
            backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        renderCommonOverlays(this@StockDetailPage, "市场", showBack = true)
        List {
            attr {
                flex(1f)
                backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            vif({ this@StockDetailPage.detailLoading || this@StockDetailPage.quote() == null }) { detailSkeleton(this@StockDetailPage) }
            velse { detailContent(this@StockDetailPage, this@StockDetailPage.quote()!!) }
            View { attr { height(32f) } }
        }
        renderBottomTab(this@StockDetailPage, "市场")
    }
}
