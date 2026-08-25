package com.zhiniu.pages.components

object Tokens {
    // ---- A.1 中性色（Dark）----
    const val bgPage = "#0A0A0A"       // 页面底
    const val bgCard = "#141414"       // 卡片底
    const val bgCardHover = "#1A1A1A"  // 卡片悬停 / 选中
    const val bgElevated = "#1A1A1A"   // 浮层面板（搜索/弹窗）
    const val divider = "#262626"      // 描边 / 分割线
    const val textPrimary = "#F5F5F5"  // 主文字
    const val textSecondary = "#8D8D8D"// 次要文字
    const val textMuted = "#626262"    // 弱化文字

    // ---- A.2 金融语义色（红涨绿跌）----
    const val up = "#F04F5F"           // 上涨红
    const val down = "#16B364"         // 下跌绿
    const val warn = "#F5A524"         // 警示 / stale
    const val info = "#7C3AED"         // 资讯标签
    const val aiAccent = "#D9FF43"     // AI 强调色（灰绿荧光）

    // ---- A.3 品牌色 ----
    const val brand = "#D9FF43"        // AI 品牌色（按钮 / 选中状态）
    const val brandText = "#0A0A0A"    // 品牌色上的文字（反色黑）
    const val brandDeep = "#00875A"    // 辅助品牌绿（历史链接/标签文字）

    // ---- A.4 字号（Kuikly 用 Float px 字面量）----
    const val fsDisplay = 32f
    const val fsH1 = 24f
    const val fsH2 = 20f
    const val fsH3 = 16f
    const val fsBody = 14f
    const val fsCaption = 12f
    const val fsMono = 13f             // 等宽数字字号

    // ---- A.5 间距（8 倍数）----
    const val space1 = 4f
    const val space2 = 8f
    const val space3 = 12f
    const val space4 = 16f
    const val space5 = 20f
    const val space6 = 24f
    const val space8 = 32f
    const val space12 = 48f

    // ---- A.6 圆角 ----
    const val radiusSm = 4f
    const val radiusMd = 6f
    const val radiusLg = 8f
    const val radiusXl = 12f
}

/** #RRGGBB → 0xAARRGGBB Int（供 Kuikly Color(Int) 构造，Web 渲染会一并染色）。 */
internal fun hexInt(hex: String): Int {
    val clean = hex.removePrefix("#")
    return (0xFF shl 24) or clean.toLong(16).toInt()
}

/** hex 颜色 + alpha 百分比（0-100）→ 0xAARRGGBB Int。 */
internal fun hexIntAlpha(hex: String, alphaPct: Int): Int {
    val clean = hex.removePrefix("#")
    val alpha = ((alphaPct.coerceIn(0, 100) * 255) / 100) and 0xFF
    return (alpha shl 24) or clean.toLong(16).toInt()
}

/** 涨跌色 红涨绿跌（返回 Int 供 Color(Int) 构造）。 */
internal fun Tokens.upOrDown(isUp: Boolean): Int = if (isUp) hexInt(up) else hexInt(down)