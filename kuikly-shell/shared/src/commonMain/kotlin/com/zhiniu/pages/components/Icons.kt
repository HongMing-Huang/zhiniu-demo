// 知牛 · 线性图标（Tencent TDesign Icons · 本地 PNG 资源）
// 来源：Tencent/tdesign-icons develop 分支 svg/ 目录（详见 docs/REFERENCE.md）。
// 渲染：Kuikly Image + 本地 assets（H5 走 assets://common/ 协议，原生端 ImageView 自带 tint）。
// 颜色：tintColor 按主题色，主题切换通过 animate(ANIM_THEME) 自动更新。
package com.zhiniu.pages.components

import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Image

/** 图标枚举（asset 字段为 TDesign 官方 svg 目录真实文件名的 PNG 映射）。 */
enum class IconKind(val asset: String) {
    SEARCH("icons/search.png"),
    BACK("icons/back.png"),
    STAR("icons/star.png"),
    STAR_FILLED("icons/star_filled.png"),
    FILTER("icons/filter.png"),
    THEME("icons/theme.png"),
    CLOSE("icons/close.png"),
    SEND("icons/send.png"),
    AI("icons/ai.png"),
    MORE("icons/more.png"),
    CHART("icons/chart.png"),
    CHEVRON_DOWN("icons/chevron_down.png"),
    CHEVRON_UP("icons/chevron_up.png");
}

/**
 * 渲染线性图标。
 * @param colorHex 主题色 hex 字符串（如 #171A1F）；null 时使用 PNG 原始色（黑）。
 * 主题切换时通过 `animate(ANIM_THEME, value = AppTheme.isDark)` 自动重新染色。
 */
fun ViewContainer<*, *>.Icon(
    kind: IconKind,
    size: Float = 18f,
    colorHex: () -> String? = { null },
) {
    Image({
        attr {
            width(size)
            height(size)
            src(ImageUri.commonAssets(kind.asset))
            colorHex()?.let { tintColor(AppTheme.colors.c(it)) }
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    })
}
