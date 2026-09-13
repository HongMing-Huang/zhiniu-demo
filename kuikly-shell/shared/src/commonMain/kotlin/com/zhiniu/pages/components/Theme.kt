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

/** 外观偏好持久化键（SharedPreferences 值 = ThemeMode 枚举名）。
 *  「我的」页三段式/快捷切换与 AI 指令 set_appearance 共用，保证单一事实源。 */
internal const val THEME_MODE_SP_KEY = "zhiniu.prefs.theme-mode.v1"

/** 涨跌配色持久化键（"1" = 绿涨红跌反转）。
 *  「我的」页双段胶囊与 AI 指令 set_color_mode 共用。 */
internal const val UPDOWN_SP_KEY = "zhiniu.prefs.swap-updown.v1"

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
    open val pageBg: String,           // App Background
    open val surface: String,           // Surface
    open val surfaceSecondary: String,  // Secondary / Skeleton / Toolbar bg
    open val surfaceHover: String,      // Row / Button hover
    open val elevated: String,          // Popover / 浮层表面
    open val border: String,
    open val borderStrong: String,
    open val textPrimary: String,
    open val textSecondary: String,
    open val textTertiary: String,      // 命名保留以兼容旧调用（语义 = Text Muted）
    open val up: String,
    open val down: String,
    open val flat: String,
    open val riseBackground: String,   // 涨 chip 浅红底
    open val fallBackground: String,   // 跌 chip 浅绿底
    open val riskLow: String,
    open val riskMediumLow: String,
    open val riskMedium: String,
    open val riskMediumHigh: String,
    open val riskHigh: String,
    open val ma5: String, open val ma10: String, open val ma20: String,
    open val dif: String, open val dea: String, open val rsi: String,
    open val chartBg: String, open val chartGrid: String, open val axisText: String,
    open val crosshair: String,         // Crosshair 虚线
    open val aiAccent: String = "#D9FF43", // AI 高亮色（仅在 AI 区域，绿黄荧光）
    open val surfaceRaised: String = surface, // Surface Raised（Dark 用）
)

/** Light —— 微灰页面底 + 纯白 Surface（卡片浮层层次，对标移动端行情 App）。 */
object LightColors : AppColors(
    pageBg = "#F7F8FA",
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
    riseBackground = "#FDEBEC",
    fallBackground = "#E6F6EE",
    riskLow = "#16B364",
    riskMediumLow = "#84B04C",
    riskMedium = "#E9A23B",
    riskMediumHigh = "#F2762E",
    riskHigh = "#F04F5F",
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
    riseBackground = "#3A1A1E",
    fallBackground = "#12291D",
    riskLow = "#3BC177",
    riskMediumLow = "#9CC064",
    riskMedium = "#F0B04C",
    riskMediumHigh = "#F58A4B",
    riskHigh = "#F6707D",
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

/**
 * 稳定的动态调色板代理。
 *
 * 组件通常会在声明阶段缓存 `AppTheme.colors`。代理本身保持不变，所有语义色 getter
 * 则在属性重新求值时读取当前主题，避免切换主题后出现浅/深色调色板混用。
 */
private object AdaptiveColors : AppColors(
    pageBg = "", surface = "", surfaceSecondary = "", surfaceHover = "", elevated = "",
    border = "", borderStrong = "", textPrimary = "", textSecondary = "", textTertiary = "",
    up = "", down = "", flat = "",
    riseBackground = "", fallBackground = "",
    riskLow = "", riskMediumLow = "", riskMedium = "", riskMediumHigh = "", riskHigh = "",
    ma5 = "", ma10 = "", ma20 = "", dif = "", dea = "",
    rsi = "", chartBg = "", chartGrid = "", axisText = "", crosshair = "", aiAccent = "",
    surfaceRaised = "",
) {
    private val active: AppColors get() = if (AppTheme.isDark) DarkColors else LightColors
    // 涨跌配色反转：up↔down / 浅红底↔浅绿底 同步交换，保证语义一致
    override val up get() = if (AppTheme.swapUpDon) active.down else active.up
    override val down get() = if (AppTheme.swapUpDon) active.up else active.down
    override val riseBackground get() = if (AppTheme.swapUpDon) active.fallBackground else active.riseBackground
    override val fallBackground get() = if (AppTheme.swapUpDon) active.riseBackground else active.fallBackground
    override val riskLow get() = active.riskLow
    override val riskMediumLow get() = active.riskMediumLow
    override val riskMedium get() = active.riskMedium
    override val riskMediumHigh get() = active.riskMediumHigh
    override val riskHigh get() = active.riskHigh
    override val pageBg get() = active.pageBg
    override val surface get() = active.surface
    override val surfaceSecondary get() = active.surfaceSecondary
    override val surfaceHover get() = active.surfaceHover
    override val elevated get() = active.elevated
    override val border get() = active.border
    override val borderStrong get() = active.borderStrong
    override val textPrimary get() = active.textPrimary
    override val textSecondary get() = active.textSecondary
    override val textTertiary get() = active.textTertiary
    override val flat get() = active.flat
    override val ma5 get() = active.ma5
    override val ma10 get() = active.ma10
    override val ma20 get() = active.ma20
    override val dif get() = active.dif
    override val dea get() = active.dea
    override val rsi get() = active.rsi
    override val chartBg get() = active.chartBg
    override val chartGrid get() = active.chartGrid
    override val axisText get() = active.axisText
    override val crosshair get() = active.crosshair
    override val aiAccent get() = active.aiAccent
    override val surfaceRaised get() = active.surfaceRaised
}

/** 全局主题控制器（单例）。 */
object AppTheme {
    var mode by observable(ThemeMode.SYSTEM)
    private var systemDark by observable(false)
    var isDark by observable(false)
    /** 涨跌配色反转（绿涨红跌）：A股海外化偏好，全App经 AdaptiveColors 代理生效。 */
    var swapUpDon by observable(false)
    val colors: AppColors get() = AdaptiveColors

    /** 外观持久化钩子：首页/我的页 created 时注入（写 SharedPreferences），
     *  让「我的」页切换与 AI 指令 set_appearance 共用同一落盘路径。 */
    internal var persistHook: ((ThemeMode) -> Unit)? = null

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
    /** 设置主题模式（applyMode：与 `mode` 属性的 JVM setter 签名区分开）。 */
    fun applyMode(m: ThemeMode) {
        mode = m
        applyDark(if (m != ThemeMode.SYSTEM) resolve(m) else systemDark)
    }
    /** 切换外观并落盘（浅色/深色/跟随系统；重启后恢复）。 */
    fun applyModePersisted(m: ThemeMode) {
        applyMode(m)
        persistHook?.invoke(m)
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
