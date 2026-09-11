// 知牛 · 公共输入（AppInput / SearchField）
// AppInput：底部输入条（AI Composer）；SearchField：Header 全局搜索（伪输入，点击打开 StockSearchOverlay）。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.c

/**
 * 文本输入框（圆角容器 + Kuikly Input）。
 * @param height 输入框高（38 标准）。
 */
fun ViewContainer<*, *>.AppInput(
    placeholder: String,
    text: String,
    height: Float = 38f,
    onTextChange: (String) -> Unit,
    onReturn: (() -> Unit)? = null,
    onRef: ((ViewRef<InputView>) -> Unit)? = null,
) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f)
            height(height)
            borderRadius(AppRadius.radius6)
            backgroundColor(colors.c(colors.surfaceSecondary))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            padding(left = 12f, right = 12f)
            overflow(true)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        Input {
            ref { onRef?.invoke(it) }
            attr {
                flex(1f)
                height(height)
                fontSize(AppTypography.fs13)
                color(colors.c(colors.textPrimary))
                text(text)
                placeholder(placeholder)
                placeholderColor(colors.c(colors.textTertiary))
                tintColor(colors.c(colors.textPrimary))
                backgroundColor(Color.TRANSPARENT)
                accessibility(placeholder)
            }
            event {
                textDidChange { params -> onTextChange(params.text) }
                onReturn?.let { inputReturn { it() } }
            }
        }
    }
}

/**
 * Header 全局搜索：伪输入框（240×36），点击后由调用方打开 StockSearchOverlay。
 * Header 是全局入口；Market 工具条只放 IconButton 触发同一 Overlay。
 */
fun ViewContainer<*, *>.SearchField(
    width: Float = 240f,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            width(width); height(36f)
            borderRadius(AppRadius.radius6)
            flexDirectionRow(); alignItemsCenter()
            padding(left = 10f, right = 10f)
            backgroundColor(colors.c(colors.surfaceSecondary))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            highlightBackgroundColor(colors.ca(colors.textSecondary, 8))
            accessibility("搜索股票或代码")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-iconbtn zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        Icon(IconKind.SEARCH, 15f)
        View { attr { width(8f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13)
                color(colors.c(colors.textTertiary))
                text("搜索股票 / 代码")
            }
        }
    }
}
