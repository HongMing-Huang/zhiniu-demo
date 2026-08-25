// 知牛 · 迷你走势图绘制（Kuikly Canvas 纯绘制层）
package com.zhiniu.pages.components

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.views.CanvasContext

/** 迷你走势图（概览卡片 / 表格行内）。 */
fun drawSparkLine(
    context: CanvasContext, values: List<Double>, color: Color,
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

private const val PI2 = kotlin.math.PI.toFloat() * 2f
