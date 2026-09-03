/* 知牛 · K 线数据/几何纯逻辑层（供 CandlestickChart 绘制消费，无 Kuikly/UI 依赖）
 *
 * T1-3 基础：Canvas 绘制层只消费这里算好的指标与几何量，绘制本身待壳工程后接入。
 * - movingAverage(): 各条均线（MA5/10/20）逐根对齐
 * - candleDirection() / prices(): K 线颜色与高低价区间
 * - CandleLayout: 把 (open/high/low/close) 归一化到给定像素画布，供 Canvas 直接描影线+实体
 */
package com.zhiniu.domain.model

/** K 线实体方向（A 股红涨绿跌）。 */
enum class CandleDirection { UP, DOWN, FLAT }

/** 单根蜡烛的像素几何（y 为像素坐标，属性名与绘制一一对应）。 */
data class CandleGeometry(
    val x: Float,          // 实体中心 x
    val bodyTop: Float,    // 实体上沿（open 或 close 之较高者）
    val bodyBottom: Float, // 实体下沿（open 或 close 之较低者）
    val bodyWidth: Float,  // 实体半宽
    val wickHigh: Float,   // 最高价影线 y
    val wickLow: Float,    // 最低价影线 y
    val direction: CandleDirection,
)

/** 整幅 K 线的绘制几何（含均线点位，MA 为 null 处不画点）。 */
data class CandleGeometryBundle(
    val candles: List<CandleGeometry>,
    val ma5: List<Float?>,
    val ma10: List<Float?>,
    val ma20: List<Float?>,
    val minPrice: Double,
    val maxPrice: Double,
)

/** K 线纯逻辑助手：均线 / 颜色 / 区间 / 像素归一化。 */
object KLineChart {

    /** 均线：返回与输入等长的序列，前 (period-1) 项为 null（样本不足）。 */
    fun movingAverage(closes: List<Double>, period: Int): List<Double?> {
        if (period <= 0) return List(closes.size) { null }
        val out = MutableList<Double?>(closes.size) { null }
        var sum = 0.0
        for (i in closes.indices) {
            sum += closes[i]
            if (i >= period) sum -= closes[i - period]
            if (i >= period - 1) out[i] = sum / period
        }
        return out
    }

    /** 高低价区间（用于 Y 轴归一化）；空表返回 0..1。 */
    fun priceBounds(bars: List<Candle>): Pair<Double, Double> {
        if (bars.isEmpty()) return 0.0 to 1.0
        val hi = bars.maxOf { it.high }
        val lo = bars.minOf { it.low }
        val pad = (hi - lo).coerceAtLeast(1e-9) * 0.08  // 上下留 8% 空边
        return (lo - pad) to (hi + pad)
    }

    /** K 线颜色：A 股红涨绿跌。 */
    fun candleDirection(open: Double, close: Double): CandleDirection = when {
        close > open -> CandleDirection.UP
        close < open -> CandleDirection.DOWN
        else -> CandleDirection.FLAT
    }

    /**
     * 生成整幅 K 线几何。总线宽 width、左右留白 padL/padR、图高 height。
     * 无 bars 时返回空 bundle（bounds 由调用方兜底 Mock，避免白屏）。
     */
    fun layout(
        bars: List<Candle>,
        width: Float,
        height: Float,
        padL: Float = 12f,
        padR: Float = 12f,
        bodyWidthFactor: Float = 0.72f,
    ): List<CandleGeometry> = layoutWithBounds(bars, width, height, priceBounds(bars), padL, padR, bodyWidthFactor)

    private fun layoutWithBounds(
        bars: List<Candle>,
        width: Float,
        height: Float,
        bounds: Pair<Double, Double>,
        padL: Float,
        padR: Float,
        bodyWidthFactor: Float,
    ): List<CandleGeometry> {
        if (bars.isEmpty()) return emptyList()
        val (lo, hi) = bounds
        val span = (hi - lo).coerceAtLeast(1e-9).toFloat()
        val n = bars.size
        val usable = (width - padL - padR).coerceAtLeast(0f)
        val slot = if (n > 1) usable / n else usable
        val bodyW = (slot * bodyWidthFactor).coerceAtMost(slot * 0.9f)
        val yOf = { v: Double -> (height - ((v - lo) / span) * height).toFloat() }

        return bars.mapIndexed { i, b ->
            val x = padL + slot * i + slot / 2f
            val top = minOf(b.open, b.close); val bot = maxOf(b.open, b.close)
            CandleGeometry(
                x = x,
                bodyTop = yOf(bot),
                bodyBottom = yOf(top),
                bodyWidth = bodyW,
                wickHigh = yOf(b.high),
                wickLow = yOf(b.low),
                direction = candleDirection(b.open, b.close),
            )
        }
    }

    /** 便捷聚合：几何 + 三条均线点位 + 价格区间（一次算好供绘制层用，蜡烛与均线共享同一 y 缩放）。 */
    fun buildBundle(bars: List<Candle>, width: Float, height: Float): CandleGeometryBundle {
        val (lo, hi) = priceBounds(bars)
        val closes = bars.map { it.close }
        val yOf = { v: Double -> (height - ((v - lo) / (hi - lo).toFloat().coerceAtLeast(1e-9f)) * height).toFloat() }
        return CandleGeometryBundle(
            candles = layoutWithBounds(bars, width, height, lo to hi, 12f, 12f, 0.72f),
            ma5 = toY(movingAverage(closes, 5), yOf),
            ma10 = toY(movingAverage(closes, 10), yOf),
            ma20 = toY(movingAverage(closes, 20), yOf),
            minPrice = lo,
            maxPrice = hi,
        )
    }

    private fun toY(values: List<Double?>, yOf: (Double) -> Float): List<Float?> =
        values.map { it?.let(yOf) }
}