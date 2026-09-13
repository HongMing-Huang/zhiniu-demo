// 知牛 · 设置浮层（SettingsPopover：外观 / 数据源 / 模型 / 免责声明）
// 宽 288，160ms opacity+translateY。显示外观、数据源、模型与风险提示。
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.ThemeMode
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass

/** 设置浮层（外观切换 + 数据源/模型状态 + 免责声明）。 */
fun ViewContainer<*, *>.ThemePopover(
    visible: () -> Boolean,
    left: Float,
    width: Float = 304f,
    topInset: Float = 0f,
    gatewayOnline: () -> Boolean,
    agentReady: () -> Boolean,
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
            absolutePosition(top = 56f + topInset, left = left)
            width(width)
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
                padding(top = 8f, bottom = 10f)
                cssClass("zn-pop")
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            View {
                attr { height(38f); flexDirectionRow(); alignItemsCenter(); padding(left = 14f, right = 14f) }
                Icon(IconKind.USER, 17f)
                View { attr { width(9f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs14); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary)); text("我的")
                    }
                }
            }
            View { attr { height(1f); backgroundColor(colors.c(colors.border)); animate(ANIM_THEME, value = AppTheme.isDark) } }
            // —— 外观 ——
            GroupLabel("外观偏好")
            View {
                attr {
                    margin(left = 14f, right = 14f)
                    height(36f); padding(all = 3f)
                    flexDirectionRow()
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    borderRadius(AppRadius.radius6)
                }
                ThemeSegment(ThemeMode.SYSTEM)
                ThemeSegment(ThemeMode.LIGHT)
                ThemeSegment(ThemeMode.DARK)
            }
            View { attr { marginTop(10f); height(1f); backgroundColor(colors.c(colors.border)); animate(ANIM_THEME, value = AppTheme.isDark) } }
            GroupLabel("服务状态")
            vif({ gatewayOnline() }) {
                ServiceStatus(IconKind.DATA, "行情与资讯", true, "实时网关")
            }
            velse {
                ServiceStatus(IconKind.DATA, "行情与资讯", false, "网关未连接")
            }
            vif({ agentReady() }) {
                ServiceStatus(IconKind.AI, "研究 Agent", true, "模型已配置")
            }
            velse {
                ServiceStatus(IconKind.AI, "研究 Agent", false, "规则降级")
            }
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
                    text("行情与 AI 输出仅供研究参考，不构成投资建议。")
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

private fun ViewContainer<*, *>.ThemeSegment(mode: ThemeMode) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f); height(30f); allCenter()
            borderRadius(AppRadius.radius5)
            backgroundColor(
                if (AppTheme.mode == mode) colors.c(colors.surface)
                else Color.TRANSPARENT
            )
            if (AppTheme.mode == mode) border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            accessibility("外观：${mode.label}")
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 7))
        }
        event { click {
            AppTheme.applyMode(mode)
        } }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                fontWeightMedium()
                color(colors.c(if (AppTheme.mode == mode) colors.textPrimary else colors.textSecondary))
                text(mode.label)
            }
        }
    }
}

/** 真实服务状态行；状态由 /healthz 返回，不把静态说明伪装成可点击设置。 */
private fun ViewContainer<*, *>.ServiceStatus(
    icon: IconKind,
    label: String,
    ok: Boolean,
    desc: String,
) {
    val colors = AppTheme.colors
    View {
        attr {
            height(44f); flexDirectionRow(); alignItemsCenter()
            padding(left = 14f, right = 14f)
            accessibility("$label：$desc")
            accessibilityRole(AccessibilityRole.TEXT)
        }
        Icon(icon, 16f)
        View { attr { width(10f) } }
        View {
            attr { flex(1f) }
            Text {
                attr {
                    fontSize(AppTypography.fs13); fontWeightMedium()
                    color(colors.c(colors.textPrimary)); text(label)
                }
            }
            Text {
                attr {
                    marginTop(2f); fontSize(AppTypography.fs11)
                    color(colors.c(colors.textTertiary)); text(desc)
                }
            }
        }
        View {
            attr {
                width(7f); height(7f); borderRadius(4f)
                backgroundColor(colors.c(if (ok) colors.down else colors.ma5))
            }
        }
    }
}
