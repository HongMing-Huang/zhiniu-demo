/* 知牛 · StockDetailPage（个股详情）
 * 布局：Header(64) → QuoteHeader(132~148) → Chart Workspace(560) → 底部 概览/AI解读 Tab。
 * Chart Workspace：Chart 72% + Right Rail 28%（1px border radius 6，无外层大 Card）。
 * Rail 默认：KeyData + AiQuickInsight；点击「查看完整分析」后原地替换为 AiInsightPanel。
 * K线视口：默认日K 80 根，支持左右拖动 Pan 查看历史。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
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
import com.zhiniu.domain.model.Candle
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
import com.zhiniu.pages.components.chart.autoVisibleCount
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
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtSymbol
import com.zhiniu.pages.components.fmtVolHand
import com.zhiniu.pages.components.ai.AiInsightPanel
import com.zhiniu.pages.components.ai.AiPanelChatLine
import com.zhiniu.data.local.Watchlist

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
    internal var aiDraft by observable("")
    /** 自选状态（响应式，点击后按钮即反馈）。 */
    internal var watchlisted by observable(false)
    internal val aiChat by observableList<AiPanelChatLine>()
    /** K线视口起点（用于 Pan）。 */
    internal var klineOffset by observable(0)
    internal var panStartX = 0f
    internal var panStartOffset = 0

    internal fun quote() = selectedSymbol()?.let { repo.quoteOf(it) }
    internal fun selectedSymbol() = pageData.params.optString("symbol", "").ifBlank { null }
    internal fun bars(): List<Candle> {
        val sym = selectedSymbol() ?: return emptyList()
        return repo.candles(sym, timeframeOf())
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
    internal fun viewCountFor(b: List<Candle>): Int = autoVisibleCount(b.size)

    internal fun sendFollowUp(question: String) {
        val text = question.trim()
        if (text.isEmpty()) return
        val q = quote() ?: return
        aiChat.add(AiPanelChatLine("user", text))
        aiDraft = ""
        val insight = MarketStore.aiService.insightFor(q.symbol)
        val answer = when {
            text.contains("RSI") || text.contains("rsi") -> "RSI 是相对强弱指标（0-100）。当前数值处于中性区域，未给出极端信号。"
            text.contains("量能") || text.contains("量") -> insight.volume
            text.contains("支撑") || text.contains("压力") -> "短期支撑关注 MA20 与近期低点；压力关注前期密集成交区。"
            else -> insight.trend
        }
        aiChat.add(AiPanelChatLine("ai", answer))
    }

    override fun created() {
        super.created()
        watchlisted = selectedSymbol()?.let { Watchlist.contains(it) } ?: false
        setTimeout(280) { detailLoading = false }
    }

    override fun body(): ViewBuilder = {
        attr {
            flexDirectionColumn()
            backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        renderCommonOverlays(this@StockDetailPage, "市场")
        List {
            attr {
                flex(1f)
                backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            vif({ this@StockDetailPage.detailLoading || this@StockDetailPage.quote() == null }) { detailSkeleton(this@StockDetailPage) }
            velse { detailContent(this@StockDetailPage, this@StockDetailPage.quote()!!) }
            View { attr { height(48f) } }
        }
    }
}

private fun ViewContainer<*, *>.detailSkeleton(host: StockDetailPage) {
    View {
        attr { padding(left = PAD, right = PAD) }
        View { attr { marginTop(28f) } }
        SkeletonBar(180f, 14f)
        View { attr { height(16f) } }
        SkeletonBar(260f, 22f)
        View { attr { height(20f) } }
        SkeletonBar(700f, 500f, )
    }
}

private fun ViewContainer<*, *>.detailContent(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    val pad: Float = PAD
        val aw: Float = host.pageData.activityWidth
        val extra: Float = if (aw > 1360f) (aw - 1360f) / 2f else 0f
        val sidePad: Float = pad + extra
    View {
        attr { padding(left = sidePad, right = sidePad) }
        // ---- 返回 ----
        View { attr { marginTop(20f) } }
        View {
            attr {
                height(32f); flexDirectionRow(); alignItemsCenter()
                borderRadius(6f); padding(left = 6f, right = 6f)
                cssClass("zn-click")
            }
            event { click { host.goBack() } }
            Icon(IconKind.BACK, 14f) { colors.textSecondary }
            View { attr { width(6f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs13)
                    color(colors.c(colors.textSecondary))
                    text("市场")
                }
            }
        }
        // ---- Quote Header（132~148） ----
        View { attr { height(12f) } }
        QuoteHeaderBlock(host, q)
        View { attr { height(16f) } }
        // ---- Chart Workspace（560） ----
        ChartWorkspace(host, q)
        View { attr { height(28f) } }
        // ---- 底部 Tab（仅 概览 / AI解读） ----
        View {
            attr { flexDirectionRow(); alignItemsCenter(); height(40f) }
            DetailTab("概览", host.detailTab == "概览") { host.detailTab = "概览" }
            DetailTab("AI解读", host.detailTab == "AI解读") { host.detailTab = "AI解读" }
        }
        Divider()
        View { attr { height(16f) } }
        vif({ host.detailTab == "概览" }) { overviewTab(q) }
        velse { aiTab(host, q) }
    }
}

private fun ViewContainer<*, *>.QuoteHeaderBlock(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    // 第一行：返回箭头旁无；名称代码 + 自选/AI分析
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs20); fontWeightSemiBold()
                color(colors.c(colors.textPrimary))
                text(q.name)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        Text {
            attr {
                marginLeft(12f); marginTop(4f)
                fontSize(AppTypography.fs13)
                color(colors.c(colors.textTertiary))
                text(fmtSymbol(q.symbol) + " · " + marketName(q.symbol))
            }
        }
        View { attr { flex(1f) } }
        FavoriteButton(active = { host.watchlisted }, height = 34f) {
            host.watchlisted = !host.watchlisted
            com.zhiniu.data.local.Watchlist.toggle(q.symbol)
        }
        View { attr { width(8f) } }
        // AI 分析：aiAccent 描边按钮（AI 强调仅此一处小面积点缀）
        View {
            attr {
                height(34f); padding(left = 14f, right = 14f)
                borderRadius(AppRadius.radius6)
                flexDirectionRow(); alignItemsCenter(); allCenter()
                border(
                    Border(1f, BorderStyle.SOLID, colors.c(colors.aiAccent))
                )
                backgroundColor(colors.ca(colors.aiAccent, 10))
                highlightBackgroundColor(colors.ca(colors.aiAccent, 20))
                cssClass("zn-click")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            event { click { host.isAiPanelVisible = !host.isAiPanelVisible } }
            Icon(IconKind.AI, 14f) { colors.aiAccent }
            View { attr { width(6f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs14); fontWeightSemiBold()
                    color(colors.c(colors.aiAccent))
                    text("AI 分析")
                }
            }
        }
    }
    // 第二行：大价格 + 涨跌
    View { attr { height(8f) } }
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs32); fontWeightSemiBold()
                fontFamily(NUM_FONT)
                color(colors.c(if (q.isUp) colors.up else colors.down))
                text(com.zhiniu.pages.components.fmt2(q.price))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        Text {
            attr {
                marginLeft(12f); marginTop(10f)
                fontSize(AppTypography.fs14); fontWeightMedium()
                fontFamily(NUM_FONT)
                color(colors.c(if (q.isUp) colors.up else colors.down))
                text(com.zhiniu.pages.components.fmtChangeSigned(q.change))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        Text {
            attr {
                marginLeft(6f); marginTop(10f
                )
                fontSize(AppTypography.fs14); fontWeightMedium()
                fontFamily(NUM_FONT)
                color(colors.c(if (q.isUp) colors.up else colors.down))
                text(com.zhiniu.pages.components.fmtPct(q.changePercent))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textTertiary))
                text("更新 " + q.date + " " + q.time)
            }
        }
    }
    // 第三四行：Quote Grid（max 760~860）
    View { attr { height(14f) } }
    val facts = com.zhiniu.pages.components.factsOf(q)
    View {
        attr { flexDirectionRow(); alignItemsCenter(); width(800f) }
        QuoteMetric("今开", com.zhiniu.pages.components.fmt2(q.open))
        QuoteMetric("最高", com.zhiniu.pages.components.fmt2(q.high))
        QuoteMetric("最低", com.zhiniu.pages.components.fmt2(q.low))
        QuoteMetric("昨收", com.zhiniu.pages.components.fmt2(q.prevClose))
    }
    View { attr { height(8f) } }
    View {
        attr { flexDirectionRow(); alignItemsCenter(); width(800f) }
        QuoteMetric("成交量", com.zhiniu.pages.components.fmtVolHand(q.volume))
        QuoteMetric("成交额", com.zhiniu.pages.components.fmtAmount(q.amount))
        QuoteMetric("换手率", com.zhiniu.pages.components.fmt2(facts.turnover) + "%")
        QuoteMetric("振幅", com.zhiniu.pages.components.fmtAmplitude(q.high, q.low, q.prevClose))
    }
}

