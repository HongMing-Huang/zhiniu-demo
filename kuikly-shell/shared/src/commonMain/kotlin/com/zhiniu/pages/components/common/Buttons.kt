// 知牛 · 公共按钮（底层使用 Kuikly 官方 Button 组件；图标类使用 View+Canvas）
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca

/** 主按钮：深底白字（Light 反转于 Dark）。用于 AI 分析 / 发送。 */
fun ViewContainer<*, *>.PrimaryButton(label: String, height: Float = 36f, onClick: () -> Unit) {
    val colors = { AppTheme.colors }
    val bg = if (AppTheme.isDark) colors().textPrimary else colors().textPrimary
    val fg = if (AppTheme.isDark) "#17191C" else "#FFFFFF"
    Button {
        attr {
            height(height)
            backgroundColor(colors().c(bg))
            highlightBackgroundColor(colors().ca(colors().textSecondary, 26))
            borderRadius(AppRadius.r8)
            padding(left = 16f, right = 16f)
            titleAttr {
                fontSize(AppTypography.fs14); fontWeightSemiBold()
                color(colors().c(fg))
                text(label)
            }
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event {
            click { onClick() }
        }
    }
}

/** 次按钮：表面 + 描边。用于 加自选。 */
fun ViewContainer<*, *>.SecondaryButton(label: String, height: Float = 36f, onClick: () -> Unit) {
    val colors = { AppTheme.colors }
    Button {
        attr {
            height(height)
            backgroundColor(colors().c(colors().surface))
            border(Border(1f, BorderStyle.SOLID, colors().c(colors().borderStrong)))
            highlightBackgroundColor(colors().ca(colors().textSecondary, 10))
            borderRadius(AppRadius.r8)
            padding(left = 14f, right = 14f)
            titleAttr {
                fontSize(AppTypography.fs14)
                color(colors().c(colors().textPrimary))
                text(label)
            }
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event {
            click { onClick() }
        }
    }
}

/** 幽灵按钮：透明 + 弱文字。用于 筛选 / 更多。 */
fun ViewContainer<*, *>.GhostButton(label: String, height: Float = 34f, onClick: () -> Unit) {
    val colors = { AppTheme.colors }
    Button {
        attr {
            height(height)
            borderRadius(AppRadius.r8)
            padding(left = 12f, right = 12f)
            titleAttr {
                fontSize(AppTypography.fs13)
                color(colors().c(colors().textSecondary))
                text(label)
            }
            cssClass("zn-iconbtn")
        }
        event {
            click { onClick() }
        }
    }
}

/** 图标按钮（Canvas 线性图标，16/18/20px）。 */
fun ViewContainer<*, *>.IconButton(
    kind: IconKind,
    size: Float = 18f,
    height: Float = 36f,
    onClick: () -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            height(height); width(height)
            borderRadius(AppRadius.r8)
            allCenter()
            backgroundColor(Color.TRANSPARENT)
            highlightBackgroundColor(colors().ca(colors().textSecondary, 10))
            cssClass("zn-iconbtn zn-click")
        }
        event { click { onClick() } }
        Icon(kind, size, { colors().textSecondary })
    }
}

/** 自选按钮：星标切换（Canvas 绘制，非 Emoji）。 */
fun ViewContainer<*, *>.FavoriteButton(
    subscribed: () -> Boolean,
    height: Float = 34f,
    onToggle: () -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            height(height)
            borderRadius(AppRadius.r8)
            flexDirectionRow()
            alignItemsCenter()
            padding(left = 12f, right = 12f)
            border(Border(1f, BorderStyle.SOLID, colors().c(colors().borderStrong)))
            backgroundColor(colors().c(colors().surface))
            highlightBackgroundColor(colors().ca(colors().textSecondary, 8))
            cssClass("zn-nav zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onToggle() } }
        Icon(
            IconKind.STAR, 15f,
            { if (subscribed()) colors().textPrimary else colors().textTertiary },
            filled = subscribed(),
        )
        Text {
            attr {
                marginLeft(6f)
                fontSize(AppTypography.fs13)
                color(colors().c(if (subscribed()) colors().textPrimary else colors().textSecondary))
                text(if (subscribed()) "已自选" else "加自选")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}
