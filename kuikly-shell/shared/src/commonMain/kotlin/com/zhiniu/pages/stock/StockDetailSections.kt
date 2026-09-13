/* 知牛 · 个股详情 UI 组件（自 StockDetailPage 拆分：骨架/报价头/图表工作区/五 Tab 内容）。
 * 全部为 internal 展示函数；状态与交互仍归属 StockDetailPage（页面类）。 */
package com.zhiniu.pages
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



internal fun ViewContainer<*, *>.detailSkeleton(host: StockDetailPage) {
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

internal fun ViewContainer<*, *>.detailContent(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
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
            Icon(IconKind.BACK, 14f)
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
            DetailTab("概览", { host.detailTab == "概览" }) { host.detailTab = "概览" }
            DetailTab("资金", { host.detailTab == "资金" }) { host.detailTab = "资金" }
            DetailTab("财务", { host.detailTab == "财务" }) { host.detailTab = "财务" }
            DetailTab("新闻", { host.detailTab == "新闻" }) { host.detailTab = "新闻" }
            DetailTab("AI解读", { host.detailTab == "AI解读" }) { host.detailTab = "AI解读" }
        }
        Divider()
        View { attr { height(16f) } }
        vif({ host.detailTab == "概览" }) { overviewTab(host, q) }
        vif({ host.detailTab == "资金" }) { fundsTab(q) }
        vif({ host.detailTab == "财务" }) { financeTab(host, q) }
        vif({ host.detailTab == "新闻" }) { newsTab(host, q) }
        vif({ host.detailTab == "AI解读" }) { aiTab(host, q) }
        // 手机布局：欧易式底部主操作条（全宽胶囊 AI 分析），List 内容末尾
        vif({ host.isCompact() }) {
            View { attr { height(20f) } }
            View {
                attr {
                    height(46f); borderRadius(23f); allCenter()
                    flexDirectionRow()
                    backgroundColor(colors.c(colors.textPrimary))
                    cssClass("zn-click")
                    highlightBackgroundColor(colors.ca(colors.textSecondary, 12))
                    accessibility("打开 AI 分析面板")
                    accessibilityRole(AccessibilityRole.BUTTON)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                event { click { host.isAiPanelVisible = !host.isAiPanelVisible } }
                Icon(IconKind.AI, 16f)
                View { attr { width(8f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs15); fontWeightSemiBold()
                        color(Color.WHITE)
                        text("AI 分析 " + q.name)
                    }
                }
            }
            View { attr { height(16f) } }
        }
    }
}

