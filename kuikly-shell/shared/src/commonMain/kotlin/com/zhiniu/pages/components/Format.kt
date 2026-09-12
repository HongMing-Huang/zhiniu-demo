// 知牛 · 数字与文本格式化（纯函数，跨端安全；不依赖 java.util.Locale）
package com.zhiniu.pages.components

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 数字等宽字体（Kuikly 官方各端字体接入，规范见 docs/typography.md）：
 * - H5：web-host/index.html 以同名 @font-face 注册，字体就绪后才拉起渲染宿主（官方 h5-custom-font 指引）
 * - Android：KRFontAdapter 从 assets/fonts/<名称>.ttf 加载（官方 IKRFontAdapter）
 * - iOS：KRFontHandler 从共享资源目录按「文件名 = 字体名」加载（官方 KRFontModule 约定）
 * - 未注册到字体的端由系统默认字体优雅降级，布局不受影响。
 */
const val NUM_FONT = "JetBrainsMono-Regular"

/** 108.5 → "108.50"；-1 → "-0.01" 防御。 */
fun fmt2(v: Double): String {
    val neg = v < 0
    val a = abs(v)
    val s = (a * 100).roundToInt().toString().padStart(3, '0')
    val out = s.substring(0, s.length - 2) + "." + s.substring(s.length - 2)
    return if (neg) "-" + out else out
}

/** 0.39 → "+0.39%"。 */
fun fmtPct(v: Double): String {
    val sign = if (v >= 0) "+" else "-"
    return sign + fmt2(abs(v)) + "%"
}

/** 元 → "30.00亿" / "1.62T" / "1.2万"。 */
fun fmtAmount(v: Double): String {
    val raw = kotlin.math.round(v).toLong()
    return when {
        raw >= 1e12 -> fmt2(v / 1e12) + "T"
        raw >= 1e8 -> fmt2(v / 1e8) + "亿"
        raw >= 1e4 -> fmt2(v / 1e4) + "万"
        else -> raw.toString()
    }
}

/** sh600519 → "600519·SH"（无空格：tabular-nums 下窄列不折行）。 */
fun fmtSymbol(sym: String): String {
    val prefix = sym.take(2).uppercase()
    val code = sym.drop(2)
    return "$code·$prefix"
}

/** 市场标签。 */
fun marketLabelOf(sym: String): String = when {
    sym.startsWith("sh") -> "沪市"
    sym.startsWith("sz") -> "深市"
    else -> "其他"
}

/** 涨跌方向文案。 */
fun fmtChangeSigned(v: Double): String =
    (if (v >= 0) "+" else "-") + fmt2(abs(v))

/** 内容最大宽度（Header 与正文共用，保证左右对齐；1920 居中、1440 完整、1280 不溢出）。 */

/** 水平边距。 */

/** 整数千分位：3128 → "3,128"；986400000000 → "9,864亿"（配合亿单位使用）。 */
fun fmtInt(v: Long): String {
    val neg = v < 0
    val s = kotlin.math.abs(v).toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(s[i])
    }
    return (if (neg) "-" else "") + sb.toString()
}

/** 成交量（手）→ "2.18万手" / "38.5万手"。 */
fun fmtVolHand(v: Long): String {
    return when {
        v >= 100_000_000 -> fmt2(v / 100_000_000.0) + "亿手"
        v >= 10_000 -> fmt2(v / 10_000.0) + "万手"
        else -> fmtInt(v) + "手"
    }
}

/** 总市值（元）→ "1.59万亿" / "2278亿"；null → "—"（A 股口径，与成交额的 T 单位区分）。 */
fun fmtMarketCap(v: Double?): String {
    if (v == null || v <= 0.0) return "—"
    return when {
        v >= 1e12 -> fmt2(v / 1e12) + "万亿"
        v >= 1e8 -> kotlin.math.round(v / 1e8).toLong().toString() + "亿"
        else -> fmt2(v / 1e4) + "万"
    }
}

/** 振幅 %。 */
fun fmtAmplitude(high: Double, low: Double, prevClose: Double): String {
    if (prevClose == 0.0) return "0.00%"
    return fmt2((high - low) / prevClose * 100.0) + "%"
}
