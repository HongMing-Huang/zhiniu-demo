// 知牛 · 个股详情 + AI 抽屉（壳内覆盖视图，不离开当前页面）
package com.zhiniu.pages

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.*

/** 详情视图（替换市场内容区显示）。 */
internal fun ViewContainer<*, *>.detailSection(host: MarketShell) {
    val q: Quote = host.openSymbol?.let { host.quoteOf(it) } ?: return
    val pal = { ThemeState.palette }
    val facts = factsOf(q)
    val view = aiViewOf(q)

    // ---------- 返回 ----------
    View {
        attr {
            height(34f); flexDirectionRow(); alignItemsCenter()
            borderRadius(8f)
            padding(right = 12f)
            cssClass("zn-nav zn-click")
        }
        event { click { host.closeDetail() } }
        Icon(IconKind.ARROW_LEFT, 16f, pal().textSecondary)
        Text {
            attr {
                marginLeft(6f)
                fontSize(14f)
                color(pal().c(pal().textSecondary))
                text("市场")
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
    View { attr { height(14f) } }
    // ---------- 身份 + 操作 ----------
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(24f); fontWeightSemiBold()
                color(pal().c(pal().textPrimary))
                text(q.name)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        Text {
            attr {
                marginLeft(14f); marginTop(6f)
                fontSize(13f)
                color(pal().c(pal().textTertiary))
                text(fmtSymbol(q.symbol) + " · " + marketLabelOf(q.symbol))
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        View { attr { flex(1f) } }
        watchButton(host, q, pal())
        View { attr { width(10f) } }
        aiButton(host, q, pal())
    }
    View { attr { height(14f) } }
    // ---------- 价格 ----------
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(32f); fontWeightSemiBold()
                color(pal().c(if (q.isUp) pal().up else pal().down))
                text(fmt2(q.price))
                fontFamily(NUM_FONT)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        Text {
            attr {
                marginLeft(14f); marginTop(10f)
                fontSize(14f); fontWeightMedium()
                color(pal().c(if (q.isUp) pal().up else pal().down))
                text(fmtChangeSigned(q.change))
                fontFamily(NUM_FONT)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        Text {
            attr {
                marginLeft(8f); marginTop(10f)
                fontSize(14f); fontWeightMedium()
                color(pal().c(if (q.isUp) pal().up else pal().down))
                text(fmtPct(q.changePercent))
                fontFamily(NUM_FONT)
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(12f)
                color(pal().c(pal().textTertiary))
                text("更新 ${q.date} ${q.time}")
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
    // ---------- 数据条 ----------
    View {
        attr { marginTop(24f); flexDirectionRow(); alignItemsCenter(); height(58f) }
        View { attr { flex(1f) } }
        statCell("今开", fmt2(q.open), pal().c(pal().textPrimary))
        statCell("最高", fmt2(q.high), pal().c(pal().up))
        statCell("最低", fmt2(q.low), pal().c(pal().down))
        statCell("昨收", fmt2(q.prevClose), pal().c(pal().textPrimary))
        statCell("成交额", fmtAmount(q.amount), pal().c(pal().textPrimary))
        statCell("换手率", fmt2(facts.turnover) + "%", pal().c(pal().textPrimary))
        View { attr { flex(1f) } }
    }
    View { attr { height(12f) } }
    Hdiv()
    View { attr { height(20f) } }

    // ---------- 主区：K 线 + 右侧数据 ----------
    View {
        attr { flexDirectionRow(); alignItemsFlexStart() }
        // 左：图表
        View {
            attr {
                flex(1f); marginRight(16f)
                backgroundColor(pal().c(pal().surface))
                border(Border(1f, BorderStyle.SOLID, pal().c(pal().borderStrong)))
                borderRadius(10f)
                cssClass("zn-card")
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
            View {
                attr { padding(top = 8f, left = 14f, right = 14f) }
                klineTabs(host, q, pal())
            }
            View { attr { height(2f) } }
            chartCanvas(host, q, pal())
            View { attr { height(10f) } }
        }
        // 右：关键数据 + AI
        View {
            attr {
                width(300f)
                backgroundColor(pal().c(pal().surface))
                border(Border(1f, BorderStyle.SOLID, pal().c(pal().borderStrong)))
                borderRadius(10f)
                cssClass("zn-card")
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
            View {
                attr { padding(top = 14f, left = 16f, right = 16f) }
                Text {
                    attr {
                        fontSize(15f); fontWeightSemiBold()
                        color(pal().c(pal().textPrimary))
                        text("关键数据")
                        animate(ANIM_THEME, value = ThemeState.isDark)
                    }
                }
                View { attr { height(8f) } }
                kvRow("市盈率", fmt2(facts.pe), pal())
                kvRow("市净率", fmt2(facts.pb), pal())
                kvRow("总市值", facts.marketCap, pal())
                kvRow("量比", fmt2(facts.volumeRatio), pal())
                kvRow("52周高", fmt2(facts.week52High), pal())
                kvRow("52周低", fmt2(facts.week52Low), pal())
                View { attr { height(6f) } }
                Hdiv()
                View { attr { height(12f) } }
                View {
                    attr { flexDirectionRow(); alignItemsCenter() }
                    Icon(IconKind.SPARKLES, 14f, pal().textPrimary)
                    Text {
                        attr {
                            marginLeft(6f)
                            fontSize(13f); fontWeightSemiBold()
                            color(pal().c(pal().textPrimary))
                            text("AI 快速观点")
                            animate(ANIM_THEME, value = ThemeState.isDark)
                        }
                    }
                }
                Text {
                    attr {
                        marginTop(8f)
                        fontSize(20f); fontWeightSemiBold()
                        color(pal().c(pal().textPrimary))
                        text(view.verdict)
                        animate(ANIM_THEME, value = ThemeState.isDark)
                    }
                }
                Text {
                    attr {
                        marginTop(6f)
                        fontSize(12f); lineHeight(19f)
                        color(pal().c(pal().textSecondary))
                        text(view.observe)
                        animate(ANIM_THEME, value = ThemeState.isDark)
                    }
                }
                View { attr { height(10f) } }
                View {
                    attr {
                        flexDirectionRow(); alignSelfStretch()
                        height(34f); borderRadius(8f); allCenter()
                        backgroundColor(pal().c(pal().surfaceSecondary))
                        highlightBackgroundColor(pal().ca(pal().textSecondary, 10))
                        cssClass("zn-nav zn-click")
                    }
                    event { click { host.aiOpen = true } }
                    Icon(IconKind.SPARKLES, 14f, pal().textSecondary)
                    Text {
                        attr {
                            marginLeft(6f)
                            fontSize(13f)
                            color(pal().c(pal().textPrimary))
                            text("查看完整分析")
                            animate(ANIM_THEME, value = ThemeState.isDark)
                        }
                    }
                }
                View { attr { height(12f) } }
            }
        }
    }
    View { attr { height(40f) } }
}

// ---------- 详情内部件 ----------
private fun ViewContainer<*, *>.watchButton(host: MarketShell, q: Quote, pal: Palette) {
    View {
        attr {
            height(34f); borderRadius(8f); flexDirectionRow(); alignItemsCenter()
            padding(left = 12f, right = 12f)
            border(Border(1f, BorderStyle.SOLID, pal.c(pal.borderStrong)))
            backgroundColor(pal.c(pal.surface))
            highlightBackgroundColor(pal.ca(pal.textSecondary, 8))
            cssClass("zn-nav zn-click")
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
        event { click { host.toggleWatch(q.symbol) } }
        Icon(IconKind.STAR, 15f, if (host.watchlisted(q.symbol)) pal.textPrimary else pal.textTertiary, filled = host.watchlisted(q.symbol))
        Text {
            attr {
                marginLeft(6f)
                fontSize(13f)
                color(pal.c(if (host.watchlisted(q.symbol)) pal.textPrimary else pal.textSecondary))
                text(if (host.watchlisted(q.symbol)) "已自选" else "加自选")
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
}

private fun ViewContainer<*, *>.aiButton(host: MarketShell, q: Quote, pal: Palette) {
    View {
        attr {
            height(34f); borderRadius(8f); flexDirectionRow(); alignItemsCenter()
            padding(left = 12f, right = 12f)
            backgroundColor(pal.c(pal.surfaceSecondary))
            highlightBackgroundColor(pal.ca(pal.textSecondary, 12))
            cssClass("zn-nav zn-click")
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
        event { click { host.aiOpen = true } }
        Icon(IconKind.SPARKLES, 15f, pal.textPrimary)
        Text {
            attr {
                marginLeft(6f)
                fontSize(13f); fontWeightMedium()
                color(pal.c(pal.textPrimary))
                text("AI 分析")
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
        }
    }
}

private fun ViewContainer<*, *>.statCell(label: String, value: String, color: Color) {
    val p = { ThemeState.palette }
    View {
        attr { width(176f) }
        Text {
            attr {
                fontSize(11f)
                color(p().c(p().textTertiary))
                text(label)
            }
        }
        Text {
            attr {
                marginTop(4f)
                fontSize(13f); fontWeightMedium()
                color(color)
                text(value)
                fontFamily(NUM_FONT)
            }
        }
    }
}

private fun ViewContainer<*, *>.kvRow(label: String, value: String, pal: Palette) {
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(30f) }
        Text {
            attr {
                fontSize(12f)
                color(pal.c(pal.textSecondary))
                text(label)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(13f); fontWeightMedium()
                color(pal.c(pal.textPrimary))
                text(value)
                fontFamily(NUM_FONT)
            }
        }
    }
}

private fun ViewContainer<*, *>.klineTabs(host: MarketShell, q: Quote, pal: Palette) {
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(34f) }
        val tabs = listOf("分时", "日K", "周K", "月K")
        tabs.forEach { t ->
            View {
                attr {
                    width(56f); height(34f)
                    alignItemsCenter(); justifyContentCenter()
                    cssClass("zn-nav zn-click")
                }
                event { click {
                    host.klineTab = t
                    host.crossX = -1f; host.crossY = -1f
                } }
                Text {
                    attr {
                        fontSize(13f)
                        fontWeight600()
                        color(pal.c(if (host.klineTab == t) pal.textPrimary else pal.textTertiary))
                        text(t)
                    }
                }
            }
        }
        View {
            attr {
                absolutePosition(top = 32f, left = 16f)
                width(24f); height(2f)
                borderRadius(allBorderRadius = 1f)
                backgroundColor(pal.c(pal.textPrimary))
                transform(translate = Translate((tabs.indexOf(host.klineTab) * 56f / 24f), 0f))
                animate(Animation.easeOut(0.16f), value = host.klineTab)
            }
        }
        View { attr { flex(1f) } }
        val bars = host.klineFor(q.symbol, host.klineTab)
        maLegend("MA5", maValue(bars, 5), pal.ma5)
        maLegend("MA10", maValue(bars, 10), pal.ma10)
        maLegend("MA20", maValue(bars, 20), pal.ma20)
    }
}

private fun ViewContainer<*, *>.maLegend(label: String, value: Double?, hex: String) {
    Text {
        attr {
            marginLeft(10f)
            fontSize(11f)
            fontFamily(NUM_FONT)
            color(ThemeState.palette.c(hex))
            text(label + " " + (value?.let { fmt2(it) } ?: "--"))
        }
    }
}

private fun maValue(bars: List<KLineBar>, n: Int): Double? {
    if (bars.size < n) return null
    var sum = 0.0
    for (i in bars.size - n until bars.size) sum += bars[i].close
    return sum / n
}

private fun ViewContainer<*, *>.chartCanvas(host: MarketShell, q: Quote, pal: Palette) {
    Canvas({
        attr {
            height(440f)
            backgroundColor(pal.c(pal.chartBg))
            animate(ANIM_THEME, value = ThemeState.isDark)
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
        val palette = ThemeState.palette
        drawKLineChart(
            context, w, h,
            host.klineFor(q.symbol, host.klineTab),
            palette, host.crossX, host.crossY
        )
    }
}

// ================= AI 抽屉（右侧滑入） =================
internal fun ViewContainer<*, *>.aiDrawer(host: MarketShell) {
    val pal = { ThemeState.palette }
    val q = host.openSymbol?.let { host.quoteOf(it) }
    val view = q?.let { aiViewOf(it) }
    // 弹层遮罩（点击外部关闭，透明不遮挡可见性）
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(Color.TRANSPARENT)
            touchEnable(host.aiOpen)
            opacity(if (host.aiOpen) 1f else 0f)
            animate(Animation.easeOut(0.16f), value = host.aiOpen)
            zIndex(20)
        }
        event { click { host.aiOpen = false } }
    }
    View {
        attr {
            width(384f)
            absolutePosition(top = 0f, bottom = 0f, right = 0f)
            backgroundColor(pal().c(pal().elevated))
            border(Border(1f, BorderStyle.SOLID, pal().c(pal().borderStrong)))
            touchEnable(host.aiOpen)
            opacity(if (host.aiOpen) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetX = if (host.aiOpen) 0f else -24f))
            animate(Animation.easeOut(0.2f), value = host.aiOpen)
            zIndex(21)
        }
        // 头部
        View {
            attr {
                height(58f); flexDirectionRow(); alignItemsCenter()
                padding(left = 18f, right = 10f)
            }
            Icon(IconKind.SPARKLES, 16f, pal().textPrimary)
            Text {
                attr {
                    marginLeft(8f)
                    fontSize(15f); fontWeightSemiBold()
                    color(pal().c(pal().textPrimary))
                    text("知牛 AI")
                }
            }
            View { attr { flex(1f) } }
            IconButton(IconKind.CLOSE, 15f, onClick = { host.aiOpen = false })
        }
        Hdiv()
        vif({ host.openSymbol != null }) {
            val dq = host.openSymbol?.let { host.quoteOf(it) }
            val dv = dq?.let { aiViewOf(it) }
            View {
                attr { padding(top = 14f, left = 18f, right = 18f) }
                Text {
                    attr {
                        fontSize(18f); fontWeightSemiBold()
                        color(pal().c(pal().textPrimary))
                        text(dq?.name ?: "")
                    }
                }
                Text {
                    attr {
                        marginTop(3f)
                        fontSize(12f)
                        color(pal().c(pal().textTertiary))
                        text(dq?.let { fmtSymbol(it.symbol) } ?: "")
                    }
                }
            }
            View { attr { height(12f) } }
            View {
                attr { padding(left = 18f, right = 18f) }
                Text {
                    attr {
                        fontSize(12f)
                        color(pal().c(pal().textTertiary))
                        text("综合观点")
                    }
                }
                Text {
                    attr {
                        marginTop(4f)
                        fontSize(20f); fontWeightSemiBold()
                        color(pal().c(pal().textPrimary))
                        text(dv?.verdict ?: "--")
                    }
                }
            }
            View { attr { height(14f) } }
            View {
                attr { flexDirectionRow(); padding(left = 18f, right = 18f) }
                drawerMetric("趋势", dv?.trend ?: "--", pal())
                drawerMetric("动量", dv?.momentum ?: "--", pal())
            }
            View {
                attr { flexDirectionRow(); padding(left = 18f, right = 18f); marginTop(10f) }
                drawerMetric("RSI", dv?.let { fmt2(it.rsi) } ?: "--", pal())
                drawerMetric("估值", dv?.valuation ?: "--", pal())
            }
            View { attr { height(10f) } }
            Hdiv()
            View { attr { height(14f) } }
            drawerBlock(pal(), "核心观察", dv?.observe ?: "")
            View { attr { height(14f) } }
            drawerBlock(pal(), "风险", dv?.risk ?: "")
            View { attr { height(10f) } }
            Hdiv()
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
                                backgroundColor(pal().c(pal().surfaceSecondary))
                            }
                            View {
                                attr { padding(top = 8f, left = 12f, right = 12f, bottom = 8f) }
                                Text {
                                    attr {
                                        fontSize(13f); lineHeight(19f)
                                        color(pal().c(pal().textPrimary))
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
                                backgroundColor(pal().c(pal().surface))
                                border(Border(1f, BorderStyle.SOLID, pal().c(pal().border)))
                            }
                            View {
                                attr { padding(top = 8f, left = 12f, right = 12f, bottom = 8f) }
                                Text {
                                    attr {
                                        fontSize(13f); lineHeight(19f)
                                        color(pal().c(pal().textSecondary))
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
        // 输入区
        View {
            attr {
                height(64f); flexDirectionRow(); alignItemsCenter()
                padding(left = 18f, right = 18f)
            }
            View {
                attr {
                    flex(1f); height(38f); borderRadius(9f)
                    backgroundColor(pal().c(pal().surfaceSecondary))
                    overflow(true)
                    padding(left = 12f, right = 12f)
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
                Input {
                    attr {
                        height(38f)
                        fontSize(13f)
                        color(pal().c(pal().textPrimary))
                        placeholder("问问这只股票...")
                        placeholderColor(pal().c(pal().textTertiary))
                        tintColor(pal().c(pal().textPrimary))
                        backgroundColor(Color.TRANSPARENT)
                    }
                    event {
                        textDidChange { params -> host.aiDraft = params.text }
                        inputReturn { host.sendAiDraft() }
                    }
                }
            }
            View { attr { width(10f) } }
            View {
                attr {
                    width(38f); height(38f); borderRadius(9f); allCenter()
                    backgroundColor(pal().c(pal().surfaceSecondary))
                    highlightBackgroundColor(pal().ca(pal().textSecondary, 12))
                    cssClass("zn-nav zn-click")
                }
                event { click { host.sendAiDraft() } }
                Icon(IconKind.SEND, 16f, pal().textPrimary)
            }
        }
    }
}

private fun ViewContainer<*, *>.drawerMetric(label: String, value: String, pal: Palette) {
    View {
        attr { flex(1f); marginRight(10f) }
        Text {
            attr {
                fontSize(11f)
                color(pal.c(pal.textTertiary))
                text(label)
            }
        }
        Text {
            attr {
                marginTop(4f)
                fontSize(14f); fontWeightMedium()
                color(pal.c(pal.textPrimary))
                text(value)
            }
        }
    }
}

private fun ViewContainer<*, *>.drawerBlock(pal: Palette, title: String, body: String) {
    View {
        attr { padding(left = 18f, right = 18f) }
        Text {
            attr {
                fontSize(12f); fontWeightSemiBold()
                color(pal.c(pal.textPrimary))
                text(title)
            }
        }
        Text {
            attr {
                marginTop(6f)
                fontSize(12f); lineHeight(19f)
                color(pal.c(pal.textSecondary))
                text(body)
            }
        }
    }
}
