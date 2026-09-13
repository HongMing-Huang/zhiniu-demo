// 知牛 · 股票搜索浮层（StockSearchOverlay，全局入口）
// 桌面：520 下拉面板；手机：全屏搜索页（输入 + 取消 + 热门榜单 + 最近 + 结果带价格）。
// 点击结果进入 StockDetailPage（Task2「聊天结果 → 详情承接」同路径）。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtSymbol

/**
 * 全局股票搜索浮层。
 * @param left 桌面浮层左坐标（手机全屏布局忽略）。
 * @param compact 手机布局（全屏搜索页）。
 */
fun ViewContainer<*, *>.StockSearchOverlay(
    visible: () -> Boolean,
    query: () -> String,
    recent: () -> ObservableList<StockQuote>,
    hot: () -> ObservableList<StockQuote>,
    results: () -> ObservableList<StockQuote>,
    left: Float,
    width: Float = 520f,
    topInset: Float = 0f,
    compact: Boolean = false,
    onQueryChange: (String) -> Unit,
    onPick: (StockQuote) -> Unit,
    onClose: () -> Unit,
) {
    if (compact) {
        FullscreenSearch(visible, query, recent, hot, results, topInset, onQueryChange, onPick, onClose)
        return
    }
    // 拦截
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(Color.TRANSPARENT)
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            animate(Animation.easeOut(0.16f), value = visible())
            zIndex(10)
        }
        event { click { onClose() } }
    }
    // 桌面下拉面板
    View {
        attr {
            absolutePosition(top = 72f + topInset, left = left)
            width(width)
            zIndex(15)
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (visible()) 0f else -4f))
            animate(Animation.easeOut(0.16f), value = visible())
        }
        View {
            attr {
                backgroundColor(AppTheme.colors.c(AppTheme.colors.elevated))
                border(Border(1f, BorderStyle.SOLID, AppTheme.colors.c(AppTheme.colors.borderStrong)))
                borderRadius(AppRadius.radius8)
                cssClass("zn-pop")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            SearchInputRow(query, onQueryChange, onClose)
            Divider()
            vif({ query().trim().isEmpty() }) {
                SectionLabel("最近")
                if (recent().isEmpty()) SectionLabel("热门")
                vfor({ recent() }) { q -> SearchRow(q, onClick = { onPick(q) }) }
                vfor({ hot() }) { q -> SearchRow(q, onClick = { onPick(q) }) }
            }
            velse {
                vif({ results().isEmpty() }) {
                    View {
                        attr { height(80f); allCenter() }
                        Text {
                            attr {
                                fontSize(AppTypography.fs13)
                                color(AppTheme.colors.c(AppTheme.colors.textTertiary))
                                text("未找到匹配的股票")
                            }
                        }
                    }
                }
                vif({ results().isNotEmpty() }) {
                    SectionLabel("搜索结果")
                    vfor({ results() }) { q -> SearchRow(q, onClick = { onPick(q) }) }
                }
            }
            View { attr { height(8f) } }
        }
    }
}

// ============== 手机全屏搜索页 ==============
private fun ViewContainer<*, *>.FullscreenSearch(
    visible: () -> Boolean,
    query: () -> String,
    recent: () -> ObservableList<StockQuote>,
    hot: () -> ObservableList<StockQuote>,
    results: () -> ObservableList<StockQuote>,
    topInset: Float,
    onQueryChange: (String) -> Unit,
    onPick: (StockQuote) -> Unit,
    onClose: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(colors.c(colors.pageBg))
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            zIndex(20)
            animate(Animation.easeOut(0.18f), value = visible())
        }
        // 顶部：胶囊输入框 + 取消
        View {
            attr {
                flexDirectionRow(); alignItemsCenter()
                paddingTop(topInset + 8f); paddingLeft(16f); paddingRight(16f); paddingBottom(10f)
                backgroundColor(colors.c(colors.surface))
                borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            View {
                attr {
                    flex(1f); height(38f); borderRadius(19f)
                    flexDirectionRow(); alignItemsCenter()
                    paddingLeft(12f); paddingRight(6f)
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                Icon(IconKind.SEARCH, 15f)
                View { attr { width(8f) } }
                Input {
                    attr {
                        flex(1f)
                        height(38f)
                        fontSize(AppTypography.fs14)
                        color(colors.c(colors.textPrimary))
                        placeholder("股票名称 / 代码 / 拼音")
                        placeholderColor(colors.c(colors.textTertiary))
                        tintColor(colors.c(colors.textPrimary))
                        backgroundColor(Color.TRANSPARENT)
                        accessibility("搜索股票")
                    }
                    event { textDidChange { params -> onQueryChange(params.text) } }
                }
                // 清空输入（有内容才显示）
                vif({ query().isNotEmpty() }) {
                    View {
                        attr {
                            width(28f); height(28f); allCenter()
                            accessibility("清空输入")
                            accessibilityRole(AccessibilityRole.BUTTON)
                            accessibilityInfo(clickable = true, longClickable = false)
                            cssClass("zn-click")
                            highlightBackgroundColor(colors.ca(colors.textSecondary, 8))
                        }
                        event { click { onQueryChange("") } }
                        Icon(IconKind.CLOSE, 13f)
                    }
                }
            }
            View { attr { width(10f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs14)
                    color(colors.c(colors.textSecondary))
                    text("取消")
                    accessibility("取消搜索")
                    accessibilityRole(AccessibilityRole.BUTTON)
                    accessibilityInfo(clickable = true, longClickable = false)
                }
                event { click { onClose() } }
            }
        }
        // 内容区
        List {
            attr { flex(1f) }
            vif({ query().trim().isEmpty() }) {
                // 热门榜单（榜单化呈现：序号着色，对标东财热搜）
                vif({ recent().isNotEmpty() }) {
                    SectionLabel("最近查看")
                    vfor({ recent() }) { q -> SearchRow(q, onClick = { onPick(q) }) }
                }
                SectionLabel("热门股票")
                vforIndex({ hot() }) { q, index, _ ->
                    RankSearchRow(q, index) { onPick(q) }
                }
                // 底部说明（数据来源诚实标注）
                View { attr { height(10f) } }
                View {
                    attr { paddingLeft(16f); paddingRight(16f) }
                    Text {
                        attr {
                            fontSize(AppTypography.fs11); lineHeight(16f)
                            color(colors.c(colors.textTertiary))
                            text("输入关键词联网搜索全市场（东方财富 suggest）；离线时匹配本地快照。")
                        }
                    }
                }
            }
            velse {
                vif({ results().isEmpty() }) {
                    View {
                        attr { height(160f); allCenter() }
                        Text {
                            attr {
                                fontSize(AppTypography.fs13)
                                color(colors.c(colors.textTertiary))
                                text("未找到匹配的股票\n换个名称 / 代码 / 拼音试试")
                            }
                        }
                    }
                }
                vif({ results().isNotEmpty() }) {
                    SectionLabel("搜索结果")
                    vfor({ results() }) { q -> SearchRow(q, onClick = { onPick(q) }) }
                }
            }
            View { attr { height(24f) } }
        }
    }
}

