// 知牛 · 个股详情 + AI 抽屉（壳内覆盖视图；K线 Canvas 为核心）
package com.zhiniu.pages

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.velseif
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.ActivityIndicator
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.*
import com.zhiniu.pages.components.chart.ChartIndicator
import com.zhiniu.pages.components.chart.drawKLineChart
import com.zhiniu.pages.components.common.*

private val TIMEFRAMES = listOf("分时", "日K", "周K", "月K", "5分", "15分", "30分", "60分")

/** 详情视图（替换市场内容区显示）。 */
internal fun ViewContainer<*, *>.detailSection(host: MarketShell) {
    val q: Quote = host.openSymbol?.let { host.quoteOf(it) } ?: return
    vif({ host.detailLoading }) { detailSkeleton() }
    velse { detailContent(host, q) }
}

// ---------- 骨架 ----------
private fun ViewContainer<*, *>.detailSkeleton() {
    View { attr { marginTop(32f) } }
    SkeletonBar(90f, 14f)
    View { attr { height(18f) } }
    SkeletonBar(220f, 26f)
    View { attr { height(10f) } }
    SkeletonBar(120f, 30f)
    View { attr { height(24f) } }
    View { attr { flexDirectionRow() }
        SkeletonBar(150f, 12f)
        View { attr { flex(1f) } }
        SkeletonBar(120f, 12f)
        View { attr { width(12f) } }
        SkeletonBar(120f, 12f)
        View { attr { width(12f) } }
        SkeletonBar(120f, 12f)
        View { attr { width(12f) } }
        SkeletonBar(120f, 12f)
    }
    View { attr { height(28f) } }
    View {
        attr { flexDirectionRow() }
        View {
            attr { flex(7f); height(460f); borderRadius(10f); backgroundColor(AppTheme.colors.c(AppTheme.colors.surface)) }
        }
        View { attr { width(16f) } }
        View {
            attr { flex(3f); height(460f); borderRadius(10f); backgroundColor(AppTheme.colors.c(AppTheme.colors.surface)) }
        }
    }
}

