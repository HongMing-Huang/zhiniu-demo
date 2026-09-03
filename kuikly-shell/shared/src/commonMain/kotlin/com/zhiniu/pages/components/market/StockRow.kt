// 知牛 · StockTable + StockRow（首页视觉中心；列比 24/12/11/11/16/15/11%）
// 无外层 Card；行间 1px 分割线；hover surfaceHover 120ms；pressed surfaceSecondary。
package com.zhiniu.pages.components.market

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.drawSparkLine
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtAmount
import com.zhiniu.pages.components.fmtChangeSigned
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtSymbol

/**
 * 行情表：表头 + 股票行（vfor）。
 * 列：股票 flex(2.4) | 最新价 120 | 涨跌额 110 | 涨跌幅 110 | 今日走势 170 | 高/低 150 | 成交额 110。
 */
fun ViewContainer<*, *>.StockTable(
    marketQuotes: () -> ObservableList<StockQuote>,
    sparkOf: (StockQuote) -> List<Double>,
    narrow: Boolean,
    onRowClick: (StockQuote) -> Unit,
) {
    StockTableHeader(narrow)
    vfor({ marketQuotes() }) { q ->
        StockRow(q, sparkOf(q), narrow) { onRowClick(q) }
    }
}

/** 表头：42px / fs12 / textTertiary；点击表头排序由调用方通过外部 SortToolbar 控制。 */
fun ViewContainer<*, *>.StockTableHeader(narrow: Boolean) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(42f) }
        View { attr { flex(2.4f) }; ThLabel("股票", alignLeft = true) }
        View { attr { width(120f) }; ThLabel("最新价", alignLeft = false) }
        View { attr { width(110f) }; ThLabel("涨跌额", alignLeft = false) }
        View { attr { width(110f) }; ThLabel("涨跌幅", alignLeft = false) }
        View { attr { width(170f) }; ThLabel("今日走势", alignLeft = false) }
        if (!narrow) {
            View { attr { width(150f) }; ThLabel("高 / 低", alignLeft = false) }
            View { attr { width(110f) }; ThLabel("成交额", alignLeft = false) }
        }
    }
    View {
        attr {
            height(1f)
            backgroundColor(colors.c(colors.border))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}

private fun ViewContainer<*, *>.ThLabel(label: String, alignLeft: Boolean) {
    val colors = AppTheme.colors
    Text {
        attr {
            fontSize(AppTypography.fs12)
            color(colors.c(colors.textTertiary))
            text(label)
            if (!alignLeft) textAlignRight() else textAlignLeft()
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}

/** 股票行：64px / 仅底部 1px 分割线 / hover surfaceHover 120ms / pressed surfaceSecondary。 */
fun ViewContainer<*, *>.StockRow(
    q: StockQuote,
    spark: List<Double>,
    narrow: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            height(64f)
            backgroundColor(colors.c(colors.surface))
            highlightBackgroundColor(colors.c(colors.surfaceHover))
            
            cssClass("zn-row zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        // 股票（名称 + 代码）
        View {
            attr { flex(2.4f) }
            Text {
                attr {
                    fontSize(AppTypography.fs14); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary))
                    text(q.name)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            Text {
                attr {
                    marginTop(3f)
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textTertiary))
                    text(fmtSymbol(q.symbol))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        // 最新价
        NumCell(fmt2(q.price), 120f, colors.textPrimary, semibold = true)
        // 涨跌额
        NumCell(fmtChangeSigned(q.change), 110f, if (q.isUp) colors.up else colors.down)
        // 涨跌幅
        NumCell(fmtPct(q.changePercent), 110f, if (q.isUp) colors.up else colors.down, semibold = true)
        // 今日走势（84×28 sparkline）
        View {
            attr { width(170f); allCenter() }
            Canvas({
                attr { width(84f); height(28f) }
            }) { context, w, h ->
                drawSparkLine(
                    context, spark,
                    colors.c(if (q.isUp) colors.up else colors.down),
                    w, h,
                )
            }
        }
        if (!narrow) {
            NumCell(fmt2(q.high) + " / " + fmt2(q.low), 150f, colors.textSecondary)
            NumCell(fmtAmount(q.amount), 110f, colors.textSecondary)
        }
        // 行底分割线（满足 vfor 单一孩子约束）
        View {
            attr {
                absolutePosition(top = 63f, left = 0f, right = 0f)
                height(1f)
                backgroundColor(colors.c(colors.border))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

private fun ViewContainer<*, *>.NumCell(
    value: String, width: Float, colorHex: String, semibold: Boolean = false,
) {
    val colors = AppTheme.colors
    View {
        attr { width(width) }
        Text {
            attr {
                fontSize(AppTypography.fs14)
                fontFamily(NUM_FONT)
                if (semibold) fontWeightSemiBold()
                color(colors.c(colorHex))
                text(value)
                textAlignRight()
            }
        }
    }
}
