// 知牛 · K线工具条（ChartToolbar：左 周期 / 右 指标 · 不再使用官方 Tabs）
// 选中：14/600 Text Primary + 底部 2px 黑色 indicator（140ms）。
// 指标：小 toggle 28px，padding 0 10，background surfaceSecondary，radius 5。
package com.zhiniu.pages.components.chart

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.Candle
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.fmt2

val TIMEFRAMES = listOf("分时", "日K", "周K", "月K")
val INDICATORS = listOf("MA", "MACD", "RSI")

/**
 * 工具条：高度 40px；左 周期 Tab（指示条在底部），右 指标 toggle，中间 Divider。
 */
fun ViewContainer<*, *>.ChartToolbar(
    timeframe: () -> String,
    indicator: () -> String,
    bars: () -> List<Candle>,
    onTimeframe: (String) -> Unit,
    onIndicator: (String) -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            height(40f)
            padding(left = 12f, right = 12f)
            backgroundColor(colors.c(colors.surface))
            borderBottom(
                com.tencent.kuikly.core.base.Border(
                    1f,
                    com.tencent.kuikly.core.base.BorderStyle.SOLID,
                    colors.c(colors.border),
                )
            )
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // 左：周期
        TIMEFRAMES.forEach { tf ->
            ToolbarTab(label = tf, active = timeframe() == tf, onClick = { onTimeframe(tf) })
        }
        View { attr { flex(1f) } }
        // 中间 Divider
        View { attr { width(1f); height(20f); backgroundColor(colors.c(colors.border)) } }
        View { attr { width(14f) } }
        // 右：指标 toggle
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textTertiary))
                text("指标")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { width(8f) } }
        INDICATORS.forEach { ind ->
            IndicatorToggle(label = ind, active = indicator() == ind, onClick = { onIndicator(ind) })
        }
    }
    // MA 图例行
    vif({ indicator() == "MA" }) {
        val barsNow = bars()
        MaLegend("MA5", maValue(barsNow, 5), AppTheme.colors.ma5)
        MaLegend("MA10", maValue(barsNow, 10), AppTheme.colors.ma10)
        MaLegend("MA20", maValue(barsNow, 20), AppTheme.colors.ma20)
    }
}

private fun ViewContainer<*, *>.ToolbarTab(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(40f); padding(left = 12f, right = 12f)
            alignItemsCenter(); justifyContentCenter()
            cssClass("zn-click")
        }
        event { click { onClick() } }
        View {
            attr { flexDirectionColumn(); alignItemsCenter(); justifyContentCenter() }
            Text {
                attr {
                    fontSize(AppTypography.fs14)
                    fontWeight600()
                    color(colors.c(if (active) colors.textPrimary else colors.textSecondary))
                    text(label)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(4f) } }
            // 2px 黑色 indicator
            View {
                attr {
                    height(2f); width(16f)
                    borderRadius(allBorderRadius = 1f)
                    backgroundColor(colors.c(colors.textPrimary))
                    opacity(if (active) 1f else 0f)
                    animate(Animation.easeOut(0.14f), value = active)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.IndicatorToggle(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(28f); padding(left = 10f, right = 10f)
            borderRadius(AppRadius.radius5)
            backgroundColor(colors.c(if (active) colors.textPrimary else colors.surfaceSecondary))
            marginRight(6f)
            alignItemsCenter(); allCenter()
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 12))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                fontWeight600()
                color(colors.c(if (active) colors.surface else colors.textSecondary))
                text(label)
            }
        }
    }
}

private fun ViewContainer<*, *>.MaLegend(label: String, value: Double?, hex: String) {
    val colors = AppTheme.colors
    Text {
        attr {
            marginLeft(14f)
            fontSize(AppTypography.fs11)
            fontFamily(NUM_FONT)
            color(colors.c(hex))
            text(label + " " + (value?.let { fmt2(it) } ?: "--"))
        }
    }
}

fun maValue(bars: List<Candle>, n: Int): Double? {
    if (bars.size < n) return null
    var sum = 0.0
    for (i in bars.size - n until bars.size) sum += bars[i].close
    return sum / n
}

fun rsiNow(bars: List<Candle>): Double? {
    val vals = rsi(bars.map { it.close })
    return vals.lastOrNull { it != null }
}
