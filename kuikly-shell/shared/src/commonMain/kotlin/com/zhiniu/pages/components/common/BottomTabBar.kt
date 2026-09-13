// 知牛 · BottomTabBar（手机布局专用：市场 / AI研究 / 待办 底部导航）
// 桌面端导航在 AppHeader；手机端导航职责移交本组件（Android/iOS App 标配交互）。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass

/** 底部导航高度（不含安全区）。 */
const val BOTTOM_TAB_HEIGHT = 54f

/**
 * 手机布局底部导航：3 个 Tab 均分，选中态 2px 顶部指示条 + 主色文字。
 * 流式布局元素（非绝对定位）——ohos 渲染器对 absolutePosition 支持不完整，
 * 由各页面在 body 末尾调用，配合内容区 flex(1f) 贴底。
 * @param activeNav 当前一级导航（"市场" / "AI研究" / "待办"）
 * @param bottomInset 底部安全区（iPhone Home 条 / Android 手势条避让）
 */
fun ViewContainer<*, *>.BottomTabBar(
    activeNav: String,
    bottomInset: Float,
    onNavMarket: () -> Unit,
    onNavAiResearch: () -> Unit,
    onNavWatchlist: () -> Unit,
    onNavAiResearch: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            height(BOTTOM_TAB_HEIGHT + bottomInset)
            flexDirectionRow()
            backgroundColor(colors.c(colors.surface))
            border(
                com.tencent.kuikly.core.base.Border(
                    1f,
                    com.tencent.kuikly.core.base.BorderStyle.SOLID,
                    colors.c(colors.border),
                )
            )
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        BottomTab("行情", IconKind.CHART, activeNav == "市场") { onNavMarket() }
        BottomTab("AI研究", IconKind.CHAT, activeNav == "AI研究") { onNavAiResearch() }
        BottomTab("自选", IconKind.STAR, activeNav == "自选") { onNavWatchlist() }
    }
}

/** 单个底部 Tab：图标 + 文字（欧易式）；选中态 = 图标不透明 + 主色文字 + semibold。 */
private fun ViewContainer<*, *>.BottomTab(label: String, icon: IconKind, active: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f)
            flexDirectionColumn()
            alignItemsCenter()
            justifyContentCenter()
            paddingTop(7f)
            accessibility(if (active) "当前在 $label 页" else "切换到 $label")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
        }
        event { click { onClick() } }
        Icon(icon, 20f)
        View { attr { height(4f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                if (active) fontWeightSemiBold()
                lines(1)
                color(colors.c(if (active) colors.textPrimary else colors.textSecondary))
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}
