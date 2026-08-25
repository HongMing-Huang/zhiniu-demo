// 知牛 · AppTheme 统一 Token（Kuikly DSL 全局唯一主题来源）
// 任何页面不得直接写 #RRGGBB / Color.WHITE / Color.BLACK；一律读取 AppTheme.*。
// Light / Dark / SYSTEM（跟随系统）三态；切换即时生效、不刷新页面。
package com.zhiniu.pages.components

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.reactive.handler.observable
import com.zhiniu.platform.applyHostTheme
import com.zhiniu.platform.systemPrefersDark
import com.zhiniu.platform.watchSystemTheme

/** 主题切换统一时长（180ms easeOut）。 */
val ANIM_THEME = Animation.easeOut(0.18f)

/** 外观模式。默认 SYSTEM。 */
enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

// ---------- 排版 / 间距 / 圆角 Token ----------
object AppTypography {
    const val fs32 = 32f
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
    const val s1 = 4f
    const val s2 = 8f
    const val s3 = 12f
    const val s4 = 16f
    const val s5 = 20f
    const val s6 = 24f
    const val s8 = 32f
    const val s10 = 40f
}

object AppRadius {
    const val r6 = 6f
    const val r8 = 8f
    const val r10 = 10f
    const val r12 = 12f
}

// ---------- 颜色 Token ----------
open class AppColors(
    // 背景 / 表面层级
    val pageBg: String,
    val surface: String,
    val surfaceSecondary: String,
    val surfaceHover: String,
    val elevated: String,
    // 描边
    val border: String,
    val borderStrong: String,
    // 文字
    val textPrimary: String,
    val textSecondary: String,
    val textTertiary: String,
    // 行情语义（红涨绿跌，仅数据状态）
    val up: String,
    val down: String,
    val flat: String,
    // 指标
    val ma5: String,
    val ma10: String,
    val ma20: String,
    val dif: String,
    val dea: String,
    val rsi: String,
    // 图表
    val chartBg: String,
    val chartGrid: String,
    val axisText: String,
)

/** Light —— 中性冷白（非米白）。 */
object LightColors : AppColors(
    pageBg = "#F5F6F7",
    surface = "#FFFFFF",
    surfaceSecondary = "#F8F9FA",
    surfaceHover = "#F2F3F5",
    elevated = "#FFFFFF",
    border = "#E7E9EC",
    borderStrong = "#D9DCE1",
    textPrimary = "#17191C",
    textSecondary = "#66707A",
    textTertiary = "#98A0A8",
    up = "#E5484D",
    down = "#12A66A",
    flat = "#98A0A8",
    ma5 = "#E3A24C",
    ma10 = "#5B7CFA",
    ma20 = "#A879D8",
    dif = "#5B7CFA",
    dea = "#E3A24C",
    rsi = "#A879D8",
    chartBg = "#FFFFFF",
    chartGrid = "#EDF0F2",
    axisText = "#89919A",
)

/** Dark —— 有层级深灰，非纯黑。 */
object DarkColors : AppColors(
    pageBg = "#0F1113",
    surface = "#16191D",
    surfaceSecondary = "#1B1F24",
    surfaceHover = "#22272D",
    elevated = "#16191D",
    border = "#292E35",
    borderStrong = "#343A42",
    textPrimary = "#F3F4F5",
    textSecondary = "#9BA3AC",
    textTertiary = "#6E7781",
    up = "#E5484D",
    down = "#12A66A",
    flat = "#6E7781",
    ma5 = "#E3A24C",
    ma10 = "#7B8DFB",
    ma20 = "#B78CE0",
    dif = "#7B8DFB",
    dea = "#E3A24C",
    rsi = "#B78CE0",
    chartBg = "#16191D",
    chartGrid = "#272C32",
    axisText = "#79828C",
)

/**
 * 全局主题控制器（单例）。
 * mode 驱动 isDark；SYSTEM 时跟随系统并监听系统切换（H5: matchMedia；其他端由 actual 决定）。
 */
object AppTheme {
    var mode by observable(ThemeMode.SYSTEM)
    private var systemDark by observable(false)
    var isDark by observable(false)

    /** 在页面创建时调用：读取系统偏好 + 注册系统切换监听。 */
    fun start() {
        systemDark = systemPrefersDark()
        applyDark(resolvedDark())
        if (!themeWatchRegistered) {
            themeWatchRegistered = true
            watchSystemTheme { dark ->
                systemDark = dark
                if (mode == ThemeMode.SYSTEM) {
                    applyDark(dark)
                }
            }
        }
    }

    fun resolve(m: ThemeMode): Boolean = when (m) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    private fun resolvedDark() = resolve(mode)

    fun setMode(m: ThemeMode) {
        mode = m
        applyDark(if (m != ThemeMode.SYSTEM) resolve(m) else systemDark)
    }

    private fun applyDark(v: Boolean) {
        isDark = v
        applyHostTheme(v)
    }

    /** 当前生效的颜色 Token。 */
    val colors: AppColors get() = if (isDark) DarkColors else LightColors

    private var themeWatchRegistered = false
}

// ---------- 兼容别名（旧调用逐步迁移） ----------
typealias Palette = AppColors
typealias LightPalette = LightColors
typealias DarkPalette = DarkColors

/** host CSS 钩子：类名会加进 DOM，host 的 :hover / transition CSS 生效。 */
fun Attr.cssClass(value: String) {
    "cssClass" with value
}

// ---------- 颜色工具 ----------
/** #RRGGBB → 0xFFRRGGBB Int（Kuikly Color(Int)）。 */
internal fun hexI(hex: String): Int {
    val clean = hex.removePrefix("#")
    return (0xFF shl 24) or clean.toLong(16).toInt()
}

/** #RRGGBB + alpha 百分比 → 0xAARRGGBB Int。 */
internal fun hexA(hex: String, alphaPct: Int): Int {
    val clean = hex.removePrefix("#")
    val alpha = ((alphaPct.coerceIn(0, 100) * 255) / 100) and 0xFF
    return (alpha shl 24) or clean.toLong(16).toInt()
}

/** 由当前 theme 取色。 */
internal fun AppColors.c(hex: String): Color = Color(hexI(hex))
internal fun AppColors.ca(hex: String, alphaPct: Int): Color = Color(hexA(hex, alphaPct))
