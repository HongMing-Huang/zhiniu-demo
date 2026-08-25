// 知牛 · K线图表组件（components/chart）
// 主图蜡烛 + MA5/10/20 + 成交量 + 可选 MACD/RSI 副图 + 十字光标 OHLC 信息。
package com.zhiniu.pages.components.chart

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.TextAlign
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.pages.components.AppColors
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtVolHand

/** 指标模式。 */
enum class ChartIndicator { NONE, MA, MACD, RSI }

/** K线周期。 */
enum class ChartTimeframe(val label: String) {
    MIN5("5分"), MIN15("15分"), MIN30("30分"), MIN60("60分"),
    INTRADAY("分时"), DAY("日K"), WEEK("周K"), MONTH("月K"),
}

/**
 * K线主图：价格区 + 成交量区 + 指标区（MA 无副图）。
 * @param crossX/crossY 视图坐标；<0 表示无十字光标。
 */
fun drawKLineChart(
    context: CanvasContext,
    w: Float, h: Float,
    bars: List<KLineBar>,
    colors: AppColors,
    indicator: ChartIndicator,
    crossX: Float, crossY: Float,
) {
    if (bars.size < 2 || w < 60f || h < 60f) return

    val leftPad = 40f
    val rightPad = 12f
    val hasIndicator = indicator == ChartIndicator.MACD || indicator == ChartIndicator.RSI
    val volH = (h * 0.14f).coerceIn(34f, 70f)
    val indH = if (hasIndicator) (h * 0.20f).coerceIn(60f, 110f) else 0f
    val gap = 12f
    val priceTop = 8f
    val priceBottom = h - volH - indH - gap * (if (hasIndicator) 2 else 1) - 6f

    // ----- 坐标 -----
    val hi = bars.maxOf { it.high }
    val lo = bars.minOf { it.low }
    val span = (hi - lo).coerceAtLeast(0.01)
    val chartTop = hi + span * 0.06
    val chartBottom = lo - span * 0.06
    val chartSpan = (chartTop - chartBottom).coerceAtLeast(0.01)
    val plotW = w - leftPad - rightPad
    val slot = plotW / bars.size
    val n = bars.size

    fun yOf(v: Double): Float = priceTop + ((chartTop - v) / chartSpan * (priceBottom - priceTop)).toFloat()
    fun xOf(i: Int): Float = leftPad + slot * (i + 0.5f)

    val gridColor = colors.c(colors.chartGrid)
    val axisColor = colors.c(colors.axisText)

    // ----- 价格网格 + Y 轴（5 条） -----
    for (i in 0..4) {
        val t = i / 4f
        val y = priceTop + (priceBottom - priceTop) * t
        context.beginPath()
        context.strokeStyle(gridColor)
        context.lineWidth(1f)
        context.moveTo(leftPad, y)
        context.lineTo(w - rightPad, y)
        context.stroke()
        context.font(9f)
        context.textAlign(TextAlign.LEFT)
        context.fillStyle(axisColor)
        context.fillText(fmt2(chartTop - chartSpan * t), 2f, y + 3f)
    }
    // 成交量区上沿
    val volTop = priceBottom + gap
    context.beginPath()
    context.strokeStyle(gridColor)
    context.moveTo(leftPad, volTop)
    context.lineTo(w - rightPad, volTop)
    context.stroke()

    // ----- X 轴时间标签（首 / 1/3 / 2/3 / 末） -----
    context.font(9f)
    context.textAlign(TextAlign.CENTER)
    context.fillStyle(axisColor)
    listOf(0, n / 3, n * 2 / 3, n - 1).distinct().forEach { i ->
        context.fillText(bars[i].day, xOf(i).coerceIn(leftPad, w - rightPad), h - 5f)
    }

    // ----- MA 均线（主图） -----
    if (indicator == ChartIndicator.MA || indicator == ChartIndicator.NONE) {
        drawMaLine(context, sma(bars.map { it.close }, 5), colors.c(colors.ma5), { xOf(it) }, { yOf(it) })
        drawMaLine(context, sma(bars.map { it.close }, 10), colors.c(colors.ma10), { xOf(it) }, { yOf(it) })
        drawMaLine(context, sma(bars.map { it.close }, 20), colors.c(colors.ma20), { xOf(it) }, { yOf(it) })
    }

    // ----- 蜡烛 -----
    bars.forEachIndexed { i, b ->
        val x = xOf(i)
        val up = b.close >= b.open
        val color = colors.c(if (up) colors.up else colors.down)
        context.strokeStyle(color)
        context.lineWidth(1f)
        context.beginPath()
        context.moveTo(x, yOf(b.high))
        context.lineTo(x, yOf(b.low))
        context.stroke()
        val top = yOf(maxOf(b.open, b.close))
        val bottom = yOf(minOf(b.open, b.close))
        val bw = (slot * 0.58f).coerceIn(1.5f, 13f)
        context.beginPath()
        context.fillStyle(color)
        context.moveTo(x - bw / 2f, top)
        context.lineTo(x + bw / 2f, top)
        context.lineTo(x + bw / 2f, bottom)
        context.lineTo(x - bw / 2f, bottom)
        context.closePath()
        context.fill()
    }

    // ----- 成交量 -----
    val maxVol = bars.maxOf { it.volume }.coerceAtLeast(1)
    bars.forEachIndexed { i, b ->
        val up = b.close >= b.open
        val bh = (b.volume.toDouble() / maxVol * (h - volTop - 12f)).toFloat()
        val x = xOf(i)
        val bw = (slot * 0.58f).coerceIn(1.5f, 13f)
        context.fillStyle(if (up) colors.ca(colors.up, 66) else colors.ca(colors.down, 66))
        context.beginPath()
        context.moveTo(x - bw / 2f, h - 8f)
        context.lineTo(x + bw / 2f, h - 8f)
        context.lineTo(x + bw / 2f, h - 8f - bh)
        context.lineTo(x - bw / 2f, h - 8f - bh)
        context.closePath()
        context.fill()
    }

    // ----- MACD / RSI 副图 -----
    if (indicator == ChartIndicator.MACD && hasIndicator) {
        drawMacdPane(context, bars, w, leftPad, rightPad, h, indH, colors, volTop + gap, { xOf(it) }, slot)
    } else if (indicator == ChartIndicator.RSI && hasIndicator) {
        drawRsiPane(context, bars, w, leftPad, rightPad, h, indH, colors, volTop + gap, { xOf(it) })
    }

    // ----- 十字光标 + OHLC 信息 -----
    if (crossX >= 0f && crossY >= 0f && crossX >= leftPad && crossX <= w - rightPad) {
        val cx = crossX
        val cy = crossY.coerceIn(priceTop, priceBottom)
        context.setLineDash(listOf(4f, 4f))
        context.strokeStyle(axisColor)
        context.lineWidth(1f)
        context.beginPath()
        context.moveTo(cx, priceTop)
        context.lineTo(cx, priceBottom)
        context.stroke()
        context.beginPath()
        context.moveTo(leftPad, cy)
        context.lineTo(w - rightPad, cy)
        context.stroke()
        context.setLineDash(emptyList())

        val priceAt = chartTop - (cy - priceTop) / (priceBottom - priceTop) * chartSpan
        // 价格标签（左）
        context.fillStyle(colors.c(colors.elevated))
        context.beginPath()
        context.moveTo(0f, cy - 8f)
        context.lineTo(leftPad, cy - 8f)
        context.lineTo(leftPad, cy + 8f)
        context.lineTo(0f, cy + 8f)
        context.closePath()
        context.fill()
        context.fillStyle(colors.c(colors.textPrimary))
        context.font(9f)
        context.textAlign(TextAlign.LEFT)
        context.fillText(fmt2(priceAt), 3f, cy + 3f)

        // OHLC 信息框（顶部）
        val idx = ((cx - leftPad) / slot).toInt().coerceIn(0, n - 1)
        val b = bars[idx]
        val infoLines = listOf(
            b.day,
            "开 ${fmt2(b.open)}",
            "高 ${fmt2(b.high)}",
            "低 ${fmt2(b.low)}",
            "收 ${fmt2(b.close)}",
            "涨跌 ${fmt2((b.close - b.open) / b.open * 100.0)}%",
            "量 ${fmtVolHand(b.volume)}",
        )
        val boxW = 78f
        val boxH = infoLines.size * 12f + 10f
        val boxX = (cx - boxW - 12f).coerceAtLeast(leftPad + 4f)
        context.fillStyle(colors.ca(colors.chartBg, 88))
        context.beginPath()
        context.moveTo(boxX, priceTop + 4f)
        context.lineTo(boxX + boxW, priceTop + 4f)
        context.lineTo(boxX + boxW, priceTop + 4f + boxH)
        context.lineTo(boxX, priceTop + 4f + boxH)
        context.closePath()
        context.fill()
        context.fillStyle(colors.c(colors.textPrimary))
        context.font(9f)
        context.textAlign(TextAlign.LEFT)
        infoLines.forEachIndexed { i, line ->
            context.fillText(line, boxX + 6f, priceTop + 4f + 14f + i * 12f)
        }
    }
}

