// 知牛 · AppHeader（所有页面共用；导航切换走 SPA Router）
// 64px / max-width 1360 / padding 0 32 / Logo 20/600 / 选中 2px 黑色 indicator（无背景块）。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.base.attr.ImageUri
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppSpacing
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass

/**
 * 全局 Header：64px，1px 底描边。
 * @param activeNav 当前一级导航（"市场" / "AI研究"）。
 * @param onSearch 点击 Header 搜索 → 打开 StockSearchOverlay。
 * @param onTheme  点击主题按钮 → 打开 ThemePopover。
 */
fun ViewContainer<*, *>.AppHeader(
    activeNav: String,
    contentWidth: Float,
    topInset: Float = 0f,
    onNavMarket: () -> Unit,
    onNavAiResearch: () -> Unit,
    onSearch: () -> Unit,
    onTheme: () -> Unit,
) {
    val colors = AppTheme.colors
    val compact = contentWidth <= 760f
    val sidePad = if (compact) 16f else 32f
    View {
        attr {
            height(AppSpacing.header + topInset)
            flexDirectionRow(); justifyContentCenter()
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
        View {
            attr {
                width(contentWidth)
                flexDirectionRow(); alignItemsCenter()
                padding(top = topInset, left = sidePad, right = sidePad)
            }
            // 品牌位：官方提供的红牛头 Logo 标（透明底，深浅色通用）+ 文字标识
            View {
                attr { flexDirectionRow(); alignItemsCenter() }
                Image {
                    attr {
                        size(24f, 24f)
                        src(ImageUri.commonAssets("brand/logo-mark.png"))
                        marginRight(if (compact) 5f else 7f)
                    }
                }
                Text {
                    attr {
                        fontSize(AppTypography.fs20); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text("知牛")
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
            }
            View { attr { width(if (compact) 12f else 28f) } }
            // 导航
            HeaderNav("市场", activeNav == "市场", onClick = onNavMarket)
            HeaderNav("AI研究", activeNav == "AI研究", onClick = onNavAiResearch)
            View { attr { flex(1f) } }
            // 全局搜索（点击打开 Overlay）
            SearchField(width = if (compact) 120f else 240f, onClick = onSearch)
            View { attr { width(if (compact) 8f else 10f) } }
            SecondaryButton("设置", height = 36f, icon = IconKind.THEME, onClick = onTheme)
        }
    }
}

/** Header 一级导航项：文字 + 选中 2px 底部 indicator（140ms）。 */
private fun ViewContainer<*, *>.HeaderNav(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            width(if (label == "AI研究") 72f else 58f)
            height(AppSpacing.header)
            alignSelfStretch()
            accessibility(label)
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
        }
        event { click { onClick() } }
        View {
            attr { flexDirectionColumn(); alignItemsCenter(); justifyContentCenter(); flex(1f) }
            Text {
                attr {
                    width(if (label == "AI研究") 48f else 32f); textAlignCenter()
                    fontSize(AppTypography.fs14)
                    fontWeight600(); lines(1); textOverFlowClip()
                    color(colors.c(if (active) colors.textPrimary else colors.textSecondary))
                    text(label)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(8f) } }
            // 2px 底部 indicator（无背景块）
            View {
                attr {
                    height(2f)
                    width(20f)
                    borderRadius(allBorderRadius = 1f)
                    backgroundColor(colors.c(colors.textPrimary))
                    opacity(if (active) 1f else 0f)
                    animate(Animation.easeOut(0.14f), value = active)
                }
            }
        }
    }
}
