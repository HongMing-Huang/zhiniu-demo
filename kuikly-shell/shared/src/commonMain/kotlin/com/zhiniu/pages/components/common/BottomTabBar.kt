// 知牛 · BottomTabBar（手机布局专用：行情 / 自选 / AI研究 / 我的 底部导航）
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
 * 手机布局底部导航：4 个 Tab 均分，图标 + 文字（对标移动端行情 App 标配 4 Tab）。
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
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        BottomTab("行情", IconKind.CHART, activeNav == "市场") { onNavMarket() }
        BottomTab("自选", IconKind.STAR, activeNav == "自选") { onNavWatchlist() }
        // OKX「交易」同款：中央圆形主按钮（AI 生态位，AI accent 底 + 白图标）
        CenterAiButton(active = activeNav == "AI研究", label = "AI研究") { onNavAiResearch() }
        BottomTab("我的", IconKind.USER, activeNav == "我的") { onNavProfile() }
    }
}

/** 中央圆形 AI 主按钮（OKX 参照）：accent 实心圆 + 白图标 + 下方小字；选中态外圈高亮。 */
private fun ViewContainer<*, *>.CenterAiButton(active: Boolean, label: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f)
            flexDirectionColumn()
            alignItemsCenter()
            justifyContentCenter()
            paddingTop(5f)
            accessibility(if (active) "当前在 $label 页" else "切换到 $label")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
        }
        event { click { onClick() } }
        View {
            attr {
                width(34f); height(34f); borderRadius(17f); allCenter()
                // 图标资源为中性灰 PNG：底色用亮色保证灰图标深浅两态对比度
                backgroundColor(colors.c(if (active) colors.aiAccent else colors.surfaceSecondary))
                border(
                    com.tencent.kuikly.core.base.Border(
                        if (active) 2f else 0f,
                        com.tencent.kuikly.core.base.BorderStyle.SOLID,
                        colors.c(colors.aiAccent),
                    )
                )
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            Icon(IconKind.AI, 18f)
        }
        View { attr { height(3f) } }
        Text {
            attr {
                fontSize(AppTypography.fs11)
                if (active) fontWeightSemiBold()
                lines(1)
                color(colors.c(if (active) colors.textPrimary else colors.textSecondary))
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

/** 单个底部 Tab：图标 + 文字；选中态 = 主色文字 + semibold + 顶部 2px 指示条。 */
private fun ViewContainer<*, *>.BottomTab(label: String, icon: IconKind, active: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f)
            flexDirectionColumn()
            alignItemsCenter()
            justifyContentCenter()
            paddingTop(6f)
            accessibility(if (active) "当前在 $label 页" else "切换到 $label")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
        }
        event { click { onClick() } }
        // 选中态顶部短指示条（3px 圆角，避免与文字争焦点）
        View {
            attr {
                width(16f); height(2f); marginBottom(5f)
                borderRadius(allBorderRadius = 1f)
                backgroundColor(colors.c(colors.textPrimary))
                opacity(if (active) 1f else 0f)
                animate(Animation.easeOut(0.14f), value = active)
            }
        }
        Icon(icon, 20f)
        View { attr { height(3f) } }
        Text {
            attr {
                fontSize(AppTypography.fs11)
                if (active) fontWeightSemiBold()
                lines(1)
                color(colors.c(if (active) colors.textPrimary else colors.textSecondary))
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}
