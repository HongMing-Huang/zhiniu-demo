// 知牛 · 设置浮层（SettingsPopover：外观 / 数据源 / 模型 / 免责声明）
// 宽 220，160ms opacity+translateY。数据源与模型为演示态（接入中标识），保留视觉占位。
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
import com.zhiniu.pages.components.ThemeMode
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass

/** 设置浮层（外观切换 + 数据源/模型演示态 + 免责声明）。 */
fun ViewContainer<*, *>.ThemePopover(
    visible: () -> Boolean,
    left: Float,
    onClose: () -> Unit,
) {
    val colors = AppTheme.colors
    // 全屏透明拦截（点击空白处关闭）
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(Color.TRANSPARENT)
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            animate(Animation.easeOut(0.16f), value = visible())
            zIndex(10)
        }
        event { click { onClose() } }
    }
    // 面板
    View {
        attr {
            absolutePosition(top = 56f, left = left)
            width(220f)
            zIndex(15)
            touchEnable(visible())
            opacity(if (visible()) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (visible()) 0f else -4f))
            animate(Animation.easeOut(0.16f), value = visible())
        }
        View {
            attr {
                backgroundColor(colors.c(colors.elevated))
                border(Border(1f, BorderStyle.SOLID, colors.c(colors.borderStrong)))
                borderRadius(AppRadius.radius8)
                padding(top = 4f, bottom = 4f)
                cssClass("zn-pop")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            // —— 外观 ——
            GroupLabel("外观")
            ThemeOption(ThemeMode.SYSTEM, onPicked = onClose)
            ThemeOption(ThemeMode.LIGHT, onPicked = onClose)
            ThemeOption(ThemeMode.DARK, onPicked = onClose)
            // —— 数据源 ——
            View { attr { marginTop(6f); height(1f); backgroundColor(colors.c(colors.border)); animate(ANIM_THEME, value = AppTheme.isDark) } }
            GroupLabel("数据源")
            SetOption(label = "演示行情", active = true)
            SetOption(label = "实时行情", disabled = true, desc = "接入中")
            // —— 模型 ——
            View { attr { marginTop(6f); height(1f); backgroundColor(colors.c(colors.border)); animate(ANIM_THEME, value = AppTheme.isDark) } }
            GroupLabel("AI 模型")
            SetOption(label = "知牛-1（演示）", active = true)
            SetOption(label = "多模型网关", disabled = true, desc = "接入中")
            // —— 免责声明 ——
            View { attr { marginTop(6f); height(1f); backgroundColor(colors.c(colors.border)); animate(ANIM_THEME, value = AppTheme.isDark) } }
            Text {
                attr {
                    marginLeft(14f); marginTop(8f)
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textSecondary))
                    text("免责声明")
                }
            }
            Text {
                attr {
                    marginLeft(14f); marginRight(14f); marginTop(3f); marginBottom(8f)
                    fontSize(AppTypography.fs11); lineHeight(16f)
                    color(colors.c(colors.textTertiary))
                    text("行情与 AI 输出均为演示数据，不构成投资建议。")
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.GroupLabel(label: String) {
    val colors = AppTheme.colors
    Text {
        attr {
            marginLeft(14f); marginTop(8f); marginBottom(4f)
            fontSize(AppTypography.fs11)
            color(colors.c(colors.textTertiary))
            text(label)
        }
    }
}

private fun ViewContainer<*, *>.ThemeOption(mode: ThemeMode, onPicked: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(32f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
            cssClass("zn-row zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 7))
        }
        event { click {
            AppTheme.setMode(mode)
            onPicked()
        } }
        // 选中圆点
        OptionDot(active = AppTheme.mode == mode)
        View { attr { width(10f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13)
                color(colors.c(if (AppTheme.mode == mode) colors.textPrimary else colors.textSecondary))
                text(mode.label)
            }
        }
    }
}

/** 设置行：圆点 + 标签（+ 可选中/禁用态）。 */
private fun ViewContainer<*, *>.SetOption(
    label: String,
    active: Boolean = false,
    disabled: Boolean = false,
    desc: String = "",
) {
    val colors = AppTheme.colors
    View {
        attr {
            height(32f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
            if (!disabled) cssClass("zn-row zn-click")
            if (!disabled) highlightBackgroundColor(colors.ca(colors.textSecondary, 7))
            opacity(if (disabled) 0.55f else 1f)
        }
        OptionDot(active = active)
        View { attr { width(10f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13)
                color(colors.c(if (active) colors.textPrimary else colors.textSecondary))
                text(label)
            }
        }
        if (desc.isNotEmpty()) {
            View { attr { flex(1f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs11)
                    color(colors.c(colors.textTertiary))
                    text(desc)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.OptionDot(active: Boolean) {
    val colors = AppTheme.colors
    View {
        attr {
            width(12f); height(12f)
            borderRadius(allBorderRadius = 6f)
            border(Border(1.2f, BorderStyle.SOLID, colors.c(colors.textSecondary)))
            alignItemsCenter(); allCenter()
        }
        if (active) {
            View {
                attr {
                    width(6f); height(6f)
                    borderRadius(allBorderRadius = 3f)
                    backgroundColor(colors.c(colors.textPrimary))
                }
            }
        }
    }
}