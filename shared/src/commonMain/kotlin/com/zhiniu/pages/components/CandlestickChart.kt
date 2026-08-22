/* 知牛 · K 线图表组件（Kuikly 内置 Canvas 自绘，D5 决策落地；硬约束 §2：不用 SVG/View 柱状近似）
 *
 * // Example: candlestickChart(vm.kline.value, widthPx = 360f, heightPx = 240f, showMA = true)
 * // Example: candlestickChart(miniBars, widthPx = 120f, heightPx = 60f, showMA = false) // 迷你走势
 *
 * 数据/几何由 domain/model/KLineChart.buildBundle 计算（蜡烛与均线共享 y 缩放，已单测）。
 * 本组件只做「几何 + 颜色 → Canvas 绘制」的落地翻译。
 * ⚠️ Canvas 绘制方法签名以 Kuikly SDK 官方模板为准；此处按 Compose Canvas 语义书写并在接入壳工程时校准。
 */
package com.zhiniu.pages.components

import com.zhiniu.domain.model.CandleDirection
import com.zhiniu.domain.model.KLineBar
import com.zhiniu.domain.model.KLineChart

/** 单根蜡烛绘制所需（几何 + A 股配色）。 */
data class CandlePaint(
    val x: Float,
    val bodyTop: Float,
    val bodyBottom: Float,
    val bodyWidth: Float,
    val wickHigh: Float,
    val wickLow: Float,
    val fill: String,   // 实体/影线颜色：红(阳)绿(阴)灰(平)
)

/** K 线图整体绘制数据（几何已算好，供 Canvas 逐项描画）。 */
data class CandlestickPaint(
    val candles: List<CandlePaint>,
    val ma5: List<Float?>,
    val ma10: List<Float?>,
    val ma20: List<Float?>,
)

/** 颜色：A 股红涨绿跌。 */
private fun candleFill(d: CandleDirection): String = when (d) {
    CandleDirection.UP -> Palette.UP
    CandleDirection.DOWN -> Palette.DOWN
    CandleDirection.FLAT -> Palette.FLAT
}

/** 几何 bundle → 绘制数据（纯函数，可单测）。 */
fun buildCandlestickPaint(
    bars: List<KLineBar>,
    widthPx: Float,
    heightPx: Float,
    showMA: Boolean = true,
): CandlestickPaint {
    val bundle = KLineChart.buildBundle(bars, widthPx, heightPx)
    return CandlestickPaint(
        candles = bundle.candles.map { c ->
            CandlePaint(c.x, c.bodyTop, c.bodyBottom, c.bodyWidth, c.wickHigh, c.wickLow, candleFill(c.direction))
        },
        ma5 = if (showMA) bundle.ma5 else emptyList(),
        ma10 = if (showMA) bundle.ma10 else emptyList(),
        ma20 = if (showMA) bundle.ma20 else emptyList(),
    )
}

/**
 * 生成 K 线 Canvas 的 DSL 块（供 Page 直接嵌入）。
 *
 * 渲染契约（几何已全量预计算在 buildCandlestickPaint）：
 *   Canvas {
 *     paint.candles.forEach { c ->
 *       drawLine(c.fill, start=(c.x, c.wickHigh), end=(c.x, c.wickLow))  // 影线
 *       drawRect(c.fill, left=c.x-c.bodyWidth, top=c.bodyTop,
 *                right=c.x+c.bodyWidth, bottom=c.bodyBottom)             // 实体
 *     }
 *     if (showMA) { drawLine(Palette.MA5, paint.ma5); drawLine(Palette.MA10, paint.ma10); drawLine(Palette.MA20, paint.ma20) }
 *   }
 * 实际的 Canvas 绘制方法签名以待 Kuikly SDK 模板校准；paint 数据已全量预计算，仅需照契约落绘制。
 */