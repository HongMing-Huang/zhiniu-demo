// 知牛 · K线绘制（Kuikly Canvas，禁止第三方图表库）
// 坐标换算：xOf(v) / yOf(v) 在主图共用，volume + indicator 副图各自换算；
// 视口：viewStart..viewStart+viewCount 区间绘制 Candle（默认日K 80 根，可横向 Pan）。
// Crosshair：1px dashed（颜色 = colors.crosshair），固定左上角 OHLC Tooltip。
package com.zhiniu.pages.components.chart

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.TextAlign
import com.zhiniu.domain.model.Candle
import com.zhiniu.pages.components.AppColors
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtVolHand

enum class ChartIndicator { NONE, MA, MACD, RSI }

private const val MIN_VISIBLE = 40
private const val DEFAULT_VISIBLE = 80

/** 根据窗口大小自动决定可见 K 线数。 */
fun autoVisibleCount(total: Int): Int =
    if (total <= 0) 0 else minOf(DEFAULT_VISIBLE, total).coerceAtLeast(MIN_VISIBLE).coerceAtMost(total)

/** 将 viewStart 限制在合法范围。 */
fun clampViewStart(viewStart: Int, total: Int, viewCount: Int): Int =
    if (viewCount <= 0 || total <= 0) 0
    else viewStart.coerceIn(0, (total - viewCount).coerceAtLeast(0))

/**
 * K线主图 + 副图（Volume / MA-MACD / RSI）。
 * @param bars 全量数据（按时间升序）。
 * @param viewStart 可视起点索引（0..total-viewCount）。
 * @param viewCount 可视条数（默认 80）。
 * @param intraday true 时绘制分时折线（无蜡烛）。
 * @param crossX/crossY Crosshair 坐标（<0 表示无 Crosshair）。
 */