// ---------- 指标计算 ----------
private fun sma(values: List<Double>, n: Int): List<Double?> {
    val out = ArrayList<Double?>(values.size)
    var sum = 0.0
    for (i in values.indices) {
        sum += values[i]
        if (i >= n) sum -= values[i - n]
        out.add(if (i >= n - 1) sum / n else null)
    }
    return out
}

private fun ema(values: List<Double>, n: Int): List<Double> {
    val k = 2.0 / (n + 1)
    val out = ArrayList<Double>(values.size)
    var prev = values[0]
    out.add(prev)
    for (i in 1 until values.size) {
        prev = values[i] * k + prev * (1 - k)
        out.add(prev)
    }
    return out
}

/** MACD(12,26,9) → (dif, dea, hist)。 */
fun macd(values: List<Double>): Triple<List<Double>, List<Double>, List<Double>> {
    if (values.size < 26) return Triple(emptyList(), emptyList(), emptyList())
    val e12 = ema(values, 12)
    val e26 = ema(values, 26)
    val dif = (0 until values.size).map { e12[it] - e26[it] }
    val dea = ema(dif, 9)
    val hist = (0 until values.size).map { (dif[it] - dea[it]) * 2 }
    return Triple(dif, dea, hist)
}

/** RSI(14)（Wilder 平滑）。 */
fun rsi(values: List<Double>, n: Int = 14): List<Double?> {
    if (values.size <= n) return List(values.size) { null }
    val out = ArrayList<Double?>(values.size)
    var gain = 0.0
    var loss = 0.0
    for (i in 1..n) {
        val d = values[i] - values[i - 1]
        if (d >= 0) gain += d else loss -= d
    }
    gain /= n; loss /= n
    out.add(if (loss == 0.0) 100.0 else 100.0 - 100.0 / (1 + gain / loss))
    for (i in n + 1 until values.size) {
        val d = values[i] - values[i - 1]
        gain = (gain * (n - 1) + (if (d >= 0) d else 0.0)) / n
        loss = (loss * (n - 1) + (if (d < 0) -d else 0.0)) / n
        out.add(if (loss == 0.0) 100.0 else 100.0 - 100.0 / (1 + gain / loss))
    }
    // 前面补 null
    val padded = ArrayList<Double?>(values.size)
    repeat(n) { padded.add(null) }
    padded.addAll(out)
    return padded
}

