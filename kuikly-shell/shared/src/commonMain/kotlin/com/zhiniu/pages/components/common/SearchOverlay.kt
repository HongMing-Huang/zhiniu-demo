// 知牛 · 股票搜索浮层（StockSearchOverlay，全局入口）
// 宽 520，160ms opacity+translateY；最近 + 搜索结果；点击结果进入 StockDetailPage。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.Input
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
 * @param left 浮层左坐标（由页面计算）。
 */
fun ViewContainer<*, *>.StockSearchOverlay(
    visible: () -> Boolean,
    query: () -> String,
    recent: () -> ObservableList<StockQuote>,
    hot: () -> ObservableList<StockQuote>,
    results: () -> ObservableList<StockQuote>,
    left: Float,
    onQueryChange: (String) -> Unit,
    onPick: (StockQuote) -> Unit,
    onClose: () -> Unit,
) {
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
    // 面板
    View {
        attr {
            absolutePosition(top = 72f, left = left)
            width(520f)
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
            // 输入
            View {
                attr {
                    flexDirectionRow(); alignItemsCenter()
                    padding(left = 14f, right = 12f); height(54f)
                    border(
                        Border(
                            0f, BorderStyle.SOLID, AppTheme.colors.c(AppTheme.colors.border),
                        )
                    )
                }
                Icon(IconKind.SEARCH, 16f) { AppTheme.colors.textTertiary }
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
            Divider()
            // 列表
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

private fun ViewContainer<*, *>.SectionLabel(label: String) {
    Text {
        attr {
            marginLeft(14f); marginTop(10f); marginBottom(4f)
            fontSize(AppTypography.fs11)
            color(AppTheme.colors.c(AppTheme.colors.textTertiary))
            text(label)
        }
    }
}

private fun ViewContainer<*, *>.SearchRow(q: StockQuote, onClick: () -> Unit) {
    View {
        attr {
            height(52f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
            cssClass("zn-row zn-click")
            highlightBackgroundColor(AppTheme.colors.ca(AppTheme.colors.textSecondary, 6))
        }
        event { click { onClick() } }
        Text {
            attr {
                width(160f)
                fontSize(AppTypography.fs14); fontWeightMedium()
                color(AppTheme.colors.c(AppTheme.colors.textPrimary))
                text(q.name)
            }
        }
        Text {
            attr {
                width(110f)
                fontSize(AppTypography.fs12)
                color(AppTheme.colors.c(AppTheme.colors.textTertiary))
                text(fmtSymbol(q.symbol))
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                width(80f)
                fontSize(AppTypography.fs13); fontWeightMedium()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(AppTheme.colors.c(AppTheme.colors.textPrimary))
                text(fmt2(q.price))
            }
        }
        Text {
            attr {
                width(72f)
                fontSize(AppTypography.fs13); fontWeightMedium()
                fontFamily(NUM_FONT)
                textAlignRight()
                color(AppTheme.colors.c(if (q.isUp) AppTheme.colors.up else AppTheme.colors.down))
                text(fmtPct(q.changePercent))
            }
        }
    }
}
