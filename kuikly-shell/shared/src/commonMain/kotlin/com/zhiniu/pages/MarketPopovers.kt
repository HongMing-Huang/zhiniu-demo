// 知牛 · 弹层（搜索 / 主题 / 设置 / 筛选）——全部在当前页面内，不跳页
package com.zhiniu.pages

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.base.ViewContainer
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.*

/** 全屏透明拦截层：任一弹层打开时拦截点击，点击任意处关闭。 */
internal fun ViewContainer<*, *>.backdropLayer(host: MarketShell) {
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(Color.TRANSPARENT)
            touchEnable((host.searchOpen || host.themeOpen || host.settingsOpen || host.filterOpen))
            opacity(if (host.searchOpen || host.themeOpen || host.settingsOpen || host.filterOpen) 1f else 0f)
            animate(Animation.easeOut(0.16f), value = (host.searchOpen || host.themeOpen || host.settingsOpen || host.filterOpen))
            zIndex(10)
        }
        event { click {
            host.closePopovers()
            host.aiOpen = false
        } }
    }
}

// ================= 搜索 =================
internal fun ViewContainer<*, *>.searchPopover(host: MarketShell) {
    View {
        attr {
            absolutePosition(top = 68f, left = host.popoverLeft(460f))
            width(460f)
            zIndex(15)
            touchEnable(host.searchOpen)
            opacity(if (host.searchOpen) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (host.searchOpen) 0f else -4f))
            animate(Animation.easeOut(0.16f), value = host.searchOpen)
        }
        View {
            attr {
                backgroundColor(ThemeState.palette.c(ThemeState.palette.elevated))
                border(Border(1f, BorderStyle.SOLID, ThemeState.palette.c(ThemeState.palette.borderStrong)))
                borderRadius(10f)
                cssClass("zn-pop")
                animate(ANIM_THEME, value = ThemeState.isDark)
            }
            // 输入行
            View {
                attr {
                    height(52f); flexDirectionRow(); alignItemsCenter()
                    padding(left = 14f, right = 8f)
                }
                View {
                    attr {
                        flex(1f); height(36f); borderRadius(8f)
                        backgroundColor(ThemeState.palette.c(ThemeState.palette.surfaceSecondary))
                        overflow(true)
                        padding(left = 12f, right = 12f)
                        animate(ANIM_THEME, value = ThemeState.isDark)
                    }
                    Input {
                        attr {
                            height(36f)
                            fontSize(13f)
                            color(ThemeState.palette.c(ThemeState.palette.textPrimary))
                            placeholder("搜索股票 / 代码")
                            placeholderColor(ThemeState.palette.c(ThemeState.palette.textTertiary))
                            tintColor(ThemeState.palette.c(ThemeState.palette.textPrimary))
                            backgroundColor(Color.TRANSPARENT)
                        }
                        event {
                            textDidChange { params ->
                                host.queryText = params.text
                                host.refreshSearchResults()
                            }
                            inputReturn { host.openFromSearch() }
                        }
                    }
                }
                View { attr { width(4f) } }
                IconButton(IconKind.CLOSE, 15f, onClick = { host.searchOpen = false })
            }
            View {
                attr { height(1f); backgroundColor(ThemeState.palette.c(ThemeState.palette.border)) }
            }
            // 内容
            View {
                attr { padding(top = 10f, bottom = 10f) }
                vif({ host.queryText.trim().isEmpty() }) {
                    vif({ !host.recentSearch.isEmpty() }) {
                        popSectionLabel("最近")
                        vfor({ host.recentSearch }) { sym ->
                            host.quoteOf(sym)?.let { row -> searchRow(host, row) }
                        }
                        View { attr { height(4f) } }
                    }
                    popSectionLabel("热门")
                    host.hotStocks().forEach { row -> searchRow(host, row) }
                }
                velse {
                    vif({ host.searchResults.isEmpty() }) {
                        View {
                            attr { height(80f); allCenter() }
                            Text {
                                attr {
                                    fontSize(13f)
                                    color(ThemeState.palette.c(ThemeState.palette.textTertiary))
                                    text("未找到匹配的股票")
                                }
                            }
                        }
                    }
                    vif({ !host.searchResults.isEmpty() }) {
                        popSectionLabel("搜索结果")
                        vfor({ host.searchResults }) { row -> searchRow(host, row) }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.popSectionLabel(label: String) {
    Text {
        attr {
            marginLeft(14f); marginTop(8f); marginBottom(4f)
            fontSize(11f)
            color(ThemeState.palette.c(ThemeState.palette.textTertiary))
            text(label)
        }
    }
}

private fun ViewContainer<*, *>.searchRow(host: MarketShell, q: Quote) {
    View {
        attr {
            height(46f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
            cssClass("zn-row zn-click")
        }
        event { click {
            host.rememberSearch(q.symbol)
            host.queryText = ""
            host.searchOpen = false
            host.openDetail(q.symbol, host.section)
        } }
        Text {
            attr {
                width(180f)
                fontSize(14f); fontWeightMedium()
                color(ThemeState.palette.c(ThemeState.palette.textPrimary))
                text(q.name)
            }
        }
        Text {
            attr {
                width(110f)
                fontSize(12f)
                color(ThemeState.palette.c(ThemeState.palette.textTertiary))
                text(fmtSymbol(q.symbol))
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                width(90f)
                fontSize(13f); fontWeightMedium()
                color(ThemeState.palette.c(ThemeState.palette.textPrimary))
                text(fmt2(q.price))
                fontFamily(NUM_FONT)
                textAlignRight()
            }
        }
        Text {
            attr {
                width(76f)
                fontSize(13f); fontWeightMedium()
                color(ThemeState.palette.c(if (q.isUp) ThemeState.palette.up else ThemeState.palette.down))
                text(fmtPct(q.changePercent))
                fontFamily(NUM_FONT)
                textAlignRight()
            }
        }
        View {
            attr {
                absolutePosition(top = 45f, left = 14f, right = 14f)
                height(1f)
                backgroundColor(ThemeState.palette.c(ThemeState.palette.border))
            }
        }
    }
}

// ================= 主题 / 设置 =================
internal fun ViewContainer<*, *>.themePopover(host: MarketShell) {
    View {
        attr {
            absolutePosition(top = 66f, left = host.popoverLeft(228f))
            width(228f)
            zIndex(15)
            touchEnable(host.themeOpen)
            opacity(if (host.themeOpen) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (host.themeOpen) 0f else -4f))
            animate(Animation.easeOut(0.16f), value = host.themeOpen)
        }
        popPanel(228f) {
            Text {
                attr {
                    marginLeft(14f); marginTop(10f)
                    fontSize(12f); fontWeightSemiBold()
                    color(ThemeState.palette.c(ThemeState.palette.textPrimary))
                    text("外观")
                }
            }
            View { attr { height(6f) } }
            themeOption(host, ThemeMode.SYSTEM, IconKind.MONITOR)
            themeOption(host, ThemeMode.LIGHT, IconKind.SUN)
            themeOption(host, ThemeMode.DARK, IconKind.MOON)
            View { attr { height(8f) } }
        }
    }
}

internal fun ViewContainer<*, *>.settingsPopover(host: MarketShell) {
    View {
        attr {
            absolutePosition(top = 66f, left = host.popoverLeft(280f))
            width(280f)
            zIndex(15)
            touchEnable(host.settingsOpen)
            opacity(if (host.settingsOpen) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (host.settingsOpen) 0f else -4f))
            animate(Animation.easeOut(0.16f), value = host.settingsOpen)
        }
        popPanel(280f) {
            Text {
                attr {
                    marginLeft(14f); marginTop(10f)
                    fontSize(12f); fontWeightSemiBold()
                    color(ThemeState.palette.c(ThemeState.palette.textPrimary))
                    text("设置")
                }
            }
            View { attr { height(6f) } }
            themeOption(host, ThemeMode.SYSTEM, null)
            themeOption(host, ThemeMode.LIGHT, null)
            themeOption(host, ThemeMode.DARK, null)
            View { attr { height(6f) } }
            View {
                attr {
                    marginLeft(14f); marginRight(14f)
                    height(1f)
                    backgroundColor(ThemeState.palette.c(ThemeState.palette.border))
                }
            }
            View { attr { height(6f) } }
            settingsRow("行情数据", "Demo · 演示数据")
            settingsRow("版本", "1.0.0")
            View { attr { height(8f) } }
        }
    }
}

private fun ViewContainer<*, *>.popPanel(width: Float, content: ViewContainer<*, *>.() -> Unit) {
    View {
        attr {
            width(width)
            backgroundColor(ThemeState.palette.c(ThemeState.palette.elevated))
            border(Border(1f, BorderStyle.SOLID, ThemeState.palette.c(ThemeState.palette.borderStrong)))
            borderRadius(10f)
            cssClass("zn-pop")
            animate(ANIM_THEME, value = ThemeState.isDark)
        }
        View { attr { padding(bottom = 6f) } }
        content()
    }
}

private fun ViewContainer<*, *>.themeOption(host: MarketShell, mode: ThemeMode, icon: IconKind?) {
    View {
        attr {
            height(40f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
            cssClass("zn-row zn-click")
        }
        event { click {
            ThemeState.setMode(mode)
            host.closePopovers()
        } }
        if (icon != null) {
            Icon(icon, 15f, ThemeState.palette.textSecondary)
            View { attr { width(10f) } }
        }
        Text {
            attr {
                fontSize(13f)
                color(ThemeState.palette.c(if (ThemeState.mode == mode) ThemeState.palette.textPrimary else ThemeState.palette.textSecondary))
                text(mode.label)
            }
        }
        View { attr { flex(1f) } }
        vif({ ThemeState.mode == mode }) { Icon(IconKind.CHECK, 14f, ThemeState.palette.textPrimary) }
    }
}

private fun ViewContainer<*, *>.settingsRow(label: String, value: String) {
    View {
        attr {
            height(36f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
        }
        Text {
            attr {
                fontSize(13f)
                color(ThemeState.palette.c(ThemeState.palette.textSecondary))
                text(label)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(12f)
                color(ThemeState.palette.c(ThemeState.palette.textTertiary))
                text(value)
            }
        }
    }
}

// ================= 筛选 =================
internal fun ViewContainer<*, *>.filterPopover(host: MarketShell) {
    View {
        attr {
            absolutePosition(top = 68f, left = host.popoverLeft(300f))
            width(300f)
            zIndex(15)
            touchEnable(host.filterOpen)
            opacity(if (host.filterOpen) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (host.filterOpen) 0f else -4f))
            animate(Animation.easeOut(0.16f), value = host.filterOpen)
        }
        popPanel(300f) {
            View { attr { height(6f) } }
            filterGroup(host, "涨跌", listOf("全部", "上涨", "下跌"), host.filterDir) { v -> host.filterDir = v; host.refreshMarketRows() }
            filterGroup(host, "涨跌幅", listOf("不限", "≥5%", "≤-5%"), host.filterGain) { v -> host.filterGain = v; host.refreshMarketRows() }
            filterGroup(host, "成交额", listOf("不限", "≥20亿", "≥50亿"), host.filterAmount) { v -> host.filterAmount = v; host.refreshMarketRows() }
            View { attr { height(2f) } }
            View {
                attr {
                    marginLeft(14f); marginRight(14f); marginBottom(10f)
                    height(32f); borderRadius(8f); allCenter()
                    backgroundColor(ThemeState.palette.c(ThemeState.palette.surfaceSecondary))
                    highlightBackgroundColor(ThemeState.palette.ca(ThemeState.palette.textSecondary, 10))
                    cssClass("zn-nav zn-click")
                }
                event { click {
                    host.filterDir = "全部"
                    host.filterGain = "不限"
                    host.filterAmount = "不限"
                    host.refreshMarketRows()
                } }
                Text {
                    attr {
                        fontSize(12f)
                        color(ThemeState.palette.c(ThemeState.palette.textSecondary))
                        text("重置筛选")
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.filterGroup(host: MarketShell, group: String, options: List<String>, current: String, onPick: (String) -> Unit) {
    Text {
        attr {
            marginLeft(14f); marginTop(10f)
            fontSize(11f)
            color(ThemeState.palette.c(ThemeState.palette.textTertiary))
            text(group)
        }
    }
    View {
        attr { flexDirectionRow(); marginLeft(14f); marginRight(14f); marginTop(6f) }
        options.forEach { opt ->
            View {
                attr {
                    height(28f)
                    borderRadius(7f)
                    marginRight(8f)
                    padding(left = 12f, right = 12f)
                    allCenter()
                    backgroundColor(ThemeState.palette.c(if (opt == current) ThemeState.palette.surfaceHover else ThemeState.palette.surfaceSecondary))
                    border(Border(1f, BorderStyle.SOLID, ThemeState.palette.c(if (opt == current) ThemeState.palette.borderStrong else ThemeState.palette.border)))
                    cssClass("zn-nav zn-click")
                    animate(ANIM_THEME, value = ThemeState.isDark)
                }
                event { click { onPick(opt) } }
                Text {
                    attr {
                        fontSize(12f)
                        color(ThemeState.palette.c(if (opt == current) ThemeState.palette.textPrimary else ThemeState.palette.textSecondary))
                        text(opt)
                    }
                }
            }
        }
    }
}