private fun ViewContainer<*, *>.QuoteMetric(label: String, value: String) {
    val colors = AppTheme.colors
    View {
        attr { flex(1f) }
        Text {
            attr {
                fontSize(AppTypography.fs11)
                color(colors.c(colors.textTertiary))
                text(label)
            }
        }
        Text {
            attr {
                marginTop(2f)
                fontSize(AppTypography.fs13); fontWeightMedium()
                fontFamily(NUM_FONT)
                color(colors.c(colors.textPrimary))
                text(value)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

private fun marketName(symbol: String): String =
    if (symbol.startsWith("sh")) "沪市" else "深市"

private fun ViewContainer<*, *>.DetailTab(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(40f); padding(left = 14f, right = 14f); marginRight(20f)
            flexDirectionColumn(); alignItemsCenter(); justifyContentCenter()
            cssClass("zn-click")
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(AppTypography.fs14)
                fontWeight600()
                color(colors.c(if (active) colors.textPrimary else colors.textSecondary))
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
                opacity(if (active) 1f else 0f)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

// ---------- Chart Workspace（72/28） ----------
private fun ViewContainer<*, *>.ChartWorkspace(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    View {
        attr {
            flexDirectionRow(); alignItemsStretch()
            height(AppSpacing.chartHeight)
            backgroundColor(colors.c(colors.surface))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            borderRadius(AppRadius.radius6)
            overflow(true)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // 左：Chart 72%
        View {
            attr { flex(7.2f); flexDirectionColumn() }
            ChartToolbar(
                timeframe = { host.selectedTimeframe },
                indicator = { host.selectedIndicator },
                bars = { host.bars() },
                onTimeframe = { t ->
                    host.selectedTimeframe = t
                    host.klineOffset = 0
                    host.crossX = -1f; host.crossY = -1f
                },
                onIndicator = { host.selectedIndicator = it },
            )
            Canvas({
                attr {
                    height(AppSpacing.chartHeight - 40f)
                    backgroundColor(colors.c(colors.chartBg))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                event {
                    pan { p ->
                        val state = p.state
                        val all = host.bars()
                        val vc = autoVisibleCount(all.size)
                        if (state == "start") {
                            host.panStartX = p.x
                            host.panStartOffset = host.klineOffset
                        } else if (state == "move") {
                            val dx = p.x - host.panStartX
                            val plotW = host.pageData.activityWidth * 0.72f - 44f - 14f
                            val s = plotW / vc
                            val shift = (dx / s).toInt()
                            host.klineOffset = clampViewStart(host.panStartOffset - shift, all.size, vc)
                        }
                        host.crossX = p.x
                        host.crossY = p.y
                    }
                    click { p ->
                        host.crossX = p.x
                        host.crossY = p.y
                    }
                }
            }) { context, w, h ->
                val all = host.bars()
                val vc = host.viewCountFor(all)
                drawKLineChart(
                    context, w, h,
                    all, colors, host.indicatorOf(),
                    host.crossX, host.crossY,
                    intraday = host.selectedTimeframe == "分时",
                    viewStart = clampViewStart(host.klineOffset, all.size, vc),
                    viewCount = vc,
                )
            }
        }
        // 1px vertical divider
        View {
            attr {
                width(1f)
                backgroundColor(colors.c(colors.border))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        // 右：Rail 28%
        vif({ host.isAiPanelVisible }) {
            AiInsightPanel(
                width = host.pageData.activityWidth * 0.28f,
                quote = { host.quote() },
                insight = { host.quote()?.let { MarketStore.aiService.insightFor(it.symbol) } },
                followUpText = { host.aiDraft },
                chatLines = { host.aiChat },
                onFollowUpChange = { host.aiDraft = it },
                onFollowUpSend = { host.sendFollowUp(it) },
                onClose = { host.isAiPanelVisible = false },
            )
        }
        velse {
            RailQuickInsight(host, q)
        }
    }
}

// ---------- Rail：KeyData + AiQuickInsight ----------
private fun ViewContainer<*, *>.RailQuickInsight(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    val facts = com.zhiniu.pages.components.factsOf(q)
    val insight = MarketStore.aiService.insightFor(q.symbol)
    val railW = host.pageData.activityWidth * 0.28f
    View {
        attr {
            width(railW)
            flexDirectionColumn()
            backgroundColor(colors.c(colors.surface))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // 关键数据
        View {
            attr { padding(top = 14f, left = 16f, right = 16f) }
            SectionHeader("关键数据")
            View { attr { height(8f) } }
            RailMetric("市盈率", com.zhiniu.pages.components.fmt2(facts.pe))
            RailMetric("市净率", com.zhiniu.pages.components.fmt2(facts.pb))
            RailMetric("总市值", facts.marketCap)
            RailMetric("量比", com.zhiniu.pages.components.fmt2(facts.volumeRatio))
        }
        View { attr { height(8f) } }
        Divider()
        View { attr { height(8f) } }
        // AI 快速解读
        View {
            attr { padding(left = 16f, right = 16f); flex(1f) }
            SectionHeader("AI 快速解读")
            View { attr { height(8f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs20); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary))
                    text(insight.verdict)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(10f) } }
            RailMetric("趋势", if (insight.trend.length > 16) insight.trend.substring(0, 16) + "…" else insight.trend)
            RailMetric("量能", if (insight.volume.length > 16) insight.volume.substring(0, 16) + "…" else insight.volume)
            RailMetric("动量", insight.indicator.split("。").firstOrNull() ?: "—")
            View { attr { height(8f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textTertiary))
                    text("关注 " + insight.followUps.firstOrNull()?.take(8) ?: "MA20 支撑")
                }
            }
            View { attr { flex(1f) } }
            View {
                attr {
                    height(34f); borderRadius(6f); allCenter()
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    highlightBackgroundColor(colors.ca(colors.textSecondary, 10))
                    cssClass("zn-click")
                }
                event { click { host.isAiPanelVisible = true } }
                Icon(IconKind.AI, 13f) { colors.aiAccent }
                View { attr { width(6f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs13); fontWeightMedium()
                        color(colors.c(colors.textPrimary))
                        text("查看完整分析")
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.RailMetric(label: String, value: String) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(24f); marginBottom(2f) }
        Text {
            attr {
                width(56f)
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textTertiary))
                text(label)
            }
        }
        Text {
            attr {
                fontSize(AppTypography.fs13); fontWeightMedium()
                fontFamily(NUM_FONT)
                color(colors.c(colors.textPrimary))
                text(value)
            }
        }
    }
}

private fun ViewContainer<*, *>.overviewTab(q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    val facts = com.zhiniu.pages.components.factsOf(q)
    View {
        attr {
            backgroundColor(colors.c(colors.surface))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            borderRadius(AppRadius.radius8)
            padding(16f)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        SectionHeader("公司概览")
        Text {
            attr {
                marginTop(8f)
                fontSize(AppTypography.fs14); fontWeightMedium()
                color(colors.c(colors.textPrimary))
                text(q.name + "（" + q.code + "·" + q.marketSuffix + "）")
            }
        }
        Text {
            attr {
                marginTop(3f)
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textSecondary))
                text(if (q.symbol.startsWith("sh600519") || q.symbol.startsWith("sz000858")) "食品饮料 · 白酒" else "沪深 A 股 · 主板")
            }
        }
        View { attr { height(14f) } }
        Divider()
        View { attr { height(12f) } }
        SectionHeader("关键指标")
        View { attr { height(6f) } }
        View {
            attr { flexDirectionRow() }
            View { attr { flex(1f) }; QuoteMetric("总市值", facts.marketCap) }
            View { attr { flex(1f) }; QuoteMetric("市盈率", com.zhiniu.pages.components.fmt2(facts.pe)) }
            View { attr { flex(1f) }; QuoteMetric("市净率", com.zhiniu.pages.components.fmt2(facts.pb)) }
            View { attr { flex(1f) }; QuoteMetric("量比", com.zhiniu.pages.components.fmt2(facts.volumeRatio)) }
        }
    }
}

private fun ViewContainer<*, *>.aiTab(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    val insight = MarketStore.aiService.insightFor(q.symbol)
    View {
        attr {
            backgroundColor(colors.c(colors.surface))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            borderRadius(AppRadius.radius8)
            padding(16f)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        SectionHeader("知牛 AI · 综合判断")
        Text {
            attr {
                marginTop(8f)
                fontSize(AppTypography.fs20); fontWeightSemiBold()
                color(colors.c(colors.textPrimary))
                text(insight.verdict)
            }
        }
        View { attr { height(12f) } }
        Divider()
        View { attr { height(10f) } }
        RailMetricFull("趋势", insight.trend)
        RailMetricFull("量能", insight.volume)
        RailMetricFull("技术信号", insight.indicator)
        RailMetricFull("风险", insight.risk, risk = true)
    }
}

private fun ViewContainer<*, *>.RailMetricFull(label: String, content: String, risk: Boolean = false) {
    val colors = AppTheme.colors
    View {
        attr { marginTop(10f) }
        Text {
            attr {
                fontSize(AppTypography.fs13); fontWeightSemiBold()
                color(colors.c(if (risk) colors.down else colors.textPrimary))
                text(label)
            }
        }
        Text {
            attr {
                marginTop(4f)
                fontSize(AppTypography.fs13); lineHeight(20.15f)
                color(colors.c(colors.textSecondary))
                text(content)
            }
        }
    }
}
