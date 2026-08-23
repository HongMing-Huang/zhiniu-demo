/* 知牛 · 公共组件库（多用 Kuikly 内置组件，不自绘 SVG）
 *
 * 组件登记（硬约束 §3：每新增组件在此 +1 行）：
 * - CandlestickChart.kt   K 线 Canvas 绘制（paint 数据；Canvas 签名待壳校准）
 * - SummaryCard.kt        AI 总结卡（摘要 + 0-100 评分色条）
 * - SignalPillGroup.kt    AI 信号标签组（≤6 Pill，红涨绿跌）
 * - RiskBadgeGroup.kt     AI 风险徽章组（HIGH红/MEDIUM橙/LOW黄 + 左边框色条）
 * - JumpCard.kt           AI 跳转卡（openPage 回调）
 * - AppErrorCard.kt       统一错误卡（⚠+错误码徽章+分级文案+重试）
 * - Skeleton.kt           骨架屏（SkeletonRow/Text/Kline）
 * - QuickChip.kt          快捷指令芯片（GHOST+16 圆角）
 */
package com.zhiniu.pages.components

import com.zhiniu.domain.model.AiInsight
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.Quote
import com.tencent.kuikly.ref.view.ViewBuilder

/** 涨跌颜色：A 股红涨绿跌。 */
object Palette {
    const val UP = "#e6432e"
    const val DOWN = "#09b76f"
    const val FLAT = "#6b7480"
    const val TEXT = "#2b2f36"
    const val SUB = "#8a919c"
    const val BG = "#ffffff"
    const val MA5 = "#f5a623"   // 均线5
    const val MA10 = "#4a90d9"  // 均线10
    const val MA20 = "#9b59b6"  // 均线20
}

/** 色值字符串 → Kuikly 颜色（渲染色对象以 SDK 模板为准）。 */
fun colorOf(hex: String): String = hex

/** 涨跌色。 */
fun priceColor(quote: Quote): String = when {
    quote.isUp -> Palette.UP
    quote.price < quote.prevClose -> Palette.DOWN
    else -> Palette.FLAT
}

/** 格式化涨跌幅（KMP 安全，见 domain/model/QuoteDisplay.kt）。 */
fun pricePct(quote: Quote): String = com.zhiniu.domain.model.QuoteDisplay.pct(quote.changePercent)

fun priceValue(quote: Quote): String = com.zhiniu.domain.model.QuoteDisplay.price(quote.price)

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