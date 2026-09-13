// 知牛 · Market Pulse（首页市场概览：指数 + 宽度，紧凑行情带）
// 设计：5 列等高 + 1px 竖分隔线 + 上下 1px 边线，92px 高；不做大 Card。
package com.zhiniu.pages.components.market

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.MarketBreadth
import com.zhiniu.domain.model.MarketIndex
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppSpacing
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.drawSparkLine
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtInt
import com.zhiniu.pages.components.fmtPct

/**
 * Market Pulse 紧凑行情带：高度 92px，1px 上下边线，5 列等高。
 * 列：3 指数 + 市场宽度 + 两市成交额；中间用 1px 竖线分隔。
 * @param narrow 窄屏 (<=1280) 隐藏"两市成交额"列。
 */
fun ViewContainer<*, *>.MarketPulse(
    indices: List<MarketIndex>,
    breadth: MarketBreadth,
    narrow: Boolean,
    compact: Boolean = false,
) {
    val colors = AppTheme.colors
    // 手机：三张指数卡（对标移动端行情 App 首页），市场宽度做第二行摘要
    if (compact) {
        View {
            attr {
                flexDirectionColumn()
                padding(top = 12f, bottom = 12f, left = 16f, right = 16f)
                backgroundColor(colors.c(colors.pageBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            View {
                attr { flexDirectionRow(); alignItemsStretch() }
                indices.forEachIndexed { i, idx ->
                    if (i > 0) View { attr { width(8f) } }
                    IndexCard(idx)
                }
            }
            View { attr { height(10f) } }
            CompactBreadthCard(breadth)
        }
        return
    }
    View {
        attr {
            height(AppSpacing.pulse)
            flexDirectionColumn(); alignItemsStretch()
            backgroundColor(colors.c(colors.surface))
            borderTop(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr { height(AppSpacing.pulse); flexDirectionRow(); alignItemsStretch() }
            indices.forEachIndexed { i, idx ->
                if (i > 0) VerticalDivider()
                IndexColumn(idx, compact)
            }
            VerticalDivider()
            BreadthColumn(breadth)
            if (!narrow) {
                VerticalDivider()
                AmountColumn(breadth)
            }
        }
    }
}

/** 手机指数卡：白底描边小卡（名称 / 大价格 / 涨跌 chip / 迷你走势）。 */
private fun ViewContainer<*, *>.IndexCard(idx: MarketIndex) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f)
            borderRadius(8f)
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            backgroundColor(colors.c(colors.surface))
            padding(top = 10f, bottom = 10f, left = 10f, right = 10f)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        Text {
            attr {
                fontSize(AppTypography.fs11)
                color(colors.c(colors.textSecondary)); text(idx.name)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(3f) } }
        Text {
            attr {
                fontSize(AppTypography.fs14); fontWeightSemiBold(); fontFamily(NUM_FONT)
                lines(1); textOverFlowClip()
                color(colors.c(if (idx.isUp) colors.up else colors.down)); text(fmt2(idx.price))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(3f) } }
        Text {
            attr {
                fontSize(AppTypography.fs11); fontWeightMedium(); fontFamily(NUM_FONT)
                color(colors.c(if (idx.isUp) colors.up else colors.down)); text(fmtPct(idx.changePercent))
            }
        }
        View { attr { height(5f) } }
        Canvas({ attr { width(72f); height(18f) } }) { context, w, h ->
            drawSparkLine(context, idx.spark, colors.c(if (idx.isUp) colors.up else colors.down), w, h)
        }
    }
}

/** 手机市场宽度摘要卡：上涨/下跌/成交，卡片容器与指数卡同语言。 */
private fun ViewContainer<*, *>.CompactBreadthCard(breadth: MarketBreadth) {
    val colors = AppTheme.colors
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            borderRadius(8f)
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            backgroundColor(colors.c(colors.surface))
            padding(top = 10f, bottom = 10f, left = 12f, right = 12f)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        Text { attr { fontSize(AppTypography.fs11); color(colors.c(colors.textSecondary)); text("市场宽度") } }
        View { attr { width(8f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13); fontWeightSemiBold(); fontFamily(NUM_FONT)
                color(colors.c(colors.up)); text(fmtInt(breadth.upCount) + " 涨")
            }
        }
        Text {
            attr {
                marginLeft(6f); fontSize(AppTypography.fs13); fontWeightSemiBold(); fontFamily(NUM_FONT)
                color(colors.c(colors.down)); text(fmtInt(breadth.downCount) + " 跌")
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs11); color(colors.c(colors.textTertiary))
                text("成交 " + fmtInt(breadth.amountYi) + " 亿")
            }
        }
    }
}

