/* 知牛 · 公共 AI 块级助手（AiMessageHeader / DemoDataNote） */
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
        Icon(IconKind.AI, 13f) { colors.aiAccent }
        Text {
            attr {
                marginLeft(6f)
                fontSize(AppTypography.fs12); fontWeightSemiBold()
                color(colors.c(colors.aiAccent))
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

/** AI 回复底部"以上为 Demo 行情 ..."标注。 */
fun ViewContainer<*, *>.DemoDataNote() {
    Text {
        attr {
            fontSize(AppTypography.fs11)
            color(AppTheme.colors.c(AppTheme.colors.textTertiary))
            text("以上为 Demo 行情与 AI 演示输出，不构成投资建议")
        }
    }
}
