// 知牛 · 公共 Tabs（AppTabs：下划线指示条；SegmentedTabs：分段胶囊）
package com.zhiniu.pages.components.common

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.c

/**
 * 下划线 Tab 条：固定槽宽 + 底部 2px 指示条（160ms easeOut 滑动）。
 * 用于市场 Tab / 详情周期 / 详情底部 Tab。
 */
fun ViewContainer<*, *>.AppTabs(
    tabs: List<String>,
    selected: () -> String,
    itemWidth: Float = 64f,
    height: Float = 44f,
    onSelect: (String) -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr { flexDirectionRow(); alignItemsFlexEnd(); height(height) }
        tabs.forEach { t ->
            View {
                attr {
                    width(itemWidth); height(height)
                    alignItemsCenter(); justifyContentCenter()
                    cssClass("zn-nav zn-click")
                }
                event { click { onSelect(t) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs14)
                        fontWeight600()
                        color(colors().c(if (selected() == t) colors().textPrimary else colors().textTertiary))
                        text(t)
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
            }
        }
        // 2px 指示条
        View {
            attr {
                absolutePosition(top = height - 2f, left = 20f)
                width(24f); height(2f)
                borderRadius(allBorderRadius = 1f)
                backgroundColor(colors().c(colors().textPrimary))
                transform(translate = Translate((tabs.indexOf(selected()) * itemWidth / 24f), 0f))
                animate(Animation.easeOut(0.16f), value = selected())
            }
        }
    }
}

/** 分段胶囊：选中项表面+描边+主文字。用于 MA/MACD/RSI 指标切换。 */
fun ViewContainer<*, *>.SegmentedTabs(
    options: List<String>,
    selected: () -> String,
    height: Float = 30f,
    onSelect: (String) -> Unit,
) {
    val colors = { AppTheme.colors }
    View {
        attr { flexDirectionRow() }
        options.forEach { opt ->
            View {
                attr {
                    height(height)
                    borderRadius(AppRadius.radius6)
                    marginRight(8f)
                    padding(left = 12f, right = 12f)
                    allCenter()
                    backgroundColor(colors().c(if (selected() == opt) colors().surface else colors().surfaceSecondary))
                    border(Border(1f, BorderStyle.SOLID, colors().c(if (selected() == opt) colors().borderStrong else colors().border)))
                    cssClass("zn-nav zn-click")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                event { click { onSelect(opt) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        if (selected() == opt) fontWeight600() else fontWeight400()
                        color(colors().c(if (selected() == opt) colors().textPrimary else colors().textSecondary))
                        text(opt)
                    }
                }
            }
        }
    }
}