// ============== 共用组件 ==============
private fun ViewContainer<*, *>.SearchInputRow(
    query: () -> String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 12f); height(54f)
        }
        Icon(IconKind.SEARCH, 16f)
        View { attr { width(10f) } }
        Input {
            attr {
                flex(1f)
                height(54f)
                fontSize(AppTypography.fs14)
                color(AppTheme.colors.c(AppTheme.colors.textPrimary))
                placeholder("搜索股票 / 名称 / 代码")
                placeholderColor(AppTheme.colors.c(AppTheme.colors.textTertiary))
                tintColor(AppTheme.colors.c(AppTheme.colors.textPrimary))
                backgroundColor(Color.TRANSPARENT)
            }
            event { textDidChange { params -> onQueryChange(params.text) } }
        }
        View { attr { width(8f) } }
        IconButton(IconKind.CLOSE, size = 15f, box = 32f, onClick = onClose)
    }
}

private fun ViewContainer<*, *>.SectionLabel(label: String) {
    Text {
        attr {
            marginLeft(16f); marginTop(14f); marginBottom(4f)
            fontSize(AppTypography.fs12); fontWeightMedium()
            color(AppTheme.colors.c(AppTheme.colors.textSecondary))
            text(label)
        }
    }
}

private fun ViewContainer<*, *>.SearchRow(q: StockQuote, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(56f); flexDirectionRow(); alignItemsCenter()
            paddingLeft(16f); paddingRight(16f)
            cssClass("zn-row zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
            accessibility("${q.name} ${fmtSymbol(q.symbol)} 现价 ${fmt2(q.price)} ${fmtPct(q.changePercent)}")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
        }
        event { click { onClick() } }
        View {
            attr { flex(1f) }
            Text {
                attr {
                    fontSize(AppTypography.fs15); fontWeightMedium()
                    color(colors.c(colors.textPrimary)); text(q.name)
                }
            }
            View { attr { height(2f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textTertiary)); text(fmtSymbol(q.symbol))
                }
            }
        }
        Text {
            attr {
                width(84f)
                fontSize(AppTypography.fs15); fontWeightMedium()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(colors.c(colors.textPrimary))
                text(fmt2(q.price))
            }
        }
        View { attr { width(10f) } }
        Text {
            attr {
                width(72f)
                fontSize(AppTypography.fs14); fontWeightMedium()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(colors.c(if (q.isUp) colors.up else colors.down))
                text(fmtPct(q.changePercent))
            }
        }
    }
}

/** 榜单行：序号着色（1-3 热红 / 其余灰），列表页直出价格与涨跌幅。 */
private fun ViewContainer<*, *>.RankSearchRow(q: StockQuote, index: Int, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(56f); flexDirectionRow(); alignItemsCenter()
            paddingLeft(16f); paddingRight(16f)
            cssClass("zn-row zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
            accessibility("热门第${index + 1}名 ${q.name} 现价 ${fmt2(q.price)}")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
        }
        event { click { onClick() } }
        Text {
            attr {
                width(26f)
                fontSize(AppTypography.fs14)
                fontWeightMedium(); fontFamily(NUM_FONT)
                color(colors.c(if (index < 3) colors.up else colors.textTertiary))
                text("${index + 1}")
            }
        }
        View {
            attr { flex(1f) }
            Text {
                attr {
                    fontSize(AppTypography.fs15); fontWeightMedium()
                    color(colors.c(colors.textPrimary)); text(q.name)
                }
            }
            View { attr { height(2f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textTertiary)); text(fmtSymbol(q.symbol))
                }
            }
        }
        Text {
            attr {
                width(84f)
                fontSize(AppTypography.fs15); fontWeightMedium()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(colors.c(colors.textPrimary))
                text(fmt2(q.price))
            }
        }
        View { attr { width(10f) } }
        Text {
            attr {
                width(72f)
                fontSize(AppTypography.fs14); fontWeightMedium()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(colors.c(if (q.isUp) colors.up else colors.down))
                text(fmtPct(q.changePercent))
            }
        }
    }
}