private fun ViewContainer<*, *>.VerticalDivider() {
    View {
        attr {
            width(1f)
            marginTop(14f); marginBottom(14f)
            backgroundColor(AppTheme.colors.c(AppTheme.colors.border))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}

private fun ViewContainer<*, *>.IndexColumn(idx: MarketIndex, compact: Boolean) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f)
            padding(top = 14f, left = if (compact) 10f else 22f, right = if (compact) 8f else 14f, bottom = 14f)
        }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textSecondary))
                text(idx.name)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(4f) } }
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            Text {
                attr {
                    fontSize(if (compact) AppTypography.fs14 else AppTypography.fs18); fontWeightSemiBold()
                    fontFamily(NUM_FONT)
                    color(colors.c(colors.textPrimary))
                    text(fmt2(idx.price))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { width(if (compact) 5f else 10f) } }
            Text {
                attr {
                    fontSize(if (compact) AppTypography.fs11 else AppTypography.fs13); fontWeightSemiBold()
                    fontFamily(NUM_FONT)
                    color(colors.c(if (idx.isUp) colors.up else colors.down))
                    text(fmtPct(idx.changePercent))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        View { attr { height(4f) } }
        // 22-26px 高 mini sparkline
        if (!compact) {
            Canvas({ attr { width(110f); height(24f) } }) { context, w, h ->
                drawSparkLine(context, idx.spark, colors.c(if (idx.isUp) colors.up else colors.down), w, h)
            }
        }
    }
}

private fun ViewContainer<*, *>.CompactBreadthRow(breadth: MarketBreadth) {
    val colors = AppTheme.colors
    View {
        attr {
            height(55f); flexDirectionRow(); alignItemsCenter()
            padding(left = 10f, right = 10f)
        }
        Text { attr { fontSize(AppTypography.fs12); color(colors.c(colors.textSecondary)); text("市场宽度") } }
        Text {
            attr {
                marginLeft(10f); fontSize(AppTypography.fs14); fontWeightSemiBold(); fontFamily(NUM_FONT)
                color(colors.c(colors.up)); text(fmtInt(breadth.upCount))
            }
        }
        Text { attr { marginLeft(4f); marginRight(4f); fontSize(AppTypography.fs12); color(colors.c(colors.textTertiary)); text("/") } }
        Text {
            attr {
                fontSize(AppTypography.fs14); fontWeightSemiBold(); fontFamily(NUM_FONT)
                color(colors.c(colors.down)); text(fmtInt(breadth.downCount))
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12); color(colors.c(colors.textSecondary))
                text("上涨 " + fmt2(breadth.upRatio * 100.0) + "% · " + breadth.status)
            }
        }
    }
}

private fun ViewContainer<*, *>.BreadthColumn(breadth: MarketBreadth) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1.05f)
            padding(top = 14f, left = 22f, right = 14f, bottom = 14f)
        }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textSecondary))
                text("上涨 / 下跌")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(4f) } }
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            Text {
                attr {
                    fontSize(AppTypography.fs18); fontWeightSemiBold()
                    fontFamily(NUM_FONT)
                    color(colors.c(colors.up))
                    text(fmtInt(breadth.upCount))
                }
            }
            Text {
                attr {
                    marginLeft(4f); marginRight(4f
                    )
                    fontSize(AppTypography.fs14)
                    color(colors.c(colors.textTertiary))
                    text("/")
                }
            }
            Text {
                attr {
                    fontSize(AppTypography.fs18); fontWeightSemiBold()
                    fontFamily(NUM_FONT)
                    color(colors.c(colors.down))
                    text(fmtInt(breadth.downCount))
                }
            }
            View { attr { width(10f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors.c(if (breadth.status == "偏强") colors.up else colors.textSecondary))
                    text(breadth.status)
                }
            }
        }
        View { attr { height(4f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textTertiary))
                text("上涨占比 " + fmt2(breadth.upRatio * 100.0) + "%")
            }
        }
    }
}

private fun ViewContainer<*, *>.AmountColumn(breadth: MarketBreadth) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(0.9f)
            padding(top = 14f, left = 22f, right = 22f, bottom = 14f)
        }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textSecondary))
                text("两市成交额")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(4f) } }
        Text {
            attr {
                fontSize(AppTypography.fs18); fontWeightSemiBold()
                fontFamily(NUM_FONT)
                color(colors.c(colors.textPrimary))
                text(fmtInt(breadth.amountYi) + " 亿")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}
