// 知牛 · 弹层（外观 / 设置 / 筛选 / 排序）——全部在当前页面内，不跳页
package com.zhiniu.pages

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.ThemeMode
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.common.AppPopover

// ================= 外观 =================
internal fun ViewContainer<*, *>.themePopover(host: MarketShell) {
    AppPopover(
        visible = { host.themeOpen },
        width = 220f,
        left = host.popoverLeft(220f),
        top = 66f,
    ) {
        Text {
            attr {
                marginLeft(14f); marginTop(10f)
                fontSize(AppTypography.fs12); fontWeightSemiBold()
                color(AppTheme.colors.c(AppTheme.colors.textPrimary))
                text("外观")
            }
        }
        View { attr { height(6f) } }
        themeOption(host, ThemeMode.SYSTEM)
        themeOption(host, ThemeMode.LIGHT)
        themeOption(host, ThemeMode.DARK)
    }
}

internal fun ViewContainer<*, *>.themeOption(host: MarketShell, mode: ThemeMode) {
    val colors = { AppTheme.colors }
    View {
        attr {
            height(36f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
            cssClass("zn-row zn-click")
        }
        event { click {
            AppTheme.setMode(mode)
            host.closePopovers()
        } }
        Text {
            attr {
                fontSize(AppTypography.fs13)
                color(colors().c(if (AppTheme.mode == mode) colors().textPrimary else colors().textSecondary))
                text(mode.label)
            }
        }
        View { attr { flex(1f) } }
        vif({ AppTheme.mode == mode }) { Icon(IconKind.CHECK, 14f, { colors().textPrimary }) }
    }
}

// ================= 设置（紧凑） =================
internal fun ViewContainer<*, *>.settingsPopover(host: MarketShell) {
    val colors = { AppTheme.colors }
    AppPopover(
        visible = { host.settingsOpen },
        width = 240f,
        left = host.popoverLeft(240f),
        top = 66f,
    ) {
        View { attr { height(2f) } }
        settingsRow(host, ThemeMode.SYSTEM)
        settingsRow(host, ThemeMode.LIGHT)
        settingsRow(host, ThemeMode.DARK)
        View { attr { height(4f) } }
        View {
            attr {
                marginLeft(14f); marginRight(14f)
                height(1f)
                backgroundColor(colors().c(colors().border))
            }
        }
        View { attr { height(4f) } }
        infoRow("行情数据", "Demo 行情")
        infoRow("版本", "1.0.0")
        View { attr { height(2f) } }
    }
}

internal fun ViewContainer<*, *>.settingsRow(host: MarketShell, mode: ThemeMode) {
    val colors = { AppTheme.colors }
    View {
        attr {
            height(30f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
            cssClass("zn-row zn-click")
        }
        event { click {
            AppTheme.setMode(mode)
            host.closePopovers()
        } }
        Text {
            attr {
                fontSize(AppTypography.fs13)
                color(colors().c(if (AppTheme.mode == mode) colors().textPrimary else colors().textSecondary))
                text(mode.label)
            }
        }
        View { attr { flex(1f) } }
        vif({ AppTheme.mode == mode }) { Icon(IconKind.CHECK, 13f, { colors().textPrimary }) }
    }
}

internal fun ViewContainer<*, *>.infoRow(label: String, value: String) {
    val colors = { AppTheme.colors }
    View {
        attr {
            height(26f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
        }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textTertiary))
                text(label)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textSecondary))
                text(value)
            }
        }
    }
}

// ================= 排序 =================
internal fun ViewContainer<*, *>.sortPopover(host: MarketShell) {
    val colors = { AppTheme.colors }
    val options = listOf("默认排序", "涨幅", "跌幅", "成交额", "换手率")
    AppPopover(
        visible = { host.sortOpen },
        width = 190f,
        left = host.popoverLeft(190f),
        top = 68f,
    ) {
        View { attr { height(4f) } }
        options.forEach { opt ->
            View {
                attr {
                    height(36f); flexDirectionRow(); alignItemsCenter()
                    padding(left = 14f, right = 14f)
                    cssClass("zn-row zn-click")
                }
                event { click {
                    host.sortBy = opt
                    host.sortOpen = false
                    host.refreshMarketRows()
                } }
                Text {
                    attr {
                        fontSize(AppTypography.fs13)
                        color(colors().c(if (host.sortBy == opt) colors().textPrimary else colors().textSecondary))
                        text(opt)
                    }
                }
                View { attr { flex(1f) } }
                vif({ host.sortBy == opt }) { Icon(IconKind.CHECK, 13f, { colors().textPrimary }) }
            }
        }
        View { attr { height(2f) } }
    }
}

// ================= 筛选 =================
internal fun ViewContainer<*, *>.filterPopover(host: MarketShell) {
    val colors = { AppTheme.colors }
    AppPopover(
        visible = { host.filterOpen },
        width = 300f,
        left = host.popoverLeft(300f),
        top = 68f,
    ) {
        View { attr { height(4f) } }
        filterGroup(host, "涨跌", listOf("全部", "上涨", "下跌"), host.filterDir) { v ->
            host.filterDir = v
            host.refreshMarketRows()
        }
        filterGroup(host, "涨跌幅", listOf("不限", "≥5%", "≤-5%"), host.filterGain) { v ->
            host.filterGain = v
            host.refreshMarketRows()
        }
        filterGroup(host, "成交额", listOf("不限", "≥20亿", "≥50亿"), host.filterAmount) { v ->
            host.filterAmount = v
            host.refreshMarketRows()
        }
        View { attr { height(2f) } }
        View {
            attr {
                marginLeft(14f); marginRight(14f); marginBottom(10f)
                height(32f); borderRadius(8f); allCenter()
                backgroundColor(colors().c(colors().surfaceSecondary))
                highlightBackgroundColor(colors().ca(colors().textSecondary, 10))
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
                    fontSize(AppTypography.fs12)
                    color(colors().c(colors().textSecondary))
                    text("重置筛选")
                }
            }
        }
    }
}

internal fun ViewContainer<*, *>.filterGroup(
    host: MarketShell,
    group: String,
    options: List<String>,
    current: String,
    onPick: (String) -> Unit,
) {
    val colors = { AppTheme.colors }
    Text {
        attr {
            marginLeft(14f); marginTop(10f)
            fontSize(AppTypography.fs11)
            color(colors().c(colors().textTertiary))
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
                    backgroundColor(colors().c(if (opt == current) colors().surfaceHover else colors().surfaceSecondary))
                    border(
                        Border(
                            1f, BorderStyle.SOLID,
                            colors().c(if (opt == current) colors().borderStrong else colors().border),
                        )
                    )
                    cssClass("zn-nav zn-click")
                }
                event { click { onPick(opt) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        color(colors().c(if (opt == current) colors().textPrimary else colors().textSecondary))
                        text(opt)
                    }
                }
            }
        }
    }
}
