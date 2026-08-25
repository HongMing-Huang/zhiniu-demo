// 知牛 · 统一 Theme Token（Kuikly DSL 全局唯一颜色来源）
// 任何页面不得直接写 #RRGGBB；一律通过 ThemeState.palette 取色。
// Light / Dark 两套中性色 + 柔和红涨绿跌；支持 跟随系统 / 浅色 / 深色。
package com.zhiniu.pages.components

import com.tencent.kuikly.core.base.Animation
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

/** 一套完整主题 Token。 */
open class Palette(
    // ---- 背景 / 表面层级 ----
    val pageBg: String,          // 页面底（铺满浏览器）
    val surface: String,         // Header / 卡片
    val surfaceSecondary: String,// 次级表面（输入框、表头）
    val surfaceHover: String,    // 悬停
    val elevated: String,        // 弹层浮面（search/popover/drawer）
    // ---- 描边 ----
    val border: String,          // 1px 弱分割线
    val borderStrong: String,    // 卡片描边
    // ---- 文字 ----
    val textPrimary: String,
    val textSecondary: String,
    val textTertiary: String,
    // ---- 行情语义色（柔和红涨绿跌，仅用于数据状态）----
    val up: String,
    val down: String,
    val flat: String,            // 平盘
    // ---- MA 均线 ----
    val ma5: String,
    val ma10: String,
    val ma20: String,
    // ---- 图表 ----
    val chartBg: String,
    val chartGrid: String,
    val axisText: String,
)

/** Light Theme —— off-white 页面 + 白色表面 + 黑色排版，参考 OKX/Linear Light。 */
object LightPalette : Palette(
    pageBg = "#F7F7F5",
    surface = "#FFFFFF",
    surfaceSecondary = "#F2F2EF",
    surfaceHover = "#ECECEA",
    elevated = "#FFFFFF",
    border = "#E5E5E1",
    borderStrong = "#D8D8D3",
    textPrimary = "#111111",
    textSecondary = "#676767",
    textTertiary = "#969696",
    up = "#D94F4F",
    down = "#2F9E6E",
    flat = "#969696",
    ma5 = "#E3A24C",
    ma10 = "#5B7CFA",
    ma20 = "#A879D8",
    chartBg = "#FFFFFF",
    chartGrid = "#EFEFED",
    axisText = "#8A8A84",
)

/** Dark Theme —— 有层级的深灰，拒绝纯黑。 */
object DarkPalette : Palette(
    pageBg = "#101110",
    surface = "#171817",
    surfaceSecondary = "#1D1E1D",
    surfaceHover = "#242524",
    elevated = "#1B1C1A",
    border = "#2A2B29",
    borderStrong = "#383936",
    textPrimary = "#F3F3F1",
    textSecondary = "#A1A19B",
    textTertiary = "#73736E",
    up = "#E06A6A",
    down = "#44AE82",
    flat = "#73736E",
    ma5 = "#E3A24C",
    ma10 = "#7B8DFB",
    ma20 = "#B78CE0",
    chartBg = "#171817",
    chartGrid = "#292A28",
    axisText = "#7E7E78",
)

/**
 * 全局主题控制器（单例，跨页面共享）。
 * mode 驱动 isDark；SYSTEM 时跟随系统并监听系统切换（H5: matchMedia；其他端由 actual 决定）。
 */
object ThemeState {
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

    /** 当前生效的 Token。 */
    val palette: Palette get() = if (isDark) DarkPalette else LightPalette

    private var themeWatchRegistered = false
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
internal fun Palette.c(hex: String): Color = Color(hexI(hex))
internal fun Palette.ca(hex: String, alphaPct: Int): Color = Color(hexA(hex, alphaPct))