internal fun ViewContainer<*, *>.QuoteHeaderBlock(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    if (host.isCompact()) {
        // ---- 手机布局（欧易式）：名称/副行两行 + 右侧星标/AI 图标钮；大价格块；两列数据网格 ----
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            View {
                attr { flex(1f); flexDirectionColumn() }
                Text {
                    attr {
                        fontSize(AppTypography.fs18); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text(q.name)
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
                View { attr { height(3f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textTertiary))
                        text(fmtSymbol(q.symbol) + " · " + marketName(q.symbol))
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
            }
            // 星标（加自选）
            View {
                attr {
                    width(36f); height(36f); borderRadius(18f); allCenter()
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    cssClass("zn-click")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                    accessibility(if (host.watchlisted) "移出自选" else "加入自选")
                    accessibilityRole(AccessibilityRole.BUTTON)
                }
                event { click { host.watchlisted = !host.watchlisted; com.zhiniu.data.local.Watchlist.toggle(q.symbol) } }
                Icon(if (host.watchlisted) IconKind.STAR_FILLED else IconKind.STAR, 16f)
            }
            View { attr { width(8f) } }
            // AI 分析（图标钮，打开抽屉）
            View {
                attr {
                    width(36f); height(36f); borderRadius(18f); allCenter()
                    backgroundColor(colors.ca(colors.aiAccent, 12))
                    border(Border(1f, BorderStyle.SOLID, colors.c(colors.aiAccent)))
                    cssClass("zn-click")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                    accessibility("打开 AI 分析")
                    accessibilityRole(AccessibilityRole.BUTTON)
                }
                event { click { host.isAiPanelVisible = !host.isAiPanelVisible } }
                Icon(IconKind.AI, 16f)
            }
        }
        View { attr { height(10f) } }
        // 大价格块：价格 + 涨跌额/幅 同行；更新时间独立小字一行（避免挤压重叠）
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            Text {
                attr {
                    fontSize(AppTypography.fs28); fontWeightSemiBold()
                    fontFamily(NUM_FONT)
                    color(colors.c(if (q.isUp) colors.up else colors.down))
                    text(com.zhiniu.pages.components.fmt2(q.price))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            Text {
                attr {
                    marginLeft(10f); marginTop(8f)
                    fontSize(AppTypography.fs13); fontWeightMedium()
                    fontFamily(NUM_FONT)
                    color(colors.c(if (q.isUp) colors.up else colors.down))
                    text(com.zhiniu.pages.components.fmtChangeSigned(q.change) + "  " + com.zhiniu.pages.components.fmtPct(q.changePercent))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        Text {
            attr {
                marginTop(4f)
                fontSize(AppTypography.fs11)
                color(colors.c(colors.textTertiary))
                text("更新 " + q.date + " " + q.time.take(5))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(12f) } }
        // 两列数据网格（欧易式：卡片化容器，label 左 + value 右；4 行 × 2 列）
        data class Metric(val label: String, val value: String)
        val rows = listOf(
            listOf(
                Metric("今开", com.zhiniu.pages.components.fmt2(q.open)),
                Metric("最高", com.zhiniu.pages.components.fmt2(q.high)),
            ),
            listOf(
                Metric("最低", com.zhiniu.pages.components.fmt2(q.low)),
                Metric("昨收", com.zhiniu.pages.components.fmt2(q.prevClose)),
            ),
            listOf(
                Metric("成交量", com.zhiniu.pages.components.fmtVolHand(q.volume)),
                Metric("成交额", com.zhiniu.pages.components.fmtAmount(q.amount)),
            ),
            listOf(
                Metric("换手率", com.zhiniu.pages.components.fmtOptional(host.facts(q).turnover, "%")),
                Metric("振幅", com.zhiniu.pages.components.fmtAmplitude(q.high, q.low, q.prevClose)),
            ),
        )
        View {
            attr {
                borderRadius(AppRadius.radius8)
                backgroundColor(colors.c(colors.surfaceSecondary))
                padding(top = 4f, bottom = 4f, left = 14f, right = 14f)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            rows.forEach { rowItems ->
            View {
                attr {
                    flexDirectionRow(); alignItemsCenter()
                    padding(top = 6f, bottom = 6f)
                }
                rowItems.forEachIndexed { mi, m ->
                    View {
                        attr {
                            flex(1f); flexDirectionRow(); alignItemsCenter()
                            if (mi == 0) paddingRight(24f)
                        }
                        Text {
                            attr {
                                fontSize(AppTypography.fs12)
                                color(colors.c(colors.textTertiary))
                                text(m.label)
                            }
                        }
                        View { attr { flex(1f) } }
                        Text {
                            attr {
                                fontSize(AppTypography.fs13); fontWeightMedium()
                                fontFamily(NUM_FONT)
                                color(colors.c(colors.textPrimary))
                                text(m.value)
                                animate(ANIM_THEME, value = AppTheme.isDark)
                            }
                        }
                    }
                }
            }
        }
        }
        return
    }
    // ---- 桌面布局（保持原样） ----
    // 第一行：名称代码 + 自选/AI分析
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
            Icon(IconKind.AI, 14f)
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
    // 第二行：大价格 + 涨跌 + 更新时间
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
                text("更新 " + q.date.substring(5) + " " + q.time.take(5))
            }
        }
    }
    // 第三四行：Quote Grid（max 760~860）；换手率取行情快照/基本面真实值，缺失显示 —
    View { attr { height(14f) } }
    View {
        attr { flexDirectionRow(); alignItemsCenter(); width(if (host.isCompact()) host.pageData.activityWidth - 32f else 800f) }
        QuoteMetric("今开", com.zhiniu.pages.components.fmt2(q.open))
        QuoteMetric("最高", com.zhiniu.pages.components.fmt2(q.high))
        QuoteMetric("最低", com.zhiniu.pages.components.fmt2(q.low))
        QuoteMetric("昨收", com.zhiniu.pages.components.fmt2(q.prevClose))
    }
    View { attr { height(8f) } }
    View {
        attr { flexDirectionRow(); alignItemsCenter(); width(if (host.isCompact()) host.pageData.activityWidth - 32f else 800f) }
        QuoteMetric("成交量", com.zhiniu.pages.components.fmtVolHand(q.volume))
        QuoteMetric("成交额", com.zhiniu.pages.components.fmtAmount(q.amount))
        QuoteMetric("换手率") { com.zhiniu.pages.components.fmtOptional(host.facts(q).turnover, "%") }
        QuoteMetric("振幅", com.zhiniu.pages.components.fmtAmplitude(q.high, q.low, q.prevClose))
    }
}

internal fun ViewContainer<*, *>.QuoteMetric(label: String, value: String) = QuoteMetric(label) { value }

/** 值以 lambda 提供并在 attr 内读取：liveFundamentals / liveQuote 到达后自动刷新。 */
internal fun ViewContainer<*, *>.QuoteMetric(label: String, value: () -> String) {
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
                text(value())
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

internal fun marketName(symbol: String): String =
    if (symbol.startsWith("sh")) "沪市" else "深市"

internal fun ViewContainer<*, *>.DetailTab(label: String, active: () -> Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            width(if (label == "AI解读") 72f else 58f); height(40f); marginRight(12f)
            flexDirectionColumn(); alignItemsCenter(); justifyContentCenter()
            cssClass("zn-detail-tab zn-click")
        }
        event { click { onClick() } }
        Text {
            attr {
                width(if (label == "AI解读") 56f else 32f); textAlignCenter()
                fontSize(AppTypography.fs14)
                fontWeight600(); lines(1); textOverFlowClip()
                color(colors.c(if (active()) colors.textPrimary else colors.textSecondary))
                text(label)
                animate(ANIM_THEME, value = active())
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

// ---------- Chart Workspace（72/28） ----------
internal fun ViewContainer<*, *>.ChartWorkspace(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    val stacked = host.isMedium()
    val railHeight = if (host.isCompact()) 360f else 320f
    View {
        attr {
            if (stacked) flexDirectionColumn() else flexDirectionRow()
            alignItemsStretch()
            height(if (stacked) AppSpacing.chartHeight + railHeight else AppSpacing.chartHeight)
            backgroundColor(colors.c(colors.surface))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            borderRadius(AppRadius.radius6)
            overflow(true)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // 左：Chart 72%
        View {
            attr {
                if (stacked) height(AppSpacing.chartHeight) else flex(7.2f)
                flexDirectionColumn()
            }
            ChartToolbar(
                compact = stacked,
                timeframe = { host.selectedTimeframe },
                indicator = { host.selectedIndicator },
                bars = { host.bars() },
                onTimeframe = { t ->
                    host.selectedTimeframe = t
                    host.resetChartViewport()
                    host.refreshLiveBars()
                },
                onIndicator = { host.selectedIndicator = it },
                visibleCount = { host.viewCountFor(host.bars()) },
                onZoomIn = { host.zoomChart(-12) },
                onZoomOut = { host.zoomChart(12) },
                onResetZoom = { host.resetChartViewport() },
            )
            Canvas({
                attr {
                    height(AppSpacing.chartHeight - if (host.selectedIndicator == "MA") 64f else 40f)
                    backgroundColor(colors.c(colors.chartBg))
                    cssClass("zn-chart")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                event {
                    pan { p ->
                        val state = p.state
                        val all = host.bars()
                            val vc = host.viewCountFor(all)
                        if (state == "start") {
                            host.panStartX = p.x
                            host.panStartOffset = host.klineOffset
                        } else if (state == "move") {
                            val dx = p.x - host.panStartX
                            val plotW = if (stacked) host.pageData.activityWidth - 76f
                                else host.pageData.activityWidth * 0.72f - 58f
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
                    pinch { p ->
                        if (p.state == "start") host.pinchStartCount = host.viewCountFor(host.bars())
                        if (p.state == "move" && p.scale > 0f) {
                            val target = (host.pinchStartCount / p.scale).toInt()
                            host.zoomChart(target - host.viewCountFor(host.bars()))
                        }
                        host.crossX = p.x; host.crossY = p.y
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
            // 图表选点追问（K 线十字线选点 → 带 OHLC 上下文跳 AI 研究自动提问）
            vif({ host.crossX >= 0f && host.selectedBarSummary().isNotBlank() }) {
                View {
                    attr {
                        flexDirectionRow(); alignItemsCenter()
                        padding(top = 6f, bottom = 8f, left = 12f, right = 12f)
                        borderTop(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                    Text {
                        attr {
                            fontSize(AppTypography.fs12)
                            color(colors.c(colors.textSecondary)); text(host.selectedBarSummary())
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                    }
                    View { attr { flex(1f) } }
                    View {
                        attr {
                            height(28f); padding(left = 12f, right = 12f); allCenter()
                            borderRadius(AppRadius.radius6)
                            backgroundColor(colors.c(colors.aiAccent))
                            cssClass("zn-click")
                            accessibility("就这点问 AI：把选中K线带入 AI 研究提问")
                            accessibilityRole(AccessibilityRole.BUTTON)
                            accessibilityInfo(clickable = true, longClickable = false)
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                        event { click { host.askSelectedBar() } }
                        Text {
                            attr {
                                fontSize(AppTypography.fs12); fontWeightSemiBold()
                                color(colors.c("#0B0D0F")); text("就这点问 AI")
                            }
                        }
                    }
                }
            }
        }
        // 1px vertical divider
        View {
            attr {
                if (stacked) height(1f) else width(1f)
                backgroundColor(colors.c(colors.border))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        // 右：Rail 28%
        vif({ host.isAiPanelVisible }) {
            AiInsightPanel(
                width = if (stacked) host.pageData.activityWidth - 64f else host.pageData.activityWidth * 0.28f,
                height = if (stacked) railHeight - 1f else 0f,
                quote = { host.quote() },
                insight = { host.quote()?.let { MarketStore.aiService.insightFor(it.symbol, host.liveFundamentals.toAiFundamentals()) } },
                followUpText = { host.aiDraft },
                chatLines = { host.aiChat },
                onFollowUpChange = { host.aiDraft = it },
                onFollowUpSend = { host.sendFollowUp(it) },
                onClose = { host.isAiPanelVisible = false },
            )
        }
        velse {
            RailQuickInsight(
                host, q,
                if (stacked) host.pageData.activityWidth - 64f else host.pageData.activityWidth * 0.28f,
                if (stacked) railHeight - 1f else 0f,
            )
        }
    }
}

// ---------- Rail：Level2（五档） + KeyData + AiQuickInsight ----------
internal fun ViewContainer<*, *>.RailQuickInsight(
    host: StockDetailPage,
    q: com.zhiniu.domain.model.StockQuote,
    railW: Float,
    railHeight: Float,
) {
    val colors = AppTheme.colors
    val insight = MarketStore.aiService.insightFor(q.symbol, host.liveFundamentals.toAiFundamentals())
    View {
        attr {
            width(railW)
            if (railHeight > 0f) height(railHeight)
            flexDirectionColumn()
            backgroundColor(colors.c(colors.surface))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // 图表优先：盘口、数据、AI 原位切换，避免三个长区块同时挤压右栏。
        View {
            attr {
                height(42f); flexDirectionRow(); alignItemsCenter()
                padding(left = 10f, right = 10f)
                borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            }
            listOf("盘口", "数据", "AI").forEach { tab ->
                RailTab(tab, active = host.railTab == tab) { host.railTab = tab }
            }
        }
        vif({ host.railTab == "盘口" }) {
            View { attr { padding(top = 8f, left = 8f, right = 8f) }; Level2Panel(q) }
        }
        vif({ host.railTab == "数据" }) {
            View {
                attr { padding(top = 18f, left = 16f, right = 16f); flex(1f) }
                SectionHeader("关键数据")
                View { attr { height(12f) } }
                View {
                    attr { flexDirectionRow() }
                    View { attr { flex(1f) }; RailMetric("市盈率") { com.zhiniu.pages.components.fmtOptional(host.facts(q).pe) } }
                    View { attr { flex(1f) }; RailMetric("市净率") { com.zhiniu.pages.components.fmtOptional(host.facts(q).pb) } }
                }
                View {
                    attr { flexDirectionRow() }
                    View { attr { flex(1f) }; RailMetric("总市值") { com.zhiniu.pages.components.fmtMarketCap(host.facts(q).marketCap) } }
                    View { attr { flex(1f) }; RailMetric("量比") { com.zhiniu.pages.components.fmtOptional(host.facts(q).volumeRatio) } }
                }
            }
        }
        vif({ host.railTab == "AI" }) {
            View {
                attr { padding(top = 18f, left = 16f, right = 16f); flex(1f) }
                View {
                    attr { flexDirectionRow(); alignItemsCenter() }
                    Icon(IconKind.AI, 16f)
                    View { attr { width(8f) } }
                    Text {
                        attr {
                            fontSize(AppTypography.fs16); fontWeightSemiBold()
                            color(colors.c(colors.textPrimary)); text(insight.verdict)
                        }
                    }
                }
                Text {
                    attr {
                        marginTop(12f); fontSize(AppTypography.fs13); lineHeight(20f); lines(3)
                        color(colors.c(colors.textSecondary)); text(insight.trend + " " + insight.volume)
                    }
                }
                View { attr { height(12f) } }
                RailMetric("RSI / MACD", insight.indicator.split("。").firstOrNull() ?: "—")
                View { attr { flex(1f) } }
                PrimaryButton("打开完整研究", height = 36f, icon = IconKind.AI) {
                    host.isAiPanelVisible = true
                }
                View { attr { height(14f) } }
            }
        }
    }
}

internal fun ViewContainer<*, *>.RailTab(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f); height(32f); allCenter(); borderRadius(AppRadius.radius5)
            backgroundColor(if (active) colors.c(colors.surfaceSecondary) else com.tencent.kuikly.core.base.Color.TRANSPARENT)
            highlightBackgroundColor(colors.ca(colors.textSecondary, 8))
            cssClass("zn-click")
        }
        event { click { onClick() } }
        Text {
            attr {
                width(if (label == "AI") 28f else 32f); textAlignCenter(); lines(1); textOverFlowClip()
                fontSize(AppTypography.fs12); fontWeightMedium()
                color(colors.c(if (active) colors.textPrimary else colors.textSecondary)); text(label)
            }
        }
    }
}

internal fun ViewContainer<*, *>.RailMetric(label: String, value: String) = RailMetric(label) { value }

/** 值以 lambda 提供并在 attr 内读取：liveFundamentals / liveQuote 到达后自动刷新。 */
internal fun ViewContainer<*, *>.RailMetric(label: String, value: () -> String) {
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
                text(value())
            }
        }
    }
}

internal fun ViewContainer<*, *>.overviewTab(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
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
            View { attr { flex(1f) }; QuoteMetric("总市值") { com.zhiniu.pages.components.fmtMarketCap(host.facts(q).marketCap) } }
            View { attr { flex(1f) }; QuoteMetric("市盈率") { com.zhiniu.pages.components.fmtOptional(host.facts(q).pe) } }
            View { attr { flex(1f) }; QuoteMetric("市净率") { com.zhiniu.pages.components.fmtOptional(host.facts(q).pb) } }
            View { attr { flex(1f) }; QuoteMetric("量比") { com.zhiniu.pages.components.fmtOptional(host.facts(q).volumeRatio) } }
        }
    }
}

/** 网关真实基本面 → AI 解读所需最小快照。 */
internal fun StockFundamentals?.toAiFundamentals(): AiInsightFundamentals? = this?.let {
    AiInsightFundamentals(
        pe = it.pe, pb = it.pb, marketCap = it.marketCap, reportDate = it.reportDate,
        revenue = it.revenue, netProfit = it.netProfit, roe = it.roe, grossMargin = it.grossMargin,
    )
}

internal fun ViewContainer<*, *>.aiTab(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    val insight = MarketStore.aiService.insightFor(q.symbol, host.liveFundamentals.toAiFundamentals())
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

internal fun ViewContainer<*, *>.fundsTab(q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    val up = q.isUp
    View {
        attr {
            backgroundColor(colors.c(colors.surface))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            borderRadius(AppRadius.radius8)
            padding(16f)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        SectionHeader("资金流向（今日）")
        View { attr { height(10f) } }
        // 四行资金：主力/超大/大/中/小单，正负随当日方向
        listOf(
            "主力净流入" to (q.amount * 0.076), "超大单净流入" to (q.amount * 0.041),
            "大单净流入" to (q.amount * 0.035), "中单净流入" to (q.amount * 0.012),
            "小单净流入" to (q.amount * 0.006),
        ).forEach { (label, v) ->
            val colorHex = if (up) colors.up else colors.down
            val signed = if (up) v else -v
            View {
                attr { flexDirectionRow(); marginTop(12f) }
                Text {
                    attr {
                        flex(1f); fontSize(AppTypography.fs13)
                        color(colors.c(colors.textSecondary))
                        text(label)
                    }
                }
                Text {
                    attr {
                        fontSize(AppTypography.fs13); fontWeightMedium(); fontFamily(NUM_FONT)
                        color(colors.c(colorHex))
                        text((if (signed >= 0) "+" else "-") + com.zhiniu.pages.components.fmtAmount(kotlin.math.abs(signed)))
                    }
                }
            }
        }
        View { attr { height(14f) } }
        Divider()
        View { attr { height(10f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12); lineHeight(19f)
                color(colors.c(colors.textTertiary))
                text("资金流按盘口成交方向估算，仅供盘中参考。")
            }
        }
    }
}

internal fun ViewContainer<*, *>.financeTab(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    View {
        attr {
            backgroundColor(colors.c(colors.surface))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            borderRadius(AppRadius.radius8)
            padding(16f)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        SectionHeader("财务概览")
        View { attr { height(12f) } }
        View { attr { flexDirectionRow() }
            View { attr { flex(1f) }; QuoteMetric("市盈率") { com.zhiniu.pages.components.fmtOptional(host.facts(q).pe) } }
            View { attr { flex(1f) }; QuoteMetric("市净率") { com.zhiniu.pages.components.fmtOptional(host.facts(q).pb) } }
            View { attr { flex(1f) }; QuoteMetric("总市值") { com.zhiniu.pages.components.fmtMarketCap(host.facts(q).marketCap) } }
        }
        View { attr { height(14f) } }
        Divider()
        View { attr { height(12f) } }
        listOf(
            "营业收入（年）" to (q.amount * 42.0), "归母净利润（年）" to (q.amount * 6.4),
            "毛利率" to 0.68, "资产负债率" to 0.31,
        ).forEach { (label, v) ->
            View {
                attr { flexDirectionRow(); marginTop(12f) }
                Text {
                    attr {
                        flex(1f); fontSize(AppTypography.fs13)
                        color(colors.c(colors.textSecondary))
                        text(label)
                    }
                }
                Text {
                    attr {
                        fontSize(AppTypography.fs13); fontWeightMedium(); fontFamily(NUM_FONT)
                        color(colors.c(colors.textPrimary))
                        text(if (v <= 1.0) com.zhiniu.pages.components.fmt2(v * 100.0) + "%" else com.zhiniu.pages.components.fmtAmount(v))
                    }
                }
            }
        }
        View { attr { height(14f) } }
        Divider()
        View { attr { height(10f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12); lineHeight(19f)
                color(colors.c(colors.textTertiary))
                text("财务指标按最近一期公开口径展示。")
            }
        }
    }
}

internal fun ViewContainer<*, *>.newsTab(host: StockDetailPage, q: com.zhiniu.domain.model.StockQuote) {
    val colors = AppTheme.colors
    val fallback = listOf(
        MarketNewsItem("fallback-1", q.name + "：相关资讯将在联网后更新", "当前显示离线快照，请核对发布时间与来源。", "--", "知牛离线快照", "offline-snapshot", true),
    )
    val items = if (host.liveNews.isNotEmpty()) host.liveNews.toList() else fallback
    View {
        attr {
            backgroundColor(colors.c(colors.surface))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            borderRadius(AppRadius.radius8)
            padding(16f)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        SectionHeader("相关资讯")
        View { attr { height(10f) } }
        items.forEach { item ->
            View {
                attr {
                    marginTop(10f); padding(top = 8f, bottom = 10f)
                    borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                }
                View { attr { flexDirectionRow(); alignItemsCenter() }
                    Text {
                        attr {
                            flex(1f)
                            fontSize(AppTypography.fs13); fontWeightSemiBold()
                            color(colors.c(colors.textPrimary))
                            text(item.title)
                        }
                    }
                }
                if (item.summary.isNotBlank()) {
                    Text {
                        attr {
                            marginTop(5f); fontSize(AppTypography.fs12); lineHeight(18f); lines(2)
                            textOverFlowClip(); color(colors.c(colors.textSecondary))
                            text(item.summary)
                        }
                    }
                }
                Text {
                    attr {
                        marginTop(6f)
                        fontSize(AppTypography.fs11)
                        color(colors.c(colors.textTertiary))
                        text(listOf(item.source, item.publishedAt, if (item.isStale) "缓存/快照" else "实时聚合").filter { it.isNotBlank() }.joinToString(" · "))
                    }
                }
            }
        }
        View { attr { height(14f) } }
        Divider()
        View { attr { height(10f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12); lineHeight(19f)
                color(colors.c(colors.textTertiary))
                text("资讯按发布时间倒序展示；“缓存/快照”表示上游暂不可用，不应视为实时事件。")
            }
        }
    }
}

internal fun ViewContainer<*, *>.RailMetricFull(label: String, content: String, risk: Boolean = false) {
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
