/* 知牛 · 公共组件库（多用 Kuikly 内置组件，不自绘 SVG）
 *
 * 说明：以下使用 Kuikly 已核实的内置组件 View/Text/ScrollView 组合。
 * 动态列表更新遵循 kuiklyDSL.mdc 的 observable/vfor 语义；此处以命令式构建示意，
 * 具体函数签名以 Kuikly SDK 官方模板为准（不臆造未证实 API）。
 */
package com.zhiniu.pages.components

import com.zhiniu.domain.model.AiInsight
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote

/** 涨跌颜色：A 股红涨绿跌。 */
object Palette {
    const val UP = "#e6432e"
    const val DOWN = "#09b76f"
    const val FLAT = "#6b7480"
    const val TEXT = "#2b2f36"
    const val SUB = "#8a919c"
    const val BG = "#ffffff"
}

/** 涨跌色。 */
fun priceColor(quote: Quote): String = when {
    quote.isUp -> Palette.UP
    quote.price < quote.prevClose -> Palette.DOWN
    else -> Palette.FLAT
}

/** 格式化涨跌幅。 */
fun pricePct(quote: Quote): String =
    (if (quote.changePercent >= 0) "+" else "") +
        String.format(java.util.Locale.US, "%.2f%%", quote.changePercent)

fun priceValue(quote: Quote): String = String.format(java.util.Locale.US, "%.2f", quote.price)

/**
 * 迷你 K 线（用 View 柱状近似蜡烛，避免依赖 Canvas 绘制 API 的未证实部分）。
 * 输入 K 线，输出 24 根等宽柱，走 View 层级。
 */
object MiniCandles {
    fun heightFor(bar: KLineBar, min: Double, max: Double, chartHeight: Float): Float {
        val span = if (max - min <= 0) 1.0 else (max - min)
        val h = ((bar.close - min) / span * chartHeight).toFloat()
        return h.coerceIn(chartHeight * 0.1f, chartHeight * 0.9f)
    }
    fun scaleMax(bars: List<KLineBar>): Double = bars.maxOfOrNull { it.high } ?: 1.0
    fun scaleMin(bars: List<KLineBar>): Double = bars.minOfOrNull { it.low } ?: 0.0
}

/** 盘口五档行数据。 */
data class DepthLine(val price: Double, val volume: Long)

/** AI 结构化结论 → 展示所需映射（供页面渲染）。 */
data class AiCardUi(
    val summary: String,
    val score: Int,
    val trendLabel: String,
    val signals: List<String>,
    val risks: List<String>,
    val jump: String,
)

fun AiInsight.toUi(): AiCardUi = AiCardUi(
    summary = summary,
    score = score,
    trendLabel = mapOf(
        "UP" to "看多", "DOWN" to "看空", "SIDEWAYS" to "震荡",
    )[trend.name] ?: "震荡",
    signals = signals.map { "${it.type}: ${it.detail}" },
    risks = risks.map { "${it.type}·${it.level}" },
    jump = cards.firstOrNull { it.type == "JUMP" }?.title ?: "查看详情",
)