fun drawKLineChart(
    context: CanvasContext,
    w: Float, h: Float,
    bars: List<Candle>,
    colors: AppColors,
    indicator: ChartIndicator,
    crossX: Float, crossY: Float,
    intraday: Boolean = false,
    viewStart: Int = 0,
    viewCount: Int = autoVisibleCount(bars.size),
) {
    if (bars.size < 2 || w < 60f || h < 60f) return
    val vc = viewCount.coerceIn(MIN_VISIBLE, bars.size)
    val vs = clampViewStart(viewStart, bars.size, vc)
    val visBars = bars.subList(vs, vs + vc)

    val leftPad = 44f
    val rightPad = 14f
    val hasIndicator = !intraday && (indicator == ChartIndicator.MACD || indicator == ChartIndicator.RSI)
    val volH = (h * 0.18f).coerceIn(60f, 90f)
    val indH = if (hasIndicator) (h * 0.18f).coerceIn(50f, 100f) else 0f
    val gap1 = 10f
    val gap2 = 8f
    val priceTop = 6f
    val priceBottom = h - volH - indH - gap1 - (if (hasIndicator) gap2 else 0f) - 6f

    // 主图价格区间
    val hi = visBars.maxOf { it.high }
    val lo = visBars.minOf { it.low }
    val span = (hi - lo).coerceAtLeast(0.01)
    val chartTop = hi + span * 0.04
    val chartBottom = lo - span * 0.04
    val chartSpan = (chartTop - chartBottom).coerceAtLeast(0.01)
    val plotW = w - leftPad - rightPad
    val slot = plotW / visBars.size
    val n = visBars.size

    fun yOf(v: Double): Float = priceTop + ((chartTop - v) / chartSpan * (priceBottom - priceTop)).toFloat()
    fun xOf(i: Int): Float = leftPad + slot * (i + 0.5f)

    val gridColor = colors.c(colors.chartGrid)
    val axisColor = colors.c(colors.axisText)

    // 主图网格 + Y 轴
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
    val volTop = priceBottom + gap1
    context.beginPath()
    context.strokeStyle(gridColor)
    context.moveTo(leftPad, volTop)
    context.lineTo(w - rightPad, volTop)
    context.stroke()

    // X 轴时间标签（首 / 中 / 末）
    context.font(9f)
    context.textAlign(TextAlign.CENTER)
    context.fillStyle(axisColor)
    val axisIdx = when (n) {
        1 -> listOf(0)
        2 -> listOf(0, 1)
        else -> listOf(0, n / 2, n - 1)
    }
    axisIdx.forEach { i ->
        val label = visBars[i].day.ifBlank { "" }
        if (label.isNotEmpty()) {
            context.fillText(label, xOf(i).coerceIn(leftPad, w - rightPad), h - 5f)
        }
    }

    // 主图内容
    if (intraday) {
        drawIntradayLine(context, visBars, leftPad, rightPad, priceTop, priceBottom, { xOf(it) }, { yOf(it) }, colors)
    } else {
        // MA 叠加主图
        if (indicator == ChartIndicator.MA || indicator == ChartIndicator.NONE) {
            drawMaLine(context, sma(visBars.map { it.close }, 5), colors.c(colors.ma5), { xOf(it) }, { yOf(it) })
            drawMaLine(context, sma(visBars.map { it.close }, 10), colors.c(colors.ma10), { xOf(it) }, { yOf(it) })
            drawMaLine(context, sma(visBars.map { it.close }, 20), colors.c(colors.ma20), { xOf(it) }, { yOf(it) })
        }
        // 蜡烛
        val bw = (slot * 0.7f).coerceIn(4f, 10f)
        val gap = slot - bw
        visBars.forEachIndexed { i, b ->
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
            context.beginPath()
            context.fillStyle(color)
            context.moveTo(x - bw / 2f, top)
            context.lineTo(x + bw / 2f, top)
            context.lineTo(x + bw / 2f, bottom)
            context.lineTo(x - bw / 2f, bottom)
            context.closePath()
            context.fill()
        }
    }

    // 成交量副图
    val maxVol = visBars.maxOf { it.volume }.coerceAtLeast(1)
    val volBaseY = h - 6f
    val volW = (slot * 0.7f).coerceIn(4f, 10f)
    visBars.forEachIndexed { i, b ->
        val up = b.close >= b.open
        val bh = (b.volume.toDouble() / maxVol * (volBaseY - volTop - 6f)).toFloat()
        val x = xOf(i)
        context.fillStyle(if (up) colors.ca(colors.up, 55) else colors.ca(colors.down, 55))
        context.beginPath()
        context.moveTo(x - volW / 2f, volBaseY)
        context.lineTo(x + volW / 2f, volBaseY)
        context.lineTo(x + volW / 2f, volBaseY - bh)
        context.lineTo(x - volW / 2f, volBaseY - bh)
        context.closePath()
        context.fill()
    }

    // MACD / RSI 副图
    if (hasIndicator) {
        val indTop = volTop + gap2
        if (indicator == ChartIndicator.MACD) {
            drawMacdPane(context, visBars, w, leftPad, rightPad, h, indH, colors, indTop, { xOf(it) }, slot)
        } else {
            drawRsiPane(context, visBars, w, leftPad, rightPad, h, indH, colors, indTop, { xOf(it) })
        }
    }

    // 固定左上角 OHLC Tooltip
    val tipIdx = if (crossX >= leftPad && crossX <= w - rightPad) {
        ((crossX - leftPad) / slot).toInt().coerceIn(0, n - 1)
    } else n - 1
    val tipBar = visBars[tipIdx]
    val tipPrev = if (tipIdx > 0) visBars[tipIdx - 1].close else tipBar.open
    val tipPct = if (tipPrev != 0.0) (tipBar.close - tipPrev) / tipPrev * 100.0 else 0.0
    val tipColor = if (tipBar.close >= tipPrev) colors.up else colors.down
    val tipLines = listOf(
        tipBar.day to "label",
        "开 ${fmt2(tipBar.open)}" to "data",
        "高 ${fmt2(tipBar.high)}" to "data",
        "低 ${fmt2(tipBar.low)}" to "data",
        "收 ${fmt2(tipBar.close)}" to "data",
        "涨跌 ${fmt2(tipPct)}%" to "data",
        "量 ${fmtVolHand(tipBar.volume)}" to "data",
    )
    val padX = 10f
    val padY = 8f
    val lineH = 12f
    val boxW = 96f
    val boxH = (tipLines.size * lineH + padY * 2).coerceAtLeast(60f)
    val boxX = leftPad + 4f
    val boxY = priceTop + 4f
    context.fillStyle(colors.ca(colors.elevated, 95))
    context.beginPath()
    context.moveTo(boxX, boxY)
    context.lineTo(boxX + boxW, boxY)
    context.lineTo(boxX + boxW, boxY + boxH)
    context.lineTo(boxX, boxY + boxH)
    context.closePath()
    context.fill()
    context.font(9f)
    context.textAlign(TextAlign.LEFT)
    var cy = boxY + padY + 7f
    tipLines.forEachIndexed { i, (line, kind) ->
        if (kind == "label") {
            context.fillStyle(colors.c(colors.textTertiary))
        } else {
            context.fillStyle(colors.c(colors.textPrimary))
        }
        context.fillText(line, boxX + padX, cy)
        cy += lineH
    }
    // 价格标签（左轴）
    if (crossX >= leftPad && crossX <= w - rightPad) {
        val cy2 = crossY.coerceIn(priceTop, priceBottom)
        val priceAt = chartTop - (cy2 - priceTop) / (priceBottom - priceTop) * chartSpan
        context.fillStyle(colors.c(colors.elevated))
        context.beginPath()
        context.moveTo(0f, cy2 - 8f); context.lineTo(leftPad, cy2 - 8f)
        context.lineTo(leftPad, cy2 + 8f); context.lineTo(0f, cy2 + 8f)
        context.closePath(); context.fill()
        context.fillStyle(colors.c(colors.textPrimary))
        context.fillText(fmt2(priceAt), 3f, cy2 + 3f)
    }

    // 十字光标（虚线）
    if (crossX >= leftPad && crossX <= w - rightPad && crossY >= 0f) {
        val cx = crossX
        val cy3 = crossY.coerceIn(priceTop, priceBottom)
        context.setLineDash(listOf(4f, 4f))
        context.strokeStyle(colors.c(colors.crosshair))
        context.lineWidth(1f)
        context.beginPath()
        context.moveTo(cx, priceTop); context.lineTo(cx, priceBottom)
        context.stroke()
        context.beginPath()
        context.moveTo(leftPad, cy3); context.lineTo(w - rightPad, cy3)
        context.stroke()
        context.setLineDash(emptyList())
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
    var prev = values[0]; out.add(prev)
    for (i in 1 until values.size) { prev = values[i] * k + prev * (1 - k); out.add(prev) }
    return out
}

fun macd(values: List<Double>): Triple<List<Double>, List<Double>, List<Double>> {
    if (values.size < 26) return Triple(emptyList(), emptyList(), emptyList())
    val e12 = ema(values, 12); val e26 = ema(values, 26)
    val dif = (0 until values.size).map { e12[it] - e26[it] }
    val dea = ema(dif, 9)
    val hist = (0 until values.size).map { (dif[it] - dea[it]) * 2 }
    return Triple(dif, dea, hist)
}

fun rsi(values: List<Double>, n: Int = 14): List<Double?> {
    if (values.size <= n) return List(values.size) { null }
    val out = ArrayList<Double?>(values.size)
    var gain = 0.0; var loss = 0.0
    for (i in 1..n) { val d = values[i] - values[i - 1]; if (d >= 0) gain += d else loss -= d }
    gain /= n; loss /= n
    out.add(if (loss == 0.0) 100.0 else 100.0 - 100.0 / (1 + gain / loss))
    for (i in n + 1 until values.size) {
        val d = values[i] - values[i - 1]
        gain = (gain * (n - 1) + (if (d >= 0) d else 0.0)) / n
        loss = (loss * (n - 1) + (if (d < 0) -d else 0.0)) / n
        out.add(if (loss == 0.0) 100.0 else 100.0 - 100.0 / (1 + gain / loss))
    }
    val padded = ArrayList<Double?>(values.size)
    repeat(n) { padded.add(null) }; padded.addAll(out)
    return padded
}

private fun drawMaLine(
    context: CanvasContext, pts: List<Double?>, color: Color,
    xOf: (Int) -> Float, yOf: (Double) -> Float,
) {
    var started = false
    context.beginPath()
    context.strokeStyle(color); context.lineWidth(1.1f)
    pts.forEachIndexed { i, v ->
        if (v == null) return@forEachIndexed
        val y = yOf(v)
        if (!started) { context.moveTo(xOf(i), y); started = true } else context.lineTo(xOf(i), y)
    }
    if (started) context.stroke()
}

private fun drawMacdPane(
    context: CanvasContext, bars: List<Candle>,
    w: Float, leftPad: Float, rightPad: Float, h: Float, indH: Float,
    colors: AppColors, top: Float, xOf: (Int) -> Float, slot: Float,
) {
    val (dif, dea, hist) = macd(bars.map { it.close })
    if (dif.isEmpty()) return
    val maxAbs = (hist.maxOf { kotlin.math.abs(it) } * 1.15).coerceAtLeast(0.001)
    val paneTop = top + 4f
    val paneBottom = top + indH - 10f
    fun y(v: Double): Float = paneTop + ((maxAbs - v) / (maxAbs * 2) * (paneBottom - paneTop)).toFloat()
    context.beginPath()
    context.strokeStyle(colors.c(colors.chartGrid))
    context.moveTo(leftPad, y(0.0)); context.lineTo(w - rightPad, y(0.0))
    context.stroke()
    bars.forEachIndexed { i, _ ->
        val v = hist[i]; val x = xOf(i); val bw = (slot * 0.7f).coerceIn(4f, 10f)
        context.fillStyle(colors.ca(if (v >= 0) colors.up else colors.down, 55))
        context.beginPath()
        context.moveTo(x - bw / 2f, y(v)); context.lineTo(x + bw / 2f, y(v))
        context.lineTo(x + bw / 2f, y(0.0)); context.lineTo(x - bw / 2f, y(0.0))
        context.closePath(); context.fill()
    }
    drawPaneLine(context, dif, colors.c(colors.dif), { xOf(it) }, { y(it) }, bars.size)
    drawPaneLine(context, dea, colors.c(colors.dea), { xOf(it) }, { y(it) }, bars.size)
    context.fillStyle(colors.c(colors.axisText))
    context.font(9f); context.textAlign(TextAlign.LEFT)
    context.fillText("MACD(12,26,9)", leftPad + 2f, paneBottom + 8f)
}

private fun drawRsiPane(
    context: CanvasContext, bars: List<Candle>,
    w: Float, leftPad: Float, rightPad: Float, h: Float, indH: Float,
    colors: AppColors, top: Float, xOf: (Int) -> Float,
) {
    val vals = rsi(bars.map { it.close })
    val paneTop = top + 4f
    val paneBottom = top + indH - 10f
    fun y(v: Double): Float = paneTop + ((100.0 - v) / 100.0 * (paneBottom - paneTop)).toFloat()
    for (lv in listOf(30.0, 70.0)) {
        context.beginPath()
        context.strokeStyle(colors.ca(colors.axisText, 35))
        context.moveTo(leftPad, y(lv)); context.lineTo(w - rightPad, y(lv))
        context.stroke()
    }
    context.beginPath()
    context.strokeStyle(colors.c(colors.chartGrid))
    context.moveTo(leftPad, y(50.0)); context.lineTo(w - rightPad, y(50.0))
    context.stroke()
    var started = false
    context.beginPath(); context.strokeStyle(colors.c(colors.rsi)); context.lineWidth(1.2f)
    vals.forEachIndexed { i, v ->
        if (v == null) return@forEachIndexed
        if (!started) { context.moveTo(xOf(i), y(v)); started = true } else context.lineTo(xOf(i), y(v))
    }
    if (started) context.stroke()
    context.fillStyle(colors.c(colors.axisText))
    context.font(9f); context.textAlign(TextAlign.LEFT)
    context.fillText("RSI(14)", leftPad + 2f, paneBottom + 8f)
}

private fun drawPaneLine(
    context: CanvasContext, vals: List<Double>, color: Color,
    xOf: (Int) -> Float, yOf: (Double) -> Float, size: Int,
) {
    if (vals.size < 2) return
    var started = false
    context.beginPath(); context.strokeStyle(color); context.lineWidth(1.1f)
    vals.forEachIndexed { i, v ->
        val y = yOf(v)
        if (!started) { context.moveTo(xOf(i), y); started = true } else context.lineTo(xOf(i), y)
    }
    if (started) context.stroke()
}

/** 分时折线 + 渐变面积。 */
private fun drawIntradayLine(
    context: CanvasContext, bars: List<Candle>,
    leftPad: Float, rightPad: Float, priceTop: Float, priceBottom: Float,
    xOf: (Int) -> Float, yOf: (Double) -> Float, colors: AppColors,
) {
    val lineColor = colors.c(colors.textPrimary)
    context.beginPath(); context.strokeStyle(lineColor); context.lineWidth(1.4f)
    bars.forEachIndexed { i, b ->
        if (i == 0) context.moveTo(xOf(i), yOf(b.close)) else context.lineTo(xOf(i), yOf(b.close))
    }
    context.stroke()
    context.beginPath()
    bars.forEachIndexed { i, b ->
        if (i == 0) context.moveTo(xOf(i), yOf(b.close)) else context.lineTo(xOf(i), yOf(b.close))
    }
    context.lineTo(xOf(bars.size - 1), priceBottom); context.lineTo(xOf(0), priceBottom)
    context.closePath()
    context.fillStyle(colors.ca(colors.textPrimary, 6)); context.fill()
}
