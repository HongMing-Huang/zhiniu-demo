// 知牛 · GlobalHeader（所有一级页面共用，禁止页面自造 Header）
// activeNav 以 () -> String 惰性读取，保证导航切换时 indicator/文字即时响应。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass

/**
 * 全局 Header：64px，左品牌 + 一级导航（底部 2px indicator）+ 右侧搜索/主题/设置。
 * 所有一级页面使用同一个实例；导航切换仅改状态，不创建页面。
 */
fun ViewContainer<*, *>.GlobalHeader(
    navs: List<String>,
    activeNav: () -> String,
    contentWidth: Float,
    onNav: (String) -> Unit,
    onSearchFocus: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTheme: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            height(64f)
            flexDirectionRow()
            justifyContentCenter()
            backgroundColor(colors().c(colors().surface))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr {
                width(contentWidth)
                flexDirectionRow()
                alignItemsCenter()
                padding(left = 32f, right = 32f)
            }
            // 品牌
            Text {
                attr {
                    fontSize(AppTypography.fs20); fontWeightSemiBold()
                    color(colors().c(colors().textPrimary))
                    text("知牛")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { width(32f) } }
            // 一级导航
            navs.forEach { nav -> navItem(nav, { nav == activeNav() }) { onNav(nav) } }
            View { attr { flex(1f) } }
            // 搜索（真实 Input；聚焦弹出搜索面板）
            SearchField(
                placeholder = "搜索股票 / 代码",
                text = "",
                width = 240f,
                onTextChange = onSearchChange,
            ) {}
            View { attr { width(10f) } }
            IconButton(IconKind.SUN, 18f, 36f, onClick = onTheme)
            IconButton(IconKind.SETTINGS, 18f, 36f, onClick = onSettings)
        }
    }
}

private fun ViewContainer<*, *>.navItem(nav: String, active: () -> Boolean, onClick: () -> Unit) {
    val colors = { AppTheme.colors }
    View {
        attr {
            padding(left = 14f, right = 14f)
            alignSelfStretch()
            cssClass("zn-nav zn-click")
            highlightBackgroundColor(colors().ca(colors().textSecondary, 8))
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(AppTypography.fs14); fontWeightMedium()
                color(colors().c(if (active()) colors().textPrimary else colors().textSecondary))
                text(nav)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View {
            attr {
                absolutePosition(top = 61f, left = 14f, right = 14f)
                height(2f)
                borderRadius(allBorderRadius = 1f)
                backgroundColor(colors().c(colors().textPrimary))
                opacity(if (active()) 1f else 0f)
                animate(Animation.easeOut(0.16f), value = active())
            }
        }
    }
}
