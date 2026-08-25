// 知牛 · 公共组件库（components/common）
// 所有一级页面只允许通过这些公共组件拼装 UI，禁止页面内自造重复实现。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca

/** 1px 弱分割线。 */
fun ViewContainer<*, *>.Divider(marginTop: Float = 0f, marginBottom: Float = 0f) {
    View {
        attr {
            height(1f)
            marginTop(marginTop); marginBottom(marginBottom)
            backgroundColor(AppTheme.colors.c(AppTheme.colors.border))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}

/** 统一卡片：radius 10 / border 1 / padding 16 / 主题动画。 */
fun ViewContainer<*, *>.AppCard(
    padding: Float = 16f,
    content: ViewContainer<*, *>.() -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            backgroundColor(colors().c(colors().surface))
            border(Border(1f, BorderStyle.SOLID, colors().c(colors().borderStrong)))
            borderRadius(AppRadius.r10)
            cssClass("zn-card")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr { padding(all = padding) }
            content()
        }
    }
}

/** 小节标题：标题 + 可选副标题 + 可选右侧操作。 */
fun ViewContainer<*, *>.SectionHeader(
    title: String,
    sub: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = { AppTheme.colors }
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs15); fontWeightSemiBold()
                color(colors().c(colors().textPrimary))
                text(title)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        if (sub != null) {
            Text {
                attr {
                    marginLeft(8f)
                    fontSize(AppTypography.fs12)
                    color(colors().c(colors().textTertiary))
                    text(sub)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        View { attr { flex(1f) } }
        if (action != null && onAction != null) {
            View {
                attr {
                    height(28f); borderRadius(6f); flexDirectionRow(); alignItemsCenter()
                    padding(left = 8f, right = 8f)
                    cssClass("zn-nav zn-click")
                }
                event { click { onAction() } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        color(colors().c(colors().textSecondary))
                        text(action)
                    }
                }
            }
        }
    }
}

/** 状态徽章（弱化背景 + 语义色文字）。 */
fun ViewContainer<*, *>.StatusBadge(label: String, colorHex: () -> String? = { null }) {
    val colors = { AppTheme.colors }
    View {
        attr {
            borderRadius(AppRadius.r6)
            padding(left = 8f, right = 8f)
            height(22f)
            allCenter()
            backgroundColor(colors().ca(colorHex() ?: colors().textSecondary, 12))
        }
        Text {
            attr {
                fontSize(AppTypography.fs11); fontWeightSemiBold()
                color(colors().c(colorHex() ?: colors().textSecondary))
                text(label)
            }
        }
    }
}

/** 指标行：左标签 + 右数值（等宽数字）。 */
fun ViewContainer<*, *>.MetricRow(label: String, value: String, valueHex: () -> String? = { null }, height: Float = 30f) {
    val colors = { AppTheme.colors }
    View {
        attr { flexDirectionRow(); alignItemsCenter(); height(height) }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors().c(colors().textSecondary))
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13); fontWeightMedium()
                fontFamily(NUM_FONT)
                color(colors().c(valueHex() ?: colors().textPrimary))
                text(value)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

/** 指标格：标签在上、数值在下（用于指标网格）。 */
fun ViewContainer<*, *>.QuoteMetric(label: String, value: String, valueHex: () -> String? = { null }) {
    val colors = { AppTheme.colors }
    View {
        attr { width(152f) }
        Text {
            attr {
                fontSize(AppTypography.fs11)
                color(colors().c(colors().textTertiary))
                text(label)
            }
        }
        Text {
            attr {
                marginTop(4f)
                fontSize(AppTypography.fs13); fontWeightMedium()
                fontFamily(NUM_FONT)
                color(colors().c(valueHex() ?: colors().textPrimary))
                text(value)
            }
        }
    }
}

/** 指标网格：一行 N 个等宽 QuoteMetric。 */
fun ViewContainer<*, *>.QuoteMetricGrid(vararg cells: Pair<String, String>) {
    View {
        attr { flexDirectionRow() }
        cells.forEach { (label, value) ->
            View {
                attr { flex(1f) }
                QuoteMetric(label, value)
            }
        }
    }
}

