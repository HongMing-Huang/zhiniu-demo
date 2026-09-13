/* 知牛 · WatchlistPage（自选行情列表 · 课题 Task1 首页要求）
 * 自选股编辑与行情浏览；Watchlist 内存态 + SharedPreferences 持久化双写，
 * 页面加载时从 SP 恢复、任何增删后写回（跨端 SharedPreferencesModule 三端落地）。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.base.openAiResearchPage
import com.zhiniu.base.openMarketPage
import com.zhiniu.data.remote.GatewayMarketClient
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.common.GhostButton
import com.zhiniu.pages.components.common.PrimaryButton
import com.zhiniu.pages.components.market.StockTable
import com.tencent.kuikly.core.coroutines.launch

private const val WATCHLIST_STORE_KEY = "zhiniu.watchlist.symbols.v1"

@Page("Watchlist", supportInLocal = true)
internal class WatchlistPage : AppBasePage() {

    internal val watchQuotes by observableList<StockQuote>()
    internal var loading by observable(true)
    internal var sourceLabel by observable("本地快照")

    private lateinit var sp: SharedPreferencesModule

    override fun created() {
        super.created()
        sp = acquireModule(SharedPreferencesModule.MODULE_NAME)
        // 恢复持久化自选（SP 是唯一事实源；MarketStore/Watchlist 内存态同步）
        val saved = sp.getString(WATCHLIST_STORE_KEY)
        if (saved.isNotBlank()) {
            com.zhiniu.data.local.Watchlist.restore(saved.split(",").filter { it.isNotBlank() })
        }
        refresh()
    }

    internal fun refresh() {
        loading = true
        sourceLabel = "同步中…"
        val symbols = com.zhiniu.data.local.Watchlist.symbols().toList()
        if (symbols.isEmpty()) {
            watchQuotes.diffUpdate(emptyList())
            loading = false
            sourceLabel = "共 0 只"
            return
        }
        lifecycleScope.launch {
            // 行情：优先网关实时；失败回落本地快照（离线演示不空屏）
            val live = runCatching { GatewayMarketClient.quotes(symbols) }.getOrNull()
            val rows = live?.takeIf { it.isNotEmpty() } ?: symbols.mapNotNull { repo.quoteOf(it) }
            watchQuotes.diffUpdate(rows.sortedByDescending { it.changePercent })
            sourceLabel = if (live != null) "实时行情" else "本地快照 · 可重试"
            loading = false
        }
    }

    internal fun remove(symbol: String) {
        com.zhiniu.data.local.Watchlist.remove(symbol)
        watchQuotes.diffUpdate(watchQuotes.filter { it.symbol != symbol })
        persist()
        refresh()
    }

    internal fun persist() {
        sp.setString(WATCHLIST_STORE_KEY, com.zhiniu.data.local.Watchlist.symbols().joinToString(","))
    }

    override fun body(): ViewBuilder = {
        attr {
            flexDirectionColumn()
            backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        renderCommonOverlays(this@WatchlistPage, "自选")
        watchContent(this@WatchlistPage)
        renderBottomTab(this@WatchlistPage, "自选")
    }
}

// ============== 内容区 ==============
private fun ViewContainer<*, *>.watchContent(host: WatchlistPage) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f); flexDirectionColumn()
            paddingLeft(if (host.isCompact()) 16f else 32f)
            paddingRight(if (host.isCompact()) 16f else 32f)
            backgroundColor(colors.c(colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr { width(host.contentWidth()); flexDirectionColumn() }
            View { attr { height(24f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs24); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary)); text("自选")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(4f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textSecondary))
                    text(host.sourceLabel + " · 左滑删除或进入详情管理")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(16f) } }
            vif({ host.watchQuotes.isEmpty() && !host.loading }) {
                // 空态：引导去市场页加自选
                View {
                    attr {
                        height(160f); allCenter(); flexDirectionColumn()
                        borderRadius(12f)
                        backgroundColor(colors.c(colors.surface))
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                    Text {
                        attr {
                            fontSize(AppTypography.fs14)
                            color(colors.c(colors.textSecondary)); text("暂无自选股")
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                    }
                    View { attr { height(10f) } }
                    Text {
                        attr {
                            fontSize(AppTypography.fs12)
                            color(colors.c(colors.textTertiary))
                            text("在「市场」页点击股票详情的「加自选」")
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                    }
                    View { attr { height(14f) } }
                    GhostButton("去市场看看", height = 34f) { host.navMarket() }
                }
            }
            velse {
                StockTable(
                    marketQuotes = { host.watchQuotes },
                    sparkOf = { host.repo.spark(it.symbol) },
                    narrow = host.isNarrow(),
                    compact = host.isCompact(),
                    columns = com.zhiniu.pages.components.market.StockColumns.DEFAULT,
                    onRowClick = { host.openStock(it.symbol) },
                )
            }
            View { attr { height(24f) } }
        }
    }
}
