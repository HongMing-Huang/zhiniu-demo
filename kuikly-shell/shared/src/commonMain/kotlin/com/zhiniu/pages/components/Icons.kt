// 知牛 · 统一线性图标（Kuikly Canvas 自绘，无 Emoji、无图片资产、无外部依赖）
// 所有图标 24x24 设计网格，1.8px 圆头描边；颜色随主题（draw 回调内读 ThemeState，主题切换自动重绘）。
package com.zhiniu.pages.components

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext

enum class IconKind { SEARCH, STAR, SUN, MOON, MONITOR, SETTINGS, FILTER, ARROW_LEFT, ARROW_RIGHT,
    CHEVRON_DOWN, CLOSE, SPARKLES, SEND, MORE, CHECK, TREND }

/** 16/18/20px 线性图标组件。 */
fun ViewContainer<*, *>.Icon(
    kind: IconKind,
    size: Float = 18f,
    colorHex: () -> String? = { null },
    filled: Boolean = false,
) {
    Canvas({
        attr { width(size); height(size) }
    }) { context, w, h ->
        val pal = AppTheme.colors
        val color = Color(hexI(colorHex() ?: pal.textSecondary))
        drawIcon(context, kind, size, color, filled)
    }
}

/** 在 (x, y) ~ (x+size, y+size) 区域内绘制图标（供 canvas 内联使用）。 */
fun drawIcon(
    context: CanvasContext, kind: IconKind, size: Float, color: Color, filled: Boolean = false
) {
    context.save()
    context.translate(size / 2f, size / 2f)
    context.scale(size / 24f, size / 24f)
    context.strokeStyle(color)
    context.fillStyle(color)
    context.lineWidth(1.8f)
    context.lineCapRound()
    when (kind) {
        IconKind.SEARCH -> {
            context.beginPath()
            context.arc(11f, 11f, 7f, 0f, PI2, false)
            context.stroke()
            context.beginPath()
            context.moveTo(16.2f, 16.2f); context.lineTo(20.5f, 20.5f)
            context.stroke()
        }
        IconKind.STAR -> {
            val cx = 12f; val cy = 12.2f; var rOut = 9.2f; var rIn = 3.7f
            if (filled) { rOut = 9.4f; rIn = 3.9f }
            context.beginPath()
            for (i in 0 until 10) {
                val ang = (-90f + i * 36f) * DEG
                val r = if (i % 2 == 0) rOut else rIn
                val x = cx + cos(ang) * r
                val y = cy + sin(ang) * r
                if (i == 0) context.moveTo(x, y) else context.lineTo(x, y)
            }
            context.closePath()
            if (filled) context.fill() else context.stroke()
        }
        IconKind.SUN -> {
            context.beginPath()
            context.arc(12f, 12f, 4.6f, 0f, PI2, false)
            context.stroke()
            for (i in 0 until 8) {
                val ang = i * 45f * DEG
                val x1 = 12f + cos(ang) * 6.6f; val y1 = 12f + sin(ang) * 6.6f
                val x2 = 12f + cos(ang) * 9.4f; val y2 = 12f + sin(ang) * 9.4f
                context.beginPath()
                context.moveTo(x1, y1); context.lineTo(x2, y2)
                context.stroke()
            }
        }
        IconKind.MOON -> {
            // 左半圆 + 右侧鼓出弦线 = 月牙
            context.beginPath()
            context.arc(12f, 12f, 8.6f, (90f * DEG), (270f * DEG), false)
            context.quadraticCurveTo(17.6f, 12f, 12f, 20.6f)
            context.stroke()
        }
        IconKind.MONITOR -> {
            context.beginPath()
            context.moveTo(3.2f, 5.2f); context.lineTo(20.8f, 5.2f)
            context.lineTo(20.8f, 16.2f); context.lineTo(3.2f, 16.2f)
            context.closePath()
            context.stroke()
            context.beginPath()
            context.moveTo(8.6f, 21f); context.lineTo(15.4f, 21f)
            context.moveTo(12f, 16.4f); context.lineTo(12f, 21f)
            context.stroke()
        }
        IconKind.SETTINGS -> {
            context.beginPath()
            context.arc(12f, 12f, 3.4f, 0f, PI2, false)
            context.stroke()
            for (i in 0 until 8) {
                val ang = i * 45f * DEG
                val x1 = 12f + cos(ang) * 6.4f; val y1 = 12f + sin(ang) * 6.4f
                val x2 = 12f + cos(ang) * 9.3f; val y2 = 12f + sin(ang) * 9.3f
                context.beginPath()
                context.moveTo(x1, y1); context.lineTo(x2, y2)
                context.stroke()
            }
        }
        IconKind.FILTER -> {
            context.beginPath()
            context.moveTo(3.4f, 5f); context.lineTo(20.6f, 5f)
            context.lineTo(14.6f, 12.4f)
            context.lineTo(14.6f, 18.6f)
            context.lineTo(9.4f, 16.6f)
            context.lineTo(9.4f, 12.4f)
            context.closePath()
            context.stroke()
        }
        IconKind.ARROW_LEFT -> {
            context.beginPath()
            context.moveTo(19.4f, 12f); context.lineTo(4.6f, 12f)
            context.moveTo(10.8f, 5.8f); context.lineTo(4.6f, 12f); context.lineTo(10.8f, 18.2f)
            context.stroke()
        }
        IconKind.ARROW_RIGHT -> {
            context.beginPath()
            context.moveTo(4.6f, 12f); context.lineTo(19.4f, 12f)
            context.moveTo(13.2f, 5.8f); context.lineTo(19.4f, 12f); context.lineTo(13.2f, 18.2f)
            context.stroke()
        }
        IconKind.CHEVRON_DOWN -> {
            context.beginPath()
            context.moveTo(6.5f, 9.4f); context.lineTo(12f, 14.9f); context.lineTo(17.5f, 9.4f)
            context.stroke()
        }
        IconKind.CLOSE -> {
            context.beginPath()
            context.moveTo(6f, 6f); context.lineTo(18f, 18f)
            context.moveTo(18f, 6f); context.lineTo(6f, 18f)
            context.stroke()
        }
        IconKind.SPARKLES -> {
            context.beginPath()
            context.moveTo(12f, 3.6f); context.lineTo(13.5f, 9.4f); context.lineTo(19.2f, 11f)
            context.lineTo(13.5f, 12.6f); context.lineTo(12f, 18.4f); context.lineTo(10.5f, 12.6f)
            context.lineTo(4.8f, 11f); context.lineTo(10.5f, 9.4f)
            context.closePath()
            context.stroke()
            context.beginPath()
            context.moveTo(19f, 4f); context.lineTo(19.8f, 6.4f); context.lineTo(22f, 7.2f)
            context.lineTo(19.8f, 8f); context.lineTo(19f, 10.4f); context.lineTo(18.2f, 8f)
            context.lineTo(16f, 7.2f); context.lineTo(18.2f, 6.4f)
            context.closePath()
            context.stroke()
        }
        IconKind.SEND -> {
            context.beginPath()
            context.moveTo(21.4f, 2.6f); context.lineTo(10.8f, 13.2f)
            context.moveTo(21.4f, 2.6f); context.lineTo(14.2f, 21.4f); context.lineTo(10.8f, 13.2f)
            context.stroke()
        }
        IconKind.MORE -> {
            for (i in intArrayOf(5, 12, 19)) {
                context.beginPath()
                context.arc(i.toFloat(), 12f, 1.5f, 0f, PI2, false)
                context.fill()
            }
        }
        IconKind.CHECK -> {
            context.beginPath()
            context.moveTo(5f, 12.6f); context.lineTo(9.8f, 17.2f); context.lineTo(19f, 6.8f)
            context.stroke()
        }
        IconKind.TREND -> {
            context.beginPath()
            context.moveTo(3.4f, 17.4f); context.lineTo(9.2f, 11.4f); context.lineTo(13f, 15.2f); context.lineTo(20.6f, 7.4f)
            context.moveTo(15.2f, 7.4f); context.lineTo(20.6f, 7.4f); context.lineTo(20.6f, 12.8f)
            context.stroke()
        }
    }
    context.restore()
}

private const val PI2 = kotlin.math.PI.toFloat() * 2f
private const val DEG = (kotlin.math.PI / 180.0).toFloat()
private fun cos(a: Float): Float = kotlin.math.cos(a).toFloat()
private fun sin(a: Float): Float = kotlin.math.sin(a).toFloat()
