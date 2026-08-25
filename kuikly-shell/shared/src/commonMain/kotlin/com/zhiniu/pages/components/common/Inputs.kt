// 知牛 · 公共输入组件（底层 Kuikly Input）
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass

/** 输入框：圆角容器 + Kuikly Input（focus 时 border 加深、表面提亮）。 */
fun ViewContainer<*, *>.AppInput(
    placeholder: String,
    text: String,
    height: Float = 38f,
    focused: Boolean = false,
    onTextChange: (String) -> Unit,
    onReturn: (() -> Unit)? = null,
    autofocus: Boolean = false,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            height(height)
            flex(1f)
            borderRadius(AppRadius.r8)
            backgroundColor(colors().c(if (focused) colors().surface else colors().surfaceSecondary))
            border(Border(1f, BorderStyle.SOLID, colors().c(if (focused) colors().borderStrong else colors().border)))
            overflow(true)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        Input {
            attr {
                height(height)
                fontSize(AppTypography.fs13)
                color(colors().c(colors().textPrimary))
                placeholder(placeholder)
                placeholderColor(colors().c(colors().textTertiary))
                tintColor(colors().c(colors().textPrimary))
                backgroundColor(Color.TRANSPARENT)
                if (autofocus) autofocus(true)
            }
            event {
                textDidChange { params -> onTextChange(params.text) }
                inputReturn {
                    onReturn?.invoke()
                }
            }
        }
    }
}

/** 搜索框：Search 图标 + Input，用于 Header / 弹层。 */
fun ViewContainer<*, *>.SearchField(
    placeholder: String,
    text: String,
    width: Float,
    height: Float = 36f,
    onTextChange: (String) -> Unit,
    onReturn: (() -> Unit)? = null,
    onFocus: (() -> Unit)? = null,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            width(width); height(height)
            borderRadius(AppRadius.r8)
            flexDirectionRow()
            alignItemsCenter()
            padding(left = 10f, right = 10f)
            backgroundColor(colors().c(colors().surfaceSecondary))
            border(Border(1f, BorderStyle.SOLID, colors().c(colors().border)))
            highlightBackgroundColor(colors().ca(colors().textSecondary, 8))
            cssClass("zn-nav zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        Icon(IconKind.SEARCH, 15f, { colors().textTertiary })
        View {
            attr { width(8f) }
        }
        Input {
            attr {
                height(height)
                flex(1f)
                fontSize(AppTypography.fs13)
                color(colors().c(colors().textPrimary))
                placeholder(placeholder)
                placeholderColor(colors().c(colors().textTertiary))
                tintColor(colors().c(colors().textPrimary))
                backgroundColor(Color.TRANSPARENT)
            }
            event {
                textDidChange { params -> onTextChange(params.text) }
                inputReturn { onReturn?.invoke() }
                inputFocus { onFocus?.invoke() }
            }
        }
    }
}

/** 标签文本（输入框左侧辅助）。 */
fun ViewContainer<*, *>.FieldLabel(label: String) {
    val colors = { AppTheme.colors }
    Text {
        attr {
            fontSize(AppTypography.fs12)
            color(colors().c(colors().textSecondary))
            text(label)
        }
    }
}