// ---------- 弹层 ----------
/** 页面内 Popover：定位 + 透明度/位移动画（160ms easeOut）。 */
fun ViewContainer<*, *>.AppPopover(
    visible: () -> Boolean,
    width: Float,
    left: Float,
    top: Float = 68f,
    content: ViewContainer<*, *>.() -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr {
            absolutePosition(top = top, left = left)
            width(width)
            zIndex(15)
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (visible()) 0f else -4f))
            animate(Animation.easeOut(0.16f), value = visible())
        }
        View {
            attr {
                backgroundColor(colors().c(colors().elevated))
                border(Border(1f, BorderStyle.SOLID, colors().c(colors().borderStrong)))
                borderRadius(AppRadius.r10)
                cssClass("zn-pop")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            View { attr { padding(bottom = 6f) } }
            content()
            View { attr { padding(bottom = 8f) } }
        }
    }
}

/** 全屏透明拦截层：弹层打开时拦截点击，点击任意处关闭。 */
fun ViewContainer<*, *>.Backdrop(visible: () -> Boolean, onClick: () -> Unit) {
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(Color.TRANSPARENT)
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            animate(Animation.easeOut(0.16f), value = visible())
            zIndex(10)
        }
        event { click { onClick() } }
    }
}

/** 右侧滑入 Drawer（200ms easeOut）。 */
fun ViewContainer<*, *>.AppDrawer(
    visible: () -> Boolean,
    width: Float = 384f,
    onClose: () -> Unit = {},
    content: ViewContainer<*, *>.() -> Unit,
) {
    val colors = { AppTheme.colors }
    // 点击外部关闭
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(Color.TRANSPARENT)
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            animate(Animation.easeOut(0.16f), value = visible())
            zIndex(20)
        }
        event { click { onClose() } }
    }
    View {
        attr {
            width(width)
            absolutePosition(top = 0f, bottom = 0f, right = 0f)
            backgroundColor(colors().c(colors().elevated))
            border(Border(1f, BorderStyle.SOLID, colors().c(colors().borderStrong)))
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetX = if (visible()) 0f else -24f))
            animate(Animation.easeOut(0.2f), value = visible())
            zIndex(21)
        }
        content()
    }
}

// ---------- 反馈态 ----------
/** 骨架条。 */
fun ViewContainer<*, *>.SkeletonBar(width: Float, height: Float, radius: Float = 4f, marginTop: Float = 0f) {
    View {
        attr {
            width(width); height(height)
            borderRadius(radius)
            marginTop(marginTop)
            backgroundColor(AppTheme.colors.c(AppTheme.colors.surfaceSecondary))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
    }
}

/** 表格骨架：rows 行 × 每行 5 段。 */
fun ViewContainer<*, *>.TableSkeleton(rows: Int = 6) {
    View { attr { height(24f) } }
    repeat(rows) {
        View {
            attr { height(64f); flexDirectionRow(); alignItemsCenter() }
            SkeletonBar(220f, 14f)
            View { attr { flex(1f) } }
            SkeletonBar(90f, 14f)
            View { attr { width(24f) } }
            SkeletonBar(90f, 14f)
            View { attr { width(24f) } }
            SkeletonBar(120f, 14f)
            View { attr { width(24f) } }
            SkeletonBar(150f, 14f)
            View { attr { width(24f) } }
            SkeletonBar(110f, 14f)
        }
        Divider()
    }
}

/** 空状态。 */
fun ViewContainer<*, *>.EmptyState(
    icon: IconKind,
    title: String,
    desc: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = { AppTheme.colors }
    View {
        attr { height(260f); allCenter() }
        Icon(icon, 30f, { colors().textTertiary })
        Text {
            attr {
                marginTop(14f)
                fontSize(AppTypography.fs15); fontWeightSemiBold()
                color(colors().c(colors().textPrimary))
                text(title)
            }
        }
        Text {
            attr {
                marginTop(6f)
                fontSize(AppTypography.fs13); lineHeight(20f)
                color(colors().c(colors().textTertiary))
                text(desc)
            }
        }
        if (actionLabel != null && onAction != null) {
            View { attr { height(16f) } }
            SecondaryButton(actionLabel, 34f, onAction)
        }
    }
}

/** 错误状态。 */
fun ViewContainer<*, *>.ErrorState(message: String, onRetry: () -> Unit) {
    val colors = { AppTheme.colors }
    View {
        attr { height(220f); allCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs15); fontWeightSemiBold()
                color(colors().c(colors().textPrimary))
                text(message)
            }
        }
        View { attr { height(14f) } }
        GhostButton("重新加载", 34f, onRetry)
    }
}
