// 知牛 · 公共按钮（AppButton + IconButton）
// 风格：克制、黑色 Primary，无大圆角；动画 100ms 背景。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.attr.AccessibilityRole
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
            flexDirectionRow(); alignItemsCenter(); allCenter()
            accessibility(label)
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        if (icon != null) {
            Icon(icon, 14f)
            View { attr { width(6f) } }
        }
        Text {
            attr {
                fontSize(AppTypography.fs14); fontWeightSemiBold(); lines(1)
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
            flexDirectionRow(); alignItemsCenter(); allCenter()
            accessibility(label)
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        if (icon != null) {
            Icon(icon, 14f)
            View { attr { width(6f) } }
        }
        Text {
            attr {
                fontSize(AppTypography.fs14); lines(1)
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
            flexDirectionRow(); alignItemsCenter(); allCenter()
            accessibility(label)
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        if (icon != null) {
            Icon(icon, 13f)
            View { attr { width(6f) } }
        }
        Text {
            attr {
                fontSize(AppTypography.fs13); lines(1)
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
    active: Boolean = false,
    accessibilityLabel: String = kind.name,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            width(box); height(box)
            borderRadius(AppRadius.radius6)
            allCenter()
            backgroundColor(
                if (active) colors.c(colors.surfaceHover)
                else com.tencent.kuikly.core.base.Color.TRANSPARENT
            )
            highlightBackgroundColor(colors.ca(colors.textSecondary, 12))
            accessibility(accessibilityLabel)
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-iconbtn zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        Icon(kind, size)
    }
}

/** 圆形图标按钮（Composer 发送 / 移动端主操作）；tone=primary accent 实心，quiet 浅底灰图标。 */
fun ViewContainer<*, *>.RoundIconButton(
    kind: IconKind,
    size: Float = 17f,
    box: Float = 38f,
    enabled: Boolean = true,
    tone: String = "primary",
    accessibilityLabel: String = kind.name,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            width(box); height(box)
            borderRadius(box / 2f)
            allCenter()
            // 启用：primary=AI accent 实心 / quiet=浅底；禁用：幽灵描边（无灰实心块的笨重感）
            backgroundColor(
                colors.c(
                    when {
                        !enabled -> colors.surface
                        tone == "quiet" -> colors.surfaceSecondary
                        else -> colors.aiAccent
                    }
                )
            )
            border(
                if (!enabled) com.tencent.kuikly.core.base.Border(1f, com.tencent.kuikly.core.base.BorderStyle.SOLID, colors.c(colors.borderStrong))
                else com.tencent.kuikly.core.base.Border(0f, com.tencent.kuikly.core.base.BorderStyle.SOLID, com.tencent.kuikly.core.base.Color.TRANSPARENT)
            )
            opacity(if (enabled) 1f else 0.7f)
            highlightBackgroundColor(colors.ca(colors.textSecondary, 18))
            accessibility(if (enabled) accessibilityLabel else "$accessibilityLabel（不可用）")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = enabled, longClickable = false)
            cssClass("zn-iconbtn zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { if (enabled) onClick() } }
        Icon(kind, size, tint = if (tone == "quiet" || !enabled) colors.textSecondary else null)
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
            accessibility(if (isActive) "移出自选" else "加入自选")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onToggle() } }
        Icon(if (isActive) IconKind.STAR_FILLED else IconKind.STAR, 14f)
        View { attr { width(6f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13); lines(1)
                color(colors.c(if (isActive) colors.textPrimary else colors.textSecondary))
                text(if (isActive) "已自选" else "加自选")
            }
        }
    }
}
