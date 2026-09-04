/* 知牛 · MarketPage（首页）：Market Pulse + 行情 Table
 * 标题区 72px → Market Pulse 92px → 28px 间距 → 工具条（自选/全部/沪市/深市/创业板/科创板 + 排序/筛选/搜索）
 * → Divider → 行情表（视觉中心）。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppSpacing
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.PAD
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.common.EmptyState
import com.zhiniu.pages.components.common.IconButton
import com.zhiniu.pages.components.common.SkeletonBar
import com.zhiniu.pages.components.market.MarketPulse
import com.zhiniu.pages.components.market.StockTable

@Page("MarketList", supportInLocal = true)
internal class MarketPage : AppBasePage() {

    // 命名按规范：marketQuotes / selectedMarket / isSearchVisible ...
    internal var selectedMarket by observable("全部")
    internal var sortMode by observable("默认排序") // 默认排序 / 涨幅 / 跌幅
    internal var filterUpOnly by observable(false)
    internal var marketLoading by observable(true)
    internal val marketQuotes by observableList<StockQuote>()

    override fun created() {
        super.created()
        setTimeout(280) {
            marketLoading = false
            refreshRows()
        }
    }

    internal fun refreshRows() {
        var rows = repo.stockQuotes().filter { it.inTab(selectedMarket) }
        if (filterUpOnly) rows = rows.filter { it.isUp }
        rows = when (sortMode) {
            "涨幅" -> rows.sortedByDescending { it.changePercent }
            "跌幅" -> rows.sortedBy { it.changePercent }
            else -> rows
        }
        marketQuotes.diffUpdate(rows)
    }

    private fun StockQuote.inTab(tab: String): Boolean = when (tab) {
        "沪市" -> symbol.startsWith("sh")
        "深市" -> symbol.startsWith("sz")
        "创业板" -> symbol.startsWith("sz30")
        "科创板" -> symbol.startsWith("sh68")
        else -> true
    }

    override fun body(): ViewBuilder = {
        attr {
            flexDirectionColumn()
            backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        renderCommonOverlays(this@MarketPage, "市场")
        List {
            attr {
                flex(1f)
                backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            marketContent(this@MarketPage)
            View { attr { height(48f) } }
        }
    }
}

private fun ViewContainer<*, *>.marketContent(host: MarketPage) {
    val colors = AppTheme.colors
    val pad: Float = PAD
        val aw: Float = host.pageData.activityWidth
        val extra: Float = if (aw > 1360f) (aw - 1360f) / 2f else 0f
        val sidePad: Float = pad + extra
    View {
        attr { padding(left = sidePad, right = sidePad) }
        // ---- 标题区（72px） ----
        View { attr { marginTop(28f) } }
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            Text {
                attr {
                    fontSize(AppTypography.fs24); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary))
                    text("市场")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { flex(1f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textTertiary))
                    text("Demo 行情 · 14:32 更新")
                }
            }
        }
        View { attr { height(4f) } }
        Text {
            attr {
                fontSize(AppTypography.fs13)
                color(colors.c(colors.textSecondary))
                text("沪深 A 股行情")
            }
        }
        // ---- Market Pulse（92px） ----
        View { attr { height(28f) } }
        MarketPulse(
            indices = host.repo.indices(),
            breadth = host.repo.breadth(),
            narrow = host.isNarrow(),
        )
        // ---- 工具条 28px 间距 ----
        View { attr { height(28f) } }
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            // Tabs（自选/全部/沪市/深市/创业板/科创板）—— 不用官方 Tabs，简单 View+Text
            val tabs = listOf("自选", "全部", "沪市", "深市", "创业板", "科创板")
            tabs.forEach { t ->
                MarketTab(label = t, active = host.selectedMarket == t) {
                    host.selectedMarket = t
                    host.refreshRows()
                }
            }
            View { attr { flex(1f) } }
            // 排序：默认 / 涨幅 / 跌幅（label + ⌄）
            View {
                attr {
                    height(34f); padding(left = 10f, right = 8f)
                    borderRadius(6f)
                    flexDirectionRow(); alignItemsCenter()
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                    cssClass("zn-click")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                event { click {
                    host.sortMode = when (host.sortMode) {
                        "默认排序" -> "涨幅"
                        "涨幅" -> "跌幅"
                        else -> "默认排序"
                    }
                    host.refreshRows()
                } }
                Text {
                    attr {
                        fontSize(AppTypography.fs13)
                        color(colors.c(colors.textPrimary))
                        text(host.sortMode)
                    }
                }
                View { attr { width(4f) } }
                Icon(if (host.sortMode == "默认排序") IconKind.FILTER_SORT else IconKind.CHEVRON_DOWN, 11f) { colors.textTertiary }
            }
            View { attr { width(8f) } }
            // 筛选：激活时图标转主色，列表仅显示上涨
            IconButton(
                IconKind.FILTER, size = 14f, box = 34f,
                colorHex = { if (host.filterUpOnly) colors.textPrimary else null },
            ) {
                host.filterUpOnly = !host.filterUpOnly
                host.refreshRows()
            }
            View { attr { width(8f) } }
            // 搜索（IconButton 34）
            IconButton(
                IconKind.SEARCH, size = 14f, box = 34f,
            ) { host.isSearchVisible = true }
        }
        View { attr { height(12f) } }
        // Divider
        View {
            attr {
                height(1f)
                backgroundColor(colors.c(colors.border))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        // ---- 行情表 ----
        vif({ host.marketLoading }) {
            MarketSkeleton()
        }
        velse {
            StockTable(
                marketQuotes = { host.marketQuotes },
                sparkOf = { host.repo.spark(it.symbol) },
                narrow = host.isNarrow(),
                onRowClick = { host.openStock(it.symbol) },
            )
            vif({ host.marketQuotes.isEmpty() }) {
                EmptyState(
                    title = "没有符合条件的股票",
                    desc = "切换上方 Tab 或调整筛选条件",
                )
            }
        }
    }
}

private fun ViewContainer<*, *>.MarketTab(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(34f); padding(left = 4f, right = 4f); marginRight(20f)
            flexDirectionColumn(); alignItemsCenter(); justifyContentCenter()
            cssClass("zn-click")
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(AppTypography.fs14)
                color(colors.c(if (active) colors.textPrimary else colors.textSecondary))
                fontWeight600()
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(4f) } }
        View {
            attr {
                height(2f); width(16f)
                borderRadius(allBorderRadius = 1f)
                backgroundColor(colors.c(colors.textPrimary))
                opacity(if (active) 1f else 0f)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

private fun ViewContainer<*, *>.MarketSkeleton() {
    val colors = AppTheme.colors
    repeat(8) {
        View {
            attr { flexDirectionRow(); alignItemsCenter(); height(64f) }
            View { attr { flex(2.4f) }; SkeletonBar(140f, 12f) }
            View { attr { width(120f) }; SkeletonBar(80f, 12f) }
            View { attr { width(110f) }; SkeletonBar(70f, 12f) }
            View { attr { width(110f) }; SkeletonBar(70f, 12f) }
            View { attr { width(170f) }; SkeletonBar(80f, 12f) }
            View { attr { width(150f) }; SkeletonBar(80f, 12f) }
            View { attr { width(110f) }; SkeletonBar(80f, 12f) }
        }
        View {
            attr { height(1f); backgroundColor(colors.c(colors.border)) }
        }
    }
}
