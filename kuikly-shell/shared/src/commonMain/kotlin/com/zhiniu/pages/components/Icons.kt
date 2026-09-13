// 知牛 · 线性图标（Tencent TDesign Icons · 本地 PNG 资源）
// 来源：Tencent/tdesign-icons develop 分支 svg/ 目录（详见 docs/REFERENCE.md）。
// 渲染：Kuikly Image + 本地透明 PNG（H5 走 assets://common/ 协议）。
// 颜色：资源预渲染为中性灰，避免 H5 SVG tint 把外部 PNG 的边界框错误填满。
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
    CHEVRON_UP("icons/chevron_up.png"),
    CHEVRON_RIGHT("icons/chevron_right.png"),
    CALENDAR("icons/calendar.png"),
    CHECK("icons/check.png"),
    FILTER_SORT("icons/filter-sort.png"),
    ERROR("icons/error-triangle.png"),
    CHAT("icons/chat-message.png"),
    DATA("icons/data-display.png"),
    USER("icons/user-circle.png"),
    REFRESH("icons/refresh.png"),
    BELL("icons/bell.png"),
    INFO("icons/info.png"),
    SETTING("icons/setting.png");
}

/**
 * 渲染线性图标。
 * 图标资源统一使用可跨明暗主题辨识的中性灰；交互状态由按钮背景与文案表达。
 */
fun ViewContainer<*, *>.Icon(
    kind: IconKind,
    size: Float = 18f,
) {
    Image({
        attr {
            width(size)
            height(size)
            src(ImageUri.commonAssets(kind.asset))
        }
    })
}
