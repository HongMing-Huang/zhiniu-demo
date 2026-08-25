// 知牛 · 市场公共组件（components/market）
// StockRow / StockTable / MiniSparkline / MarketOverviewCard / StockSearchOverlay / FavoriteButton
package com.zhiniu.pages.components.market

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.drawSparkLine
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtAmount
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtSymbol

/** 迷你走势图（Canvas）。 */
fun ViewContainer<*, *>.MiniSparkline(values: List<Double>, isUp: Boolean, w: Float, h: Float) {
    Canvas({
        attr { width(w); height(h) }
    }) { context, cw, ch ->
        val colors = { AppTheme.colors }
        drawSparkLine(context, values, colors().c(if (isUp) colors().up else colors().down), cw, ch)
    }
}

/**
 * 行情表头列（百分比列宽由调用方以 flex 控制；此处保持文字对齐语义）。
 * @param width flex 权重
 */
fun ViewContainer<*, *>.ThCell(label: String, weight: Float, align: TextAlign) {
    val colors = { AppTheme.colors }
    View {
        attr { flex(weight) }
        Text {
            attr {
                fontSize(AppTypography.fs11)
                color(colors().c(colors().textTertiary))
                text(label)
                when (align) {
                    TextAlign.RIGHT -> textAlignRight()
                    TextAlign.CENTER -> textAlignCenter()
                    else -> textAlignLeft()
                }
            }
        }
    }
}

/**
 * 唯一 StockRow：64px，六列（股票/最新价/涨跌幅/今日走势/最高最低/成交额）。
 * 底部 1px 分隔线内嵌（满足 vfor 单一孩子约束）。
 * @param narrow true 时隐藏 最高/最低 与 成交额 列（<1280 响应式）
 */
fun ViewContainer<*, *>.StockRow(
    q: Quote,
    spark: List<Double>,
    narrow: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            height(64f)
            cssClass("zn-row zn-click")
            highlightBackgroundColor(colors().ca(colors().textSecondary, 7))
        }
        event { click { onClick() } }
        // 股票（24%）
        View {
            attr { flex(2f) }
            Text {
                attr {
                    fontSize(AppTypography.fs14); fontWeightMedium()
                    color(colors().c(colors().textPrimary))
                    text(q.name)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            Text {
                attr {
                    marginTop(3f)
                    fontSize(AppTypography.fs11)
                    color(colors().c(colors().textTertiary))
                    text(fmtSymbol(q.symbol))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        // 最新价（12%）
        ThCellValue(fmt2(q.price), 1f, TextAlign.RIGHT, { colors().textPrimary }, semibold = true)
        // 涨跌幅（12%）
        ThCellValue(fmtPct(q.changePercent), 1f, TextAlign.RIGHT, { if (q.isUp) colors().up else colors().down }, semibold = true)
        // 今日走势（18%）
        View {
            attr { flex(1.5f); allCenter() }
            MiniSparkline(spark, q.isUp, 96f, 28f)
        }
        if (!narrow) {
            // 最高 / 最低（18%）
            View {
                attr { flex(1.5f); flexDirectionRow(); alignItemsCenter() }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        fontFamily(NUM_FONT)
                        color(colors().c(colors().up))
                        text(fmt2(q.high))
                    }
                }
                Text {
                    attr {
                        marginLeft(4f); marginRight(4f)
                        fontSize(AppTypography.fs11)
                        color(colors().c(colors().textTertiary))
                        text("/")
                    }
                }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        fontFamily(NUM_FONT)
                        color(colors().c(colors().down))
                        text(fmt2(q.low))
                    }
                }
            }
            // 成交额（16%）
            ThCellValue(fmtAmount(q.amount), 1.3f, TextAlign.RIGHT, { colors().textSecondary })
        }
        // 行底分隔线
        View {
            attr {
                absolutePosition(top = 63f, left = 0f, right = 0f)
                height(1f)
                backgroundColor(colors().c(colors().border))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

private fun ViewContainer<*, *>.ThCellValue(
    value: String,
    weight: Float,
    align: TextAlign,
    colorHex: () -> String,
    semibold: Boolean = false,
) {
    val colors = { AppTheme.colors }
    View {
        attr { flex(weight) }
        Text {
            attr {
                fontSize(AppTypography.fs14)
                fontFamily(NUM_FONT)
                if (semibold) fontWeightSemiBold()
                color(colors().c(colorHex()))
                text(value)
                when (align) {
                    TextAlign.RIGHT -> textAlignRight()
                    TextAlign.CENTER -> textAlignCenter()
                    else -> textAlignLeft()
                }
            }
        }
    }
}

/** 表格头（与 StockRow 相同 flex 权重，保证列对齐）。 */
fun ViewContainer<*, *>.StockTableHeader(narrow: Boolean = false) {
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(32f) }
        ThCell("股票", 2f, TextAlign.LEFT)
        ThCell("最新价", 1f, TextAlign.RIGHT)
        ThCell("涨跌幅", 1f, TextAlign.RIGHT)
        ThCell("今日走势", 1.5f, TextAlign.CENTER)
        if (!narrow) {
            ThCell("最高 / 最低", 1.5f, TextAlign.LEFT)
            ThCell("成交额", 1.3f, TextAlign.RIGHT)
        }
    }
}