private fun drawMaLine(
    context: CanvasContext, pts: List<Double?>, color: Color,
    xOf: (Int) -> Float, yOf: (Double) -> Float,
) {
    var started = false
    context.beginPath()
    context.strokeStyle(color)
    context.lineWidth(1.1f)
    pts.forEachIndexed { i, v ->
        if (v == null) return@forEachIndexed
        val y = yOf(v)
        if (!started) { context.moveTo(xOf(i), y); started = true } else context.lineTo(xOf(i), y)
    }
    if (started) context.stroke()
}

private fun drawMacdPane(
    context: CanvasContext, bars: List<KLineBar>,
    w: Float, leftPad: Float, rightPad: Float, h: Float, indH: Float,
    colors: AppColors, top: Float, xOf: (Int) -> Float, slot: Float,
) {
    val (dif, dea, hist) = macd(bars.map { it.close })
    if (dif.isEmpty()) return
    val maxAbs = (hist.maxOf { kotlin.math.abs(it) } * 1.15).coerceAtLeast(0.001)
    val paneTop = top + 4f
    val paneBottom = top + indH - 10f

    fun y(v: Double): Float = paneTop + ((maxAbs - v) / (maxAbs * 2) * (paneBottom - paneTop)).toFloat()

    val gridColor = colors.c(colors.chartGrid)
    // 零线
    context.beginPath()
    context.strokeStyle(gridColor)
    context.moveTo(leftPad, y(0.0))
    context.lineTo(w - rightPad, y(0.0))
    context.stroke()
    // 柱
    bars.forEachIndexed { i, _ ->
        val v = hist[i]
        val x = xOf(i)
        val bw = (slot * 0.5f).coerceIn(1f, 8f)
        val yv = y(v)
        context.fillStyle(colors.ca(if (v >= 0) colors.up else colors.down, 60))
        context.beginPath()
        context.moveTo(x - bw / 2f, yv)
        context.lineTo(x + bw / 2f, yv)
        context.lineTo(x + bw / 2f, y(0.0))
        context.lineTo(x - bw / 2f, y(0.0))
        context.closePath()
        context.fill()
    }
    // DIF / DEA
    drawPaneLine(context, dif, colors.c(colors.dif), { xOf(it) }, { y(it) }, bars.size)
    drawPaneLine(context, dea, colors.c(colors.dea), { xOf(it) }, { y(it) }, bars.size)
    context.fillStyle(colors.c(colors.axisText))
    context.font(9f)
    context.textAlign(TextAlign.LEFT)
    context.fillText("MACD(12,26,9)", leftPad + 2f, paneBottom + 8f)
}

