package com.zhiniu.pages.components

/**
 * 知牛 Light Theme 设计 Token（唯一权威，来源 docs/ai-design-guide.md 附录 A）。
 *
 * 红线约束（dev-prompt 九）：禁止硬编码色值/尺寸/圆角/动效——一律引用本对象。
 * 涨 #D93025 / 跌 #1E8E3E（红涨绿跌，中国习惯）。
 */
object Tokens {
    // ---- A.1 中性色 ----
    const val bgPage = "#F5F7FA"      // 页面底
    const val bgCard = "#FFFFFF"      // 卡片底
    const val bgHover = "#F0F3F7"     // 悬停 / 二级面板
    const val border = "#E5E9F0"      // 描边 / 分割线
    const val textPrimary = "#1F2329" // 主文字
    const val textSecondary = "#4E5561"
    const val textTertiary = "#8A919C"

    // ---- A.2 金融语义色（红涨绿跌）----
    const val up = "#D93025"          // 涨
    const val down = "#1E8E3E"        // 跌
    const val warn = "#B45309"        // 警示 / stale
    const val info = "#7C3AED"        // 资讯标签

    // ---- A.3 品牌色 ----
    const val brand = "#00A870"       // 主绿（按钮 / Tab 选中 / 品牌元素）
    const val brandDeep = "#00875A"   // 主绿文字版

    // ---- A.4 字号（Kuikly 用 Float px 字面量）----
    const val fsDisplay = 32f
    const val fsH1 = 24f
    const val fsH2 = 20f
    const val fsH3 = 16f
    const val fsBody = 14f
    const val fsCaption = 12f

    // ---- A.5 间距（8 倍数）----
    const val space1 = 4f
    const val space2 = 8f
    const val space3 = 12f
    const val space4 = 16f
    const val space5 = 20f
    const val space6 = 24f
    const val space8 = 32f
    const val space12 = 48f
}

/** #RRGGBB → 0xAARRGGBB Int（供 Kuikly Color(Int) 构造，Web 渲染会一并染色）。 */
internal fun hexInt(hex: String): Int {
    val clean = hex.removePrefix("#")
    return (0xFF shl 24) or clean.toLong(16).toInt()
}

/** 涨跌色 红涨绿跌（返回 Int 供 Color(Int) 构造）。 */
internal fun Tokens.upOrDown(isUp: Boolean): Int = if (isUp) hexInt(up) else hexInt(down)