// ---------- 详情内容 ----------
private fun ViewContainer<*, *>.detailContent(host: MarketShell, q: Quote) {
    val colors = { AppTheme.colors }
    val facts = factsOf(q)
    val view = aiViewOf(q)

    // 返回
    View {
        attr {
            height(34f); flexDirectionRow(); alignItemsCenter()
            borderRadius(8f)
            padding(right = 12f)
            cssClass("zn-nav zn-click")
        }
        event { click { host.closeDetail() } }
        Icon(IconKind.ARROW_LEFT, 16f, { colors().textSecondary })
        Text {
            attr {
                marginLeft(6f)
                fontSize(AppTypography.fs14)
                color(colors().c(colors().textSecondary))
                text("市场")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
    View { attr { height(16f) } }
    // 身份 + 操作
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs24); fontWeightSemiBold()
                color(colors().c(colors().textPrimary))
                text(q.name)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        Text {
            attr {
                marginLeft(14f); marginTop(6f)
                fontSize(AppTypography.fs13)
                color(colors().c(colors().textTertiary))
                text(fmtSymbol(q.symbol) + " · " + marketLabelOf(q.symbol))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { flex(1f) } }
        FavoriteButton({ host.watchlisted(q.symbol) }, 34f) { host.toggleWatch(q.symbol) }
        View { attr { width(10f) } }
        PrimaryButton("AI 分析", 34f) {
            host.aiOpen = true
            host.aiAnalyzing = true
            setTimeout(700) { host.aiAnalyzing = false }
        }
    }
    View { attr { height(16f) } }
    // 价格
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(32f); fontWeightSemiBold()
                fontFamily(NUM_FONT)
                color(colors().c(if (q.isUp) colors().up else colors().down))
                text(fmt2(q.price))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        Text {
            attr {
                marginLeft(14f); marginTop(10f)
                fontSize(AppTypography.fs15); fontWeightMedium()
                fontFamily(NUM_FONT)
                color(colors().c(if (q.isUp) colors().up else colors().down))
                text(fmtChangeSigned(q.change))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        Text {
            attr {
                marginLeft(8f); marginTop(10f)
                fontSize(AppTypography.fs15); fontWeightMedium()
                fontFamily(NUM_FONT)
                color(colors().c(if (q.isUp) colors().up else colors().down))
                text(fmtPct(q.changePercent))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textTertiary))
                text("更新 ${q.date} ${q.time}")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
    // 紧凑行情指标（2 行 × 4）
    View { attr { marginTop(20f) } }
    QuoteMetricGrid(
        "今开" to fmt2(q.open),
        "最高" to fmt2(q.high),
        "最低" to fmt2(q.low),
        "昨收" to fmt2(q.prevClose),
    )
    View { attr { marginTop(12f) } }
    QuoteMetricGrid(
        "成交量" to fmtVolHand(q.volume),
        "成交额" to fmtAmount(q.amount),
        "换手率" to fmt2(facts.turnover) + "%",
        "振幅" to fmtAmplitude(q.high, q.low, q.prevClose),
    )
    View { attr { height(20f) } }
    Divider()
    View { attr { height(20f) } }
    // 主区：K线（72%）+ 侧栏（28%）
    vif({ host.isNarrow() }) {
        chartPanel(host, q)
        View { attr { height(16f) } }
        sidebarPanel(host, q, facts, view)
    }
    velse {
        View {
            attr { flexDirectionRow(); alignItemsFlexStart() }
            View {
                attr { flex(7f); marginRight(16f) }
                chartPanel(host, q)
            }
            View { attr { flex(3f) } }
            sidebarPanel(host, q, facts, view)
        }
    }
    View { attr { height(40f) } }
    // 详情底部 Tabs
    detailTabs(host, q)
}

// ---------- K线面板 ----------
private fun ViewContainer<*, *>.chartPanel(host: MarketShell, q: Quote) {
    val colors = { AppTheme.colors }
    View {
        attr {
            backgroundColor(colors().c(colors().surface))
            border(Border(1f, BorderStyle.SOLID, colors().c(colors().borderStrong)))
            borderRadius(10f)
            cssClass("zn-card")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // 周期（第一维度）
        View {
            attr { padding(top = 10f, left = 12f, right = 12f) }
            AppTabs(TIMEFRAMES, { host.timeframe }, 56f, 36f) { t ->
                host.timeframe = t
                host.crossX = -1f; host.crossY = -1f
            }
        }
        // 指标（第二维度，独立一组）
        View {
            attr {
                flexDirectionRow(); alignItemsCenter()
                padding(left = 12f, right = 12f)
            }
            SegmentedTabs(listOf("MA", "MACD", "RSI"), { host.indicator.name }, 30f) { t ->
                host.indicator = when (t) {
                    "MACD" -> ChartIndicator.MACD
                    "RSI" -> ChartIndicator.RSI
                    else -> ChartIndicator.MA
                }
            }
            View { attr { flex(1f) } }
            vif({ host.indicator == ChartIndicator.MA }) {
                val bars = host.klineFor(q.symbol, host.timeframe)
                maLegend("MA5", maValue(bars, 5), colors().ma5)
                maLegend("MA10", maValue(bars, 10), colors().ma10)
                maLegend("MA20", maValue(bars, 20), colors().ma20)
            }
        }
        View { attr { height(6f) } }
        // K 线 Canvas
        Canvas({
            attr {
                height(440f)
                backgroundColor(colors().c(colors().chartBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            event {
                pan { pt ->
                    host.crossX = pt.x
                    host.crossY = pt.y
                }
                click { pt ->
                    host.crossX = pt.x
                    host.crossY = pt.y
                }
            }
        }) { context, w, h ->
            val palette = AppTheme.colors
            drawKLineChart(
                context, w, h,
                host.klineFor(q.symbol, host.timeframe),
                palette, host.indicator, host.crossX, host.crossY,
            )
        }
        View { attr { height(10f) } }
    }
}

private fun ViewContainer<*, *>.maLegend(label: String, value: Double?, hex: String) {
    Text {
        attr {
            marginLeft(10f)
            fontSize(AppTypography.fs11)
            fontFamily(NUM_FONT)
            color(AppTheme.colors.c(hex))
            text(label + " " + (value?.let { fmt2(it) } ?: "--"))
        }
    }
}

private fun maValue(bars: List<com.zhiniu.domain.model.KLineBar>, n: Int): Double? {
    if (bars.size < n) return null
    var sum = 0.0
    for (i in bars.size - n until bars.size) sum += bars[i].close
    return sum / n
}

// ---------- 侧栏：关键数据 + AI 观点 ----------
private fun ViewContainer<*, *>.sidebarPanel(
    host: MarketShell,
    q: Quote,
    facts: StockFacts,
    view: AiView,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            backgroundColor(colors().c(colors().surface))
            border(Border(1f, BorderStyle.SOLID, colors().c(colors().borderStrong)))
            borderRadius(10f)
            cssClass("zn-card")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr { padding(top = 14f, left = 16f, right = 16f) }
            SectionHeader("关键数据")
            View { attr { height(6f) } }
            MetricRow("市盈率", fmt2(facts.pe))
            MetricRow("市净率", fmt2(facts.pb))
            MetricRow("总市值", facts.marketCap)
            MetricRow("换手率", fmt2(facts.turnover) + "%")
            MetricRow("量比", fmt2(facts.volumeRatio))
            MetricRow("52周高", fmt2(facts.week52High))
            View { attr { height(4f) } }
            Divider()
            View { attr { height(12f) } }
            SectionHeader("AI 观点")
            Text {
                attr {
                    marginTop(8f)
                    fontSize(20f); fontWeightSemiBold()
                    color(colors().c(colors().textPrimary))
                    text(view.verdict)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            Text {
                attr {
                    marginTop(6f)
                    fontSize(AppTypography.fs12); lineHeight(19f)
                    color(colors().c(colors().textSecondary))
                    text(view.observe)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(12f) } }
            SecondaryButton("查看完整分析", 34f) {
                host.aiOpen = true
                host.aiAnalyzing = true
                setTimeout(700) { host.aiAnalyzing = false }
            }
            View { attr { height(12f) } }
        }
    }
}

// ---------- 详情底部 Tabs ----------
private fun ViewContainer<*, *>.detailTabs(host: MarketShell, q: Quote) {
    val tabs = listOf("概览", "资金", "财务", "新闻", "AI解读")
    AppTabs(tabs, { host.detailTab }, 76f, 40f) { t -> host.detailTab = t }
    Divider()
    View { attr { height(16f) } }
    vif({ host.detailTab == "概览" }) { overviewTab(host, q) }
    velseif({ host.detailTab == "资金" }) { fundTab(host, q) }
    velseif({ host.detailTab == "财务" }) { financeTab(host, q) }
    velseif({ host.detailTab == "新闻" }) { newsTab(host, q) }
    velse { aiTab(host, q) }
}

private fun ViewContainer<*, *>.overviewTab(host: MarketShell, q: Quote) {
    val colors = { AppTheme.colors }
    AppCard {
        SectionHeader("公司概览")
        Text {
            attr {
                marginTop(8f)
                fontSize(AppTypography.fs14); fontWeightMedium()
                color(colors().c(colors().textPrimary))
                text(q.name + "股份有限公司")
            }
        }
        Text {
            attr {
                marginTop(3f)
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textSecondary))
                text(if (q.symbol.startsWith("sh600519") || q.symbol.startsWith("sz000858") || q.symbol.startsWith("sh600809") || q.symbol.startsWith("sz000568")) "食品饮料 · 白酒" else "沪深 A 股 · 主板")
            }
        }
        View { attr { height(14f) } }
        Divider()
        View { attr { height(12f) } }
        SectionHeader("关键指标")
        View { attr { height(6f) } }
        val facts = factsOf(q)
        View { attr { flexDirectionRow() }
            View { attr { flex(1f) }; QuoteMetric("总市值", facts.marketCap) }
            View { attr { flex(1f) }; QuoteMetric("市盈率", fmt2(facts.pe)) }
            View { attr { flex(1f) }; QuoteMetric("市净率", fmt2(facts.pb)) }
            View { attr { flex(1f) }; QuoteMetric("ROE", fmt2(9.0 + stableHash(q.symbol) % 180 / 10.0) + "%") }
            View { attr { flex(1f) }; QuoteMetric("股息率", fmt2(1.2 + stableHash(q.symbol) % 40 / 10.0) + "%") }
        }
    }
    View { attr { height(16f) } }
    Text {
        attr {
            fontSize(AppTypography.fs12); lineHeight(20f)
            color(colors().c(colors().textTertiary))
            text(q.name + "（" + q.symbol + "）是所属行业的代表性公司，品牌壁垒与定价权突出，盈利能力稳定。以上为演示数据。")
        }
    }
}

private fun ViewContainer<*, *>.fundTab(host: MarketShell, q: Quote) {
    val h = stableHash(q.symbol)
    AppCard {
        SectionHeader("资金流向")
        View { attr { height(6f) } }
        MetricRow("主力净流入", fmt2(0.5 + h % 300 / 100.0) + "亿", { if (h % 2 == 0) AppTheme.colors.up else AppTheme.colors.down })
        MetricRow("主力净流入占比", fmt2(8.0 + h % 120 / 10.0) + "%")
        MetricRow("超大单", fmt2(0.2 + h % 150 / 100.0) + "亿")
        MetricRow("大单", fmt2(0.1 + h % 90 / 100.0) + "亿")
        MetricRow("北向资金", (if (h % 2 == 0) "+" else "-") + fmt2(0.1 + h % 80 / 100.0) + "亿")
    }
}

private fun ViewContainer<*, *>.financeTab(host: MarketShell, q: Quote) {
    val h = stableHash(q.symbol)
    AppCard {
        SectionHeader("财务概览")
        View { attr { height(6f) } }
        MetricRow("营业收入", fmt2(180.0 + h % 400 / 10.0) + "亿", { AppTheme.colors.textPrimary })
        MetricRow("营收同比", "+" + fmt2(6.0 + h % 150 / 10.0) + "%", { AppTheme.colors.up })
        MetricRow("净利润", fmt2(40.0 + h % 200 / 10.0) + "亿")
        MetricRow("净利润同比", "+" + fmt2(8.0 + h % 120 / 10.0) + "%", { AppTheme.colors.up })
        MetricRow("毛利率", fmt2(28.0 + h % 500 / 10.0) + "%")
        MetricRow("ROE", fmt2(9.0 + h % 180 / 10.0) + "%")
    }
}

private fun ViewContainer<*, *>.newsTab(host: MarketShell, q: Quote) {
    val h = stableHash(q.symbol)
    val news = listOf(
        q.name + "：公司经营情况正常，产能与渠道保持稳健",
        "机构调研：" + q.name + "获多家机构关注，聚焦行业景气度",
        "行业动态：消费/制造板块资金回流，市场情绪回暖",
    )
    news.forEachIndexed { i, title ->
        View {
            attr {
                height(56f)
                cssClass("zn-row")
            }
            Text {
                attr {
                    fontSize(AppTypography.fs14)
                    color(AppTheme.colors.c(AppTheme.colors.textPrimary))
                    text(title)
                }
            }
            Text {
                attr {
                    marginTop(4f)
                    fontSize(AppTypography.fs11)
                    color(AppTheme.colors.c(AppTheme.colors.textTertiary))
                    text("08-${20 - (h + i) % 6}")
                }
            }
        }
        Divider()
    }
}

private fun ViewContainer<*, *>.aiTab(host: MarketShell, q: Quote) {
    val view = aiViewOf(q)
    AppCard {
        SectionHeader("知牛 AI · 综合判断")
        Text {
            attr {
                marginTop(8f)
                fontSize(20f); fontWeightSemiBold()
                color(AppTheme.colors.c(AppTheme.colors.textPrimary))
                text(view.verdict)
            }
        }
        View { attr { height(12f) } }
        Divider()
        View { attr { height(8f) } }
        MetricRow("趋势", view.trend)
        MetricRow("动量", view.momentum)
        MetricRow("RSI", fmt2(view.rsi))
        MetricRow("估值", view.valuation)
        View { attr { height(4f) } }
        Divider()
        View { attr { height(8f) } }
        MetricRow("关键支撑", fmt2(q.price * 0.98))
        MetricRow("关键压力", fmt2(q.price * 1.03))
        View { attr { height(10f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12); lineHeight(20f)
                color(AppTheme.colors.c(AppTheme.colors.textSecondary))
                text(view.observe + view.risk)
            }
        }
    }
}

// ================= AI 抽屉 =================
internal fun ViewContainer<*, *>.aiDrawer(host: MarketShell) {
    val colors = { AppTheme.colors }
    AppDrawer(visible = { host.aiOpen }, width = 384f, onClose = { host.aiOpen = false }) {
        // 头部
        View {
            attr {
                height(58f); flexDirectionRow(); alignItemsCenter()
                padding(left = 18f, right = 10f)
            }
            Icon(IconKind.SPARKLES, 16f, { colors().textPrimary })
            Text {
                attr {
                    marginLeft(8f)
                    fontSize(AppTypography.fs15); fontWeightSemiBold()
                    color(colors().c(colors().textPrimary))
                    text("知牛 AI")
                }
            }
            View { attr { flex(1f) } }
            IconButton(IconKind.CLOSE, 15f, 34f) { host.aiOpen = false }
        }
        Divider()
        vif({ host.aiAnalyzing }) {
            View {
                attr { height(140f); allCenter() }
                ActivityIndicator {
                    attr { isGrayStyle(true) }
                }
                Text {
                    attr {
                        marginTop(12f)
                        fontSize(AppTypography.fs13)
                        color(colors().c(colors().textSecondary))
                        text("AI 正在分析...")
                    }
                }
            }
        }
        velse {
            vif({ host.openSymbol != null }) {
                val dq = host.openSymbol?.let { host.quoteOf(it) }
                val dv = dq?.let { aiViewOf(it) }
                View {
                    attr { padding(top = 14f, left = 18f, right = 18f) }
                    Text {
                        attr {
                            fontSize(AppTypography.fs18); fontWeightSemiBold()
                            color(colors().c(colors().textPrimary))
                            text(dq?.name ?: "")
                        }
                    }
                    Text {
                        attr {
                            marginTop(3f)
                            fontSize(AppTypography.fs12)
                            color(colors().c(colors().textTertiary))
                            text(dq?.let { fmtSymbol(it.symbol) } ?: "")
                        }
                    }
                }
                View { attr { height(12f) } }
                View {
                    attr { padding(left = 18f, right = 18f) }
                    Text {
                        attr {
                            fontSize(AppTypography.fs12)
                            color(colors().c(colors().textTertiary))
                            text("综合观点")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(20f); fontWeightSemiBold()
                            color(colors().c(colors().textPrimary))
                            text(dv?.verdict ?: "--")
                        }
                    }
                }
                View { attr { height(12f) } }
                View {
                    attr { padding(left = 18f, right = 18f) }
                    MetricRow("趋势", dv?.trend ?: "--")
                    MetricRow("量能", dv?.momentum ?: "--")
                    MetricRow("RSI", dv?.let { fmt2(it.rsi) } ?: "--")
                    MetricRow("估值", dv?.valuation ?: "--")
                }
                View { attr { height(8f) } }
                Divider()
                View { attr { height(10f) } }
                View {
                    attr { padding(left = 18f, right = 18f) }
                    Text {
                        attr {
                            fontSize(AppTypography.fs12); fontWeightSemiBold()
                            color(colors().c(colors().textPrimary))
                            text("关键位置")
                        }
                    }
                    MetricRow("支撑", dq?.let { fmt2(it.price * 0.98) } ?: "--", { colors().up })
                    MetricRow("压力", dq?.let { fmt2(it.price * 1.03) } ?: "--", { colors().down })
                }
                View { attr { height(8f) } }
                Divider()
                View { attr { height(10f) } }
                View {
                    attr { padding(left = 18f, right = 18f) }
                    Text {
                        attr {
                            fontSize(AppTypography.fs12); fontWeightSemiBold()
                            color(colors().c(colors().textPrimary))
                            text("风险")
                        }
                    }
                    Text {
                        attr {
                            marginTop(6f)
                            fontSize(AppTypography.fs12); lineHeight(19f)
                            color(colors().c(colors().textSecondary))
                            text(dv?.risk ?: "")
                        }
                    }
                }
                View { attr { height(10f) } }
                Divider()
            }
            // 聊天
            List {
                attr { flex(1f); padding(top = 10f, left = 18f, right = 18f) }
                vfor({ host.aiChat }) { line ->
                    View {
                        attr { }
                        View { attr { height(8f) } }
                        if (line.role == "user") {
                            View {
                                attr {
                                    alignSelfFlexEnd()
                                    maxWidth(300f)
                                    borderRadius(10f)
                                    backgroundColor(colors().c(colors().surfaceSecondary))
                                }
                                View {
                                    attr { padding(top = 8f, left = 12f, right = 12f, bottom = 8f) }
                                    Text {
                                        attr {
                                            fontSize(AppTypography.fs13); lineHeight(19f)
                                            color(colors().c(colors().textPrimary))
                                            text(line.text)
                                        }
                                    }
                                }
                            }
                        } else {
                            View {
                                attr {
                                    alignSelfFlexStart()
                                    maxWidth(300f)
                                    borderRadius(10f)
                                    backgroundColor(colors().c(colors().surface))
                                    border(Border(1f, BorderStyle.SOLID, colors().c(colors().border)))
                                }
                                View {
                                    attr { padding(top = 8f, left = 12f, right = 12f, bottom = 8f) }
                                    Text {
                                        attr {
                                            fontSize(AppTypography.fs13); lineHeight(19f)
                                            color(colors().c(colors().textSecondary))
                                            text(line.text)
                                        }
                                    }
                                }
                            }
                        }
                        View { attr { height(8f) } }
                    }
                }
            }
        }
        // 输入区
        View {
            attr {
                height(64f); flexDirectionRow(); alignItemsCenter()
                padding(left = 18f, right = 18f)
            }
            AppInput("问问这只股票...", host.aiDraft, 38f, onTextChange = { host.aiDraft = it })
            View { attr { width(10f) } }
            PrimaryButton("发送", 38f) { host.sendAiDraft() }
        }
    }
}
