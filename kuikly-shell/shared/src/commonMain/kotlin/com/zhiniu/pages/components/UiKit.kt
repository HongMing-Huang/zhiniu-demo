// 知牛 · 通用 UI 原语（Kuikly DSL）
// 仅使用 core.* 已验证 API；颜色一律从 ThemeState.palette 取。
// 悬停：H5 通过 cssClass + host CSS :hover（120ms）实现；原生端退化为点击态高亮。
package com.zhiniu.pages.components

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** web-host CSS 钩子：类名会被加进 DOM，host 的 :hover / transition CSS 生效。 */
fun Attr.cssClass(value: String) {
    "cssClass" with value
}

/** 1px 弱分割线。 */
fun ViewContainer<*, *>.Hdiv(marginTop: Float = 0f, marginBottom: Float = 0f) {
    View {
        attr {
            height(1f)
            marginTop(marginTop); marginBottom(marginBottom)
            backgroundColor(ThemeState.palette.c(ThemeState.palette.border))
            animate(Animation.easeOut(0.18f), value = ThemeState.isDark)
        }
    }
}

/** 32x32 图标按钮（hover 灰底、cursor 手指、按下高亮）。 */
fun ViewContainer<*, *>.IconButton(
    kind: IconKind,
    size: Float = 18f,
    onClick: () -> Unit,
    iconColorHex: String? = null,
    testTag: String = "",
) {
    View {
        attr {
            width(34f); height(34f)
            borderRadius(8f)
            allCenter()
            backgroundColor(Color.TRANSPARENT)
            highlightBackgroundColor(ThemeState.palette.ca(ThemeState.palette.textSecondary, 12))
            cssClass("zn-iconbtn zn-click")
        }
        event { click { onClick() } }
        Icon(kind, size, iconColorHex)
    }
}

/** 主色文本按钮（普通状态文字色 + hover 灰底）。 */
fun ViewContainer<*, *>.TextButton(
    label: String,
    fontSize: Float = 14f,
    colorHex: String? = null,
    height: Float = 34f,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(height)
            borderRadius(8f)
            allCenter()
            backgroundColor(Color.TRANSPARENT)
            cssClass("zn-iconbtn zn-click")
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(fontSize)
                color(ThemeState.palette.c(colorHex ?: ThemeState.palette.textSecondary))
                text(label)
                animate(Animation.easeOut(0.18f), value = ThemeState.isDark)
            }
        }
    }
}

/** 描边按钮（次要操作）：高 32-36，1px border。 */
fun ViewContainer<*, *>.OutlineButton(
    label: String,
    height: Float = 34f,
    onClick: () -> Unit,
    accentHex: String? = null,
) {
    View {
        attr {
            height(height)
            border(Border(1f, BorderStyle.SOLID, ThemeState.palette.c(ThemeState.palette.borderStrong)))
            borderRadius(8f)
            allCenter()
            backgroundColor(ThemeState.palette.c(ThemeState.palette.surface))
            highlightBackgroundColor(ThemeState.palette.ca(ThemeState.palette.textSecondary, 10))
            animate(Animation.easeOut(0.18f), value = ThemeState.isDark)
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(13f)
                color(ThemeState.palette.c(accentHex ?: ThemeState.palette.textPrimary))
                text(label)
                animate(Animation.easeOut(0.18f), value = ThemeState.isDark)
            }
        }
    }
}

/** 主色实心按钮（AI 等强调操作）。 */
fun ViewContainer<*, *>.SolidButton(
    label: String,
    height: Float = 36f,
    bgHex: String,
    textHex: String,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(height)
            borderRadius(8f)
            allCenter()
            backgroundColor(ThemeState.palette.c(bgHex))
            highlightBackgroundColor(ThemeState.palette.ca(bgHex, 78))
            animate(Animation.easeOut(0.18f), value = ThemeState.isDark)
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(14f)
                color(ThemeState.palette.c(textHex))
                text(label)
                animate(Animation.easeOut(0.18f), value = ThemeState.isDark)
            }
        }
    }
}

/** 带标题和「→」的卡片标题行。 */
fun ViewContainer<*, *>.CardTitleRow(title: String, onMore: (() -> Unit)? = null) {
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(15f); fontWeightSemiBold()
                color(ThemeState.palette.c(ThemeState.palette.textPrimary))
                text(title)
                animate(Animation.easeOut(0.18f), value = ThemeState.isDark)
            }
        }
        if (onMore != null) {
            View { attr { flex(1f) } }
            View {
                attr { flexDirectionRow(); cssClass("zn-iconbtn zn-click"); borderRadius(6f) }
                event { click { onMore() } }
                Text {
                    attr {
                        fontSize(12f)
                        color(ThemeState.palette.c(ThemeState.palette.textTertiary))
                        text("查看全部")
                    }
                }
            }
        }
    }
}

/** 弹层容器：无布局占位（absolutePosition 由调用方设定），统一透明度/位移动画。 */
fun ViewContainer<*, *>.PopoverShell(
    visible: Boolean,
    init: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            backgroundColor(ThemeState.palette.c(ThemeState.palette.elevated))
            border(Border(1f, BorderStyle.SOLID, ThemeState.palette.c(ThemeState.palette.border)))
            borderRadius(10f)
            opacity(if (visible) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (visible) 0f else -4f))
            touchEnable(visible)
            animate(Animation.easeOut(0.16f), value = visible)
        }
        init()
    }
}

/** 全屏透明点击拦截层（用于关闭弹层）。 */
fun ViewContainer<*, *>.Backdrop(visible: Boolean, onClick: () -> Unit) {
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(Color.TRANSPARENT)
            touchEnable(visible)
            opacity(if (visible) 1f else 0f)
            animate(Animation.easeOut(0.16f), value = visible)
            zIndex(10)
        }
        event { click { onClick() } }
    }
}
