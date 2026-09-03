// 知牛 · 主题 Token（Light/Dark · 赛题规范）
// AppColors 字段 = 语义层；调用端始终通过 `colors.c(colors.<field>)` 取色。
package com.zhiniu.pages.components

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.reactive.handler.observable
import com.zhiniu.platform.applyHostTheme
import com.zhiniu.platform.systemPrefersDark
import com.zhiniu.platform.watchSystemTheme

/** 主题切换统一时长。 */
val ANIM_THEME = com.tencent.kuikly.core.base.Animation.easeOut(0.18f)

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色"),
}

// ---------- 排版 / 间距 / 圆角 / 尺寸 ----------
object AppTypography {
    const val fs32 = 32f
    const val fs28 = 28f
    const val fs24 = 24f
    const val fs20 = 20f
    const val fs18 = 18f
    const val fs16 = 16f
    const val fs15 = 15f
    const val fs14 = 14f
    const val fs13 = 13f
    const val fs12 = 12f
    const val fs11 = 11f
    /** 数字等宽视觉（H5 生效，原生端优雅降级）。 */
    const val NUM_FONT = "ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace"
}

object AppSpacing {
    const val s1 = 4f; const val s2 = 8f; const val s3 = 12f
    const val s4 = 16f; const val s5 = 20f; const val s6 = 24f
    const val s8 = 32f; const val s10 = 40f
    const val header = 64f
    const val marketTitle = 72f
    const val pulse = 92f
    const val chartHeight = 560f
    const val row64 = 64f
}

object AppRadius {
    const val radius5 = 5f
    const val radius6 = 6f
    const val radius8 = 8f
}

/** 内容最大宽度 / 水平留白。 */
const val CONTENT_W = 1360f
const val PAD = 32f

// ---------- 颜色 Token ----------
open class AppColors(
    val pageBg: String,           // App Background
    val surface: String,           // Surface
    val surfaceSecondary: String,  // Secondary / Skeleton / Toolbar bg
    val surfaceHover: String,      // Row / Button hover
    val elevated: String,          // Popover / 浮层表面
    val border: String,
    val borderStrong: String,
    val textPrimary: String,
    val textSecondary: String,
    val textTertiary: String,      // 命名保留以兼容旧调用（语义 = Text Muted）
    val up: String,
    val down: String,
    val flat: String,
    val ma5: String, val ma10: String, val ma20: String,
    val dif: String, val dea: String, val rsi: String,
    val chartBg: String, val chartGrid: String, val axisText: String,
    val crosshair: String,         // Crosshair 虚线
    val aiAccent: String = "#D9FF43", // AI 高亮色（仅在 AI 区域，绿黄荧光）
    val surfaceRaised: String = surface, // Surface Raised（Dark 用）
)

/** Light —— 中性白（AppBg #FFFFFF，绝非整页灰底）。 */
object LightColors : AppColors(
    pageBg = "#FFFFFF",
    surface = "#FFFFFF",
    surfaceSecondary = "#F7F8FA",
    surfaceHover = "#EEF1F4",
    elevated = "#FFFFFF",
    border = "#E7E9ED",
    borderStrong = "#D9DDE3",
    textPrimary = "#171A1F",
    textSecondary = "#66707A",
    textTertiary = "#98A1AB",
    up = "#F04F5F",
    down = "#16B364",
    flat = "#98A1AB",
    ma5 = "#E9A23B",
    ma10 = "#4D7CFE",
    ma20 = "#A56BEA",
    dif = "#4D7CFE",
    dea = "#E9A23B",
    rsi = "#A56BEA",
    chartBg = "#FFFFFF",
    chartGrid = "#EFF1F3",
    axisText = "#919AA4",
    crosshair = "#AAB1B9",
)

/** Dark —— 有层级深灰，非纯黑。 */
object DarkColors : AppColors(
    pageBg = "#0B0D0F",
    surface = "#111417",
    surfaceSecondary = "#171B20",
    surfaceRaised = "#171B20",
    surfaceHover = "#1B2026",
    elevated = "#171B20",
    border = "#262B31",
    borderStrong = "#2D3239",
    textPrimary = "#F3F4F5",
    textSecondary = "#9AA3AD",
    textTertiary = "#69727D",
    up = "#F04F5F",
    down = "#16B364",
    flat = "#69727D",
    ma5 = "#E9A23B",
    ma10 = "#4D7CFE",
    ma20 = "#A56BEA",
    dif = "#4D7CFE",
    dea = "#E9A23B",
    rsi = "#A56BEA",
    chartBg = "#111417",
    chartGrid = "#20242A",
    axisText = "#6C7682",
    crosshair = "#3A4148",
)

/** 全局主题控制器（单例）。 */
object AppTheme {
    var mode by observable(ThemeMode.SYSTEM)
    private var systemDark by observable(false)
    var isDark by observable(false)
    val colors: AppColors get() = if (isDark) DarkColors else LightColors

    fun start() {
        systemDark = systemPrefersDark()
        applyDark(resolvedDark())
        if (!themeWatchRegistered) {
            themeWatchRegistered = true
            watchSystemTheme { dark ->
                systemDark = dark
                if (mode == ThemeMode.SYSTEM) applyDark(dark)
            }
        }
    }
    fun resolve(m: ThemeMode): Boolean = when (m) {
        ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> systemDark
    }
    private fun resolvedDark() = resolve(mode)
    fun setMode(m: ThemeMode) {
        mode = m
        applyDark(if (m != ThemeMode.SYSTEM) resolve(m) else systemDark)
    }
    private fun applyDark(v: Boolean) { isDark = v; applyHostTheme(v) }
    private var themeWatchRegistered = false
}

typealias Palette = AppColors

/** host CSS 钩子。 */
fun Attr.cssClass(value: String) { "cssClass" with value }

/** #RRGGBB → 0xFFRRGGBB Int。 */
internal fun hexI(hex: String): Int {
    val c = hex.removePrefix("#")
    return (0xFF shl 24) or c.toLong(16).toInt()
}
internal fun hexA(hex: String, alphaPct: Int): Int {
    val c = hex.removePrefix("#")
    val a = ((alphaPct.coerceIn(0, 100) * 255) / 100) and 0xFF
    return (a shl 24) or c.toLong(16).toInt()
}
internal fun AppColors.c(hex: String): Color = Color(hexI(hex))
internal fun AppColors.ca(hex: String, alphaPct: Int): Color = Color(hexA(hex, alphaPct))
