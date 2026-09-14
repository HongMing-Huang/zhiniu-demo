// 知牛 · 公共基础组件（Divider / EmptyState / ErrorState / ActivityIndicatorRow / SectionHeader / AppCard）
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.ActivityIndicator
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass

/** 1px 水平分割线。 */
fun ViewContainer<*, *>.Divider() {
    View {
        attr {
            height(1f)
            backgroundColor(AppTheme.colors.c(AppTheme.colors.border))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}

/** 卡片外壳：1px border + radius 8；不带 hover lift。 */
fun ViewContainer<*, *>.AppCard(
    padding: Float = 16f,
    content: ViewContainer<*, *>.() -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            backgroundColor(colors.c(colors.surface))
            border(
                com.tencent.kuikly.core.base.Border(
                    1f,
                    com.tencent.kuikly.core.base.BorderStyle.SOLID,
                    colors.c(colors.border),
                )
            )
            borderRadius(AppRadius.radius8)
            cssClass("zn-card")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View { attr { padding(all = padding) }; content() }
    }
}

/** 小节标题：16/600 + 右侧 slot。 */
fun ViewContainer<*, *>.SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs16); fontWeightSemiBold()
                color(colors.c(colors.textPrimary))
                text(title)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { flex(1f) } }
        if (action != null && onAction != null) {
            View {
                attr {
                    height(28f); borderRadius(6f); padding(left = 10f, right = 10f)
                    allCenter()
                    highlightBackgroundColor(colors.ca(colors.textSecondary, 8))
                    cssClass("zn-click")
                }
                event { click { onAction() } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textSecondary))
                        text(action)
                    }
                }
            }
        }
    }
}

/** 空状态：标题 + 描述 + 可选 Action。 */
fun ViewContainer<*, *>.EmptyState(
    title: String,
    desc: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    View {
        attr { height(240f); allCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs15); fontWeightSemiBold()
                color(colors.c(colors.textPrimary))
                text(title)
            }
        }
        View { attr { height(6f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13); lineHeight(20f)
                color(colors.c(colors.textTertiary))
                text(desc)
            }
        }
        if (actionLabel != null && onAction != null) {
            View { attr { height(14f) } }
            GhostButton(actionLabel, 34f, onClick = onAction)
        }
    }
}

/** 指标格：标签在上（fs10）、数值在下（fs14 semibold 等宽），OKX 式排版。 */
fun ViewContainer<*, *>.QuoteMetric(label: String, value: String) {
    val colors = AppTheme.colors
    View {
        attr { width(152f) }
        Text {
            attr {
                fontSize(AppTypography.fs10)
                color(colors.c(colors.textTertiary))
                text(label)
            }
        }
        Text {
            attr {
                marginTop(4f)
                fontSize(AppTypography.fs14); fontWeightSemiBold()
                fontFamily(com.zhiniu.pages.components.NUM_FONT)
                color(colors.c(colors.textPrimary))
                text(value)
            }
        }
    }
}

/** AI 流式加载行。 */
fun ViewContainer<*, *>.ActivityIndicatorRow(label: String) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        ActivityIndicator {
            attr { width(14f); height(14f); isGrayStyle(true) }
        }
        Text {
            attr {
                marginLeft(8f)
                fontSize(AppTypography.fs13)
                color(colors.c(colors.textSecondary))
                text(label)
            }
        }
    }
}

/** 骨架条（surfaceSecondary 背景，等宽占位）。 */
fun ViewContainer<*, *>.SkeletonBar(width: Float, height: Float) {
    val colors = AppTheme.colors
    View {
        attr {
            width(width); height(height)
            borderRadius(allBorderRadius = 4f)
            backgroundColor(colors.c(colors.surfaceSecondary))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}
