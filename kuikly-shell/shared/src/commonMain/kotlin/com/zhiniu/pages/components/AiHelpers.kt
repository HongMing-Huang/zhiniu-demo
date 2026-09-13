/* 知牛 · 公共 AI 块级助手（AiMessageHeader） */
package com.zhiniu.pages.components

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.c

/** AI 消息气泡头部（AI 图标 + 标签；AI 区域用 aiAccent 强调，少量点缀）。 */
fun ViewContainer<*, *>.AiMessageHeader(label: String) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Icon(IconKind.AI, 13f)
        Text {
            attr {
                marginLeft(6f)
                fontSize(AppTypography.fs12); fontWeightSemiBold()
                color(colors.c(colors.textSecondary))
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}
