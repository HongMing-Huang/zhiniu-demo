// 知牛 · 公共按钮（AppButton + IconButton）
// 风格：克制、黑色 Primary，无大圆角；动画 100ms 背景。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewContainer
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

/** 主按钮：textPrimary 实色 + 反色文字（可选前置图标）。 */
fun ViewContainer<*, *>.PrimaryButton(
    label: String,
    height: Float = 36f,
    icon: IconKind? = null,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            height(height)
            padding(left = 16f, right = 16f)
            borderRadius(AppRadius.radius6)
            backgroundColor(colors.c(colors.textPrimary))
            highlightBackgroundColor(colors.ca(colors.textSecondary, 18))
            alignItemsCenter(); allCenter()
            cssClass("zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        if (icon != null) {
            Icon(icon, 14f) { colors.surface }
            View { attr { width(6f) } }
        }
        Text {
            attr {
                fontSize(AppTypography.fs14); fontWeightSemiBold()
                color(colors.c(colors.surface))
                text(label)
            }
        }
    }
}

/** 次按钮：surface 底 + 1px border（可选前置图标）。 */
fun ViewContainer<*, *>.SecondaryButton(
    label: String,
    height: Float = 36f,
    icon: IconKind? = null,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            height(height)
            padding(left = 14f, right = 14f)
            borderRadius(AppRadius.radius6)
            backgroundColor(colors.c(colors.surface))
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            highlightBackgroundColor(colors.ca(colors.textSecondary, 10))
            alignItemsCenter(); allCenter()
            cssClass("zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        if (icon != null) {
            Icon(icon, 14f) { colors.textSecondary }
            View { attr { width(6f) } }
        }
        Text {
            attr {
                fontSize(AppTypography.fs14)
                color(colors.c(colors.textPrimary))
                text(label)
            }
        }
    }
}

/** 幽灵按钮：透明 + 弱文字（可选前置图标）。 */
fun ViewContainer<*, *>.GhostButton(
    label: String,
    height: Float = 34f,
    icon: IconKind? = null,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            height(height)
            padding(left = 12f, right = 12f)
            borderRadius(AppRadius.radius6)
            backgroundColor(colors.c(colors.surface))
            highlightBackgroundColor(colors.ca(colors.textSecondary, 8))
            alignItemsCenter(); allCenter()
            cssClass("zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        if (icon != null) {
            Icon(icon, 13f) { colors.textSecondary }
            View { attr { width(6f) } }
        }
        Text {
            attr {
                fontSize(AppTypography.fs13)
                color(colors.c(colors.textSecondary))
                text(label)
            }
        }
    }
}

/** 图标按钮：36×36 方块，hover 表面色。 */
fun ViewContainer<*, *>.IconButton(
    kind: IconKind,
    size: Float = 18f,
    box: Float = 36f,
    colorHex: () -> String? = { null },
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val finalColor = colorHex() ?: colors.textSecondary
    View {
        attr {
            width(box); height(box)
            borderRadius(AppRadius.radius6)
            allCenter()
            backgroundColor(com.tencent.kuikly.core.base.Color.TRANSPARENT)
            highlightBackgroundColor(colors.ca(colors.textSecondary, 12))
            cssClass("zn-iconbtn zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        Icon(kind, size) { finalColor }
    }
}


/** 自选按钮：星标切换（调用方传入收藏切换逻辑）。 */
fun ViewContainer<*, *>.FavoriteButton(active: () -> Boolean, height: Float = 34f, onToggle: () -> Unit) {
    val colors = AppTheme.colors
    val isActive = active()
    View {
        attr {
            height(height)
            padding(left = 14f, right = 14f)
            borderRadius(6f)
            flexDirectionRow(); alignItemsCenter()
            backgroundColor(colors.c(colors.surface))
            border(
                com.tencent.kuikly.core.base.Border(
                    1f,
                    com.tencent.kuikly.core.base.BorderStyle.SOLID,
                    colors.c(colors.border),
                )
            )
            highlightBackgroundColor(colors.ca(colors.textSecondary, 8))
            cssClass("zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onToggle() } }
        Icon(
            IconKind.STAR,
            14f,
        ) {
            if (isActive) colors.textPrimary else colors.textTertiary
        }
        View { attr { width(6f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13)
                color(colors.c(if (isActive) colors.textPrimary else colors.textSecondary))
                text(if (isActive) "已自选" else "加自选")
            }
        }
    }
}
