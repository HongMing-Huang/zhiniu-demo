// 知牛 · BottomTabBar（手机布局专用：行情 / 自选 / AI研究 / 我的 底部导航）
// 常规 App 底栏样式：图标随选中态着色 + 文字加粗，无花哨指示条/圆形按钮。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.cssClass

/** 底部导航高度（不含安全区）。 */
const val BOTTOM_TAB_HEIGHT = 54f

/**
 * 手机布局底部导航：4 个 Tab 均分，图标 + 文字。
 * 流式布局元素（非绝对定位）——ohos 渲染器对 absolutePosition 支持不完整，
 * 由各页面在 body 末尾调用，配合内容区 flex(1f) 贴底。
 * @param activeNav 当前一级导航（"市场" / "自选" / "AI研究" / "我的"）
 * @param bottomInset 底部安全区（iPhone Home 条 / Android 手势条避让）
 */
fun ViewContainer<*, *>.BottomTabBar(
    activeNav: String,
    bottomInset: Float,
    onNavMarket: () -> Unit,
    onNavWatchlist: () -> Unit,
    onNavAiResearch: () -> Unit,
    onNavProfile: () -> Unit,
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
        }
        BottomTab("行情", "行情", IconKind.CHART, activeNav == "市场") { onNavMarket() }
        BottomTab("自选", "自选", IconKind.STAR, activeNav == "自选") { onNavWatchlist() }
        BottomTab("AI研究", "AI研究", IconKind.CHAT, activeNav == "AI研究") { onNavAiResearch() }
        BottomTab("我的", "我的", IconKind.USER, activeNav == "我的") { onNavProfile() }
    }
}

/**
 * 单个底部 Tab：图标 + 文字。
 * 选中态：图标染主色 + 文字加粗主色；未选中：灰图标 + 灰文字（常规 App 样式）。
 */
private fun ViewContainer<*, *>.BottomTab(
    label: String,
    a11yLabel: String,
    icon: IconKind,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val fg = if (active) colors.textPrimary else colors.textSecondary
    View {
        attr {
            flex(1f)
            flexDirectionColumn()
            alignItemsCenter()
            justifyContentCenter()
            accessibility(if (active) "当前在 $a11yLabel 页" else "切换到 $a11yLabel")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
        }
        event { click { onClick() } }
        Icon(icon, 21f, tint = fg)
        View { attr { height(4f) } }
        Text {
            attr {
                fontSize(AppTypography.fs10)
                if (active) fontWeightSemiBold()
                lines(1)
                color(colors.c(fg))
                text(label)
            }
        }
    }
}