private fun drawRsiPane(
    context: CanvasContext, bars: List<KLineBar>,
    w: Float, leftPad: Float, rightPad: Float, h: Float, indH: Float,
    colors: AppColors, top: Float, xOf: (Int) -> Float,
) {
    val vals = rsi(bars.map { it.close })
    val paneTop = top + 4f
    val paneBottom = top + indH - 10f

    fun y(v: Double): Float = paneTop + ((100.0 - v) / 100.0 * (paneBottom - paneTop)).toFloat()

    val gridColor = colors.c(colors.chartGrid)
    val axisColor = colors.c(colors.axisText)
    for (lv in listOf(30.0, 70.0)) {
        context.beginPath()
        context.strokeStyle(colors.ca(colors.axisText, 40))
        context.moveTo(leftPad, y(lv))
        context.lineTo(w - rightPad, y(lv))
        context.stroke()
    }
    context.beginPath()
    context.strokeStyle(gridColor)
    context.moveTo(leftPad, y(50.0))
    context.lineTo(w - rightPad, y(50.0))
    context.stroke()

    var started = false
    context.beginPath()
    context.strokeStyle(colors.c(colors.rsi))
    context.lineWidth(1.2f)
    vals.forEachIndexed { i, v ->
        if (v == null) return@forEachIndexed
        if (!started) { context.moveTo(xOf(i), y(v)); started = true } else context.lineTo(xOf(i), y(v))
    }
    if (started) context.stroke()

    context.fillStyle(axisColor)
    context.font(9f)
    context.textAlign(TextAlign.LEFT)
    context.fillText("RSI(14)", leftPad + 2f, paneBottom + 8f)
}

private fun drawPaneLine(
    context: CanvasContext, vals: List<Double>, color: Color,
    xOf: (Int) -> Float, yOf: (Double) -> Float, size: Int,
) {
    if (vals.size < 2) return
    var started = false
    context.beginPath()
    context.strokeStyle(color)
    context.lineWidth(1.1f)
    vals.forEachIndexed { i, v ->
        val y = yOf(v)
        if (!started) { context.moveTo(xOf(i), y); started = true } else context.lineTo(xOf(i), y)
    }
    if (started) context.stroke()
}
