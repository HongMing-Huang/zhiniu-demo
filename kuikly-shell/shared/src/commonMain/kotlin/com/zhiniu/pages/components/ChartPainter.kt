// 知牛 · K 线 / 分时 主图绘制（Kuikly Canvas 纯绘制层）
// 蜡烛 + 影线 + MA5/10/20 + 成交量 + X/Y 轴 + 十字光标；颜色来自当前 Palette。
package com.zhiniu.pages.components

import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.TextAlign
import com.zhiniu.domain.model.KLineBar

/** 主图：价格区 + 成交量区 + 网格 + 轴 + 十字光标。 */
fun drawKLineChart(
    context: CanvasContext,
    w: Float, h: Float,
    bars: List<KLineBar>,
    pal: Palette,
    crossX: Float, crossY: Float,   // 视图坐标；<0 表示无十字光标
) {
    if (bars.size < 2 || w < 40f || h < 40f) return

    val leftPad = 36f
    val rightPad = 12f
    val volH = (h * 0.20f).coerceIn(36f, 90f)
    val gap = 14f
    val priceTop = 6f
    val priceBottom = h - volH - gap - 6f

    // ----- 坐标 -----
    val hi = bars.maxOf { it.high }
    val lo = bars.minOf { it.low }
    val span = (hi - lo).coerceAtLeast(0.01)
    val padTop = span * 0.06
    val chartTop = hi + padTop
    val chartBottom = lo - padTop
    val chartSpan = (chartTop - chartBottom).coerceAtLeast(0.01)
    val plotW = w - leftPad - rightPad
    val slot = plotW / bars.size

    fun yOf(v: Double): Float = priceTop + ((chartTop - v) / chartSpan * (priceBottom - priceTop)).toFloat()
    fun xOf(i: Int): Float = leftPad + slot * (i + 0.5f)

    // ----- 网格 + Y 轴（4 条）-----
    val gridColor = pal.c(pal.chartGrid)
    val axisColor = pal.c(pal.axisText)
    for (i in 0..4) {
        val t = i / 4f
        val y = priceTop + (priceBottom - priceTop) * t
        context.beginPath()
        context.strokeStyle(gridColor)
        context.lineWidth(1f)
        context.moveTo(leftPad, y)
        context.lineTo(w - rightPad, y)
        context.stroke()
        val price = chartTop - chartSpan * t
        context.font(9f)
        context.textAlign(TextAlign.LEFT)
        context.fillStyle(axisColor)
        context.fillText(fmt2(price), 2f, y + 3f)
    }
    // 成交量区网格（1 条）
    val volTop = priceBottom + gap
    context.beginPath()
    context.strokeStyle(gridColor)
    context.moveTo(leftPad, volTop)
    context.lineTo(w - rightPad, volTop)
    context.stroke()

    // ----- X 轴时间标签（首 / 1/3 / 2/3 / 末）-----
    val n = bars.size
    context.font(9f)
    context.textAlign(TextAlign.CENTER)
    context.fillStyle(axisColor)
    listOf(0, n / 3, n * 2 / 3, n - 1).distinct().forEach { i ->
        val x = xOf(i).coerceIn(leftPad, w - rightPad)
        context.fillText(bars[i].day, x, h - 4f)
    }

    // ----- MA 均线 -----
    val ma5 = sma(bars.map { it.close }, 5)
    val ma10 = sma(bars.map { it.close }, 10)
    val ma20 = sma(bars.map { it.close }, 20)
    drawMaLine(context, ma5, pal.c(pal.ma5), ::xOf, ::yOf)
    drawMaLine(context, ma10, pal.c(pal.ma10), ::xOf, ::yOf)
    drawMaLine(context, ma20, pal.c(pal.ma20), ::xOf, ::yOf)

    // ----- 蜡烛 -----
    bars.forEachIndexed { i, b ->
        val x = xOf(i)
        val up = b.close >= b.open
        val color = pal.c(if (up) pal.up else pal.down)
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
        val bh = (b.volume.toDouble() / maxVol * (h - volTop - 10f)).toFloat()
        val x = xOf(i)
        val bw = (slot * 0.58f).coerceIn(1.5f, 13f)
        context.fillStyle(if (up) pal.ca(pal.up, 72) else pal.ca(pal.down, 72))
        context.beginPath()
        context.moveTo(x - bw / 2f, h - 8f)
        context.lineTo(x + bw / 2f, h - 8f)
        context.lineTo(x + bw / 2f, h - 8f - bh)
        context.lineTo(x - bw / 2f, h - 8f - bh)
        context.closePath()
        context.fill()
    }

    // ----- 十字光标 -----
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
        // 价格标签（左侧）
        val priceAt = chartTop - (cy - priceTop) / (priceBottom - priceTop) * chartSpan
        context.fillStyle(pal.c(pal.elevated))
        context.beginPath()
        context.moveTo(0f, cy - 8f)
        context.lineTo(leftPad, cy - 8f)
        context.lineTo(leftPad, cy + 8f)
        context.lineTo(0f, cy + 8f)
        context.closePath()
        context.fill()
        context.fillStyle(pal.c(pal.textPrimary))
        context.font(9f)
        context.textAlign(TextAlign.LEFT)
        context.fillText(fmt2(priceAt), 3f, cy + 3f)
        // 时间标签（底部）
        val idx = ((cx - leftPad) / slot).toInt().coerceIn(0, n - 1)
        if (crossY < priceBottom + 2f || true) {
            val label = bars[idx].day
            context.font(9f)
            context.textAlign(TextAlign.CENTER)
            context.fillStyle(pal.c(pal.elevated))
            val tx = xOf(idx).coerceIn(leftPad + 30f, w - rightPad - 30f)
            context.beginPath()
            context.moveTo(tx - 27f, h - 18f)
            context.lineTo(tx + 27f, h - 18f)
            context.lineTo(tx + 27f, h - 2f)
            context.lineTo(tx - 27f, h - 2f)
            context.closePath()
            context.fill()
            context.fillStyle(pal.c(pal.textPrimary))
            context.fillText(label, tx, h - 7f)
        }
    }
}

/** 迷你走势图（概览卡片 / 表格行内）。 */
fun drawSparkLine(
    context: CanvasContext, values: List<Double>, color: com.tencent.kuikly.core.base.Color,
    w: Float, h: Float, endDot: Boolean = true,
) {
    if (values.size < 2 || w < 10f || h < 6f) return
    val mn = values.minOrNull() ?: return
    val mx = values.maxOrNull() ?: return
    val span = (mx - mn).coerceAtLeast(1e-9)
    val n = values.size
    context.beginPath()
    context.strokeStyle(color)
    context.lineWidth(1.4f)
    context.lineCapRound()
    values.forEachIndexed { i, v ->
        val x = (i.toFloat() / (n - 1)) * w
        val y = h - ((v - mn) / span * h).toFloat()
        if (i == 0) context.moveTo(x, y) else context.lineTo(x, y)
    }
    context.stroke()
    if (endDot) {
        val ly = h - ((values.last() - mn) / span * h).toFloat()
        context.beginPath()
        context.fillStyle(color)
        context.arc(w, ly, 1.6f, 0f, PI2, false)
        context.fill()
    }
}

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

private fun drawMaLine(
    context: CanvasContext, pts: List<Double?>, color: com.tencent.kuikly.core.base.Color,
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

private const val PI2 = kotlin.math.PI.toFloat() * 2f
