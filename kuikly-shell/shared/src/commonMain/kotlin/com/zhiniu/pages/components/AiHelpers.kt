/* 知牛 · 公共 AI 块级助手（AiMessageHeader） */
package com.zhiniu.pages.components

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** AI 消息头部（豆包/ChatGPT 式）：accent 实心圆头像 + 主色加粗标签，消息身份锚点。 */
fun ViewContainer<*, *>.AiMessageHeader(label: String) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        View {
            attr {
                width(24f); height(24f); borderRadius(12f); allCenter()
                backgroundColor(colors.c(colors.aiAccent))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            Icon(IconKind.AI, 14f)
        }
        Text {
            attr {
                marginLeft(8f)
                fontSize(AppTypography.fs13); fontWeightSemiBold()
                color(colors.c(colors.textPrimary))
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}
