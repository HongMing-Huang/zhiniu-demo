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
import com.tencent.kuikly.core.directives.vfor
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
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.common.GhostButton
import com.zhiniu.pages.components.common.PrimaryButton
import com.zhiniu.pages.components.common.SectionHeader
import com.zhiniu.pages.components.market.StockTable
import com.tencent.kuikly.core.coroutines.launch

private const val WATCHLIST_STORE_KEY = "zhiniu.watchlist.symbols.v1"
private const val ALERTS_STORE_KEY = "zhiniu.alert.items.v1"

@Page("Watchlist", supportInLocal = true)
internal class WatchlistPage : AppBasePage() {

    internal val watchQuotes by observableList<StockQuote>()
    internal val alerts by observableList<com.zhiniu.data.local.PriceAlert>()
    internal var alertFiredNote by observable("")
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
        // 恢复价格预警（AI 经 ⟦TOOL⟧ 指令设置的预警与其他页写入共享同一 SP 键）
        com.zhiniu.data.local.AlertStore.restore(com.zhiniu.data.local.AlertStore.deserialize(sp.getString(ALERTS_STORE_KEY)))
        alerts.diffUpdate(com.zhiniu.data.local.AlertStore.alerts())
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
            // 预警触发检查（真实行情到达才检查；离线快照不触发，避免误报）
            if (live != null) checkAlerts(live)
            loading = false
        }
    }

    /** 依据最新行情检查预警并刷新列表/提示（幂等：已触发的不再重复提示）。 */
    internal fun checkAlerts(quotes: List<StockQuote>) {
        val fired = com.zhiniu.data.local.AlertStore.check(quotes)
        if (fired.isNotEmpty()) {
            persistAlerts()
            alertFiredNote = fired.joinToString("；") { "${it.name} 已${if (it.operator == "above") "突破" else "跌破"} ${it.price}" }
        } else if (alertFiredNote.isNotBlank() && com.zhiniu.data.local.AlertStore.alerts().none { !it.triggered }) {
            alertFiredNote = ""
        }
        alerts.diffUpdate(com.zhiniu.data.local.AlertStore.alerts())
    }

    internal fun removeAlert(id: String) {
        com.zhiniu.data.local.AlertStore.remove(id)
        persistAlerts()
        alerts.diffUpdate(com.zhiniu.data.local.AlertStore.alerts())
    }

    internal fun persistAlerts() {
        sp.setString(ALERTS_STORE_KEY, com.zhiniu.data.local.AlertStore.serialize())
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
            // 价格预警（AI 经 ⟦TOOL⟧ 指令设置；真实行情到达时自动标记触发）
            vif({ host.alerts.isNotEmpty() }) {
                View { attr { height(20f) } }
                SectionHeader(
                    "价格预警",
                    action = "刷新", onAction = { host.refresh() },
                )
                vif({ host.alertFiredNote.isNotBlank() }) {
                    View {
                        attr {
                            marginTop(8f); borderRadius(10f)
                            padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                            backgroundColor(colors.ca(colors.up, 10))
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                        Text {
                            attr {
                                fontSize(AppTypography.fs12); lineHeight(18f)
                                color(colors.c(colors.textPrimary)); text("⚡ ${host.alertFiredNote}")
                            }
                        }
                    }
                }
                View { attr { height(8f) } }
                vfor({ host.alerts }) { alert ->
                    View {
                        attr {
                            flexDirectionRow(); alignItemsCenter()
                            marginBottom(8f); borderRadius(10f)
                            padding(left = 12f, right = 8f, top = 10f, bottom = 10f)
                            backgroundColor(colors.c(colors.surface))
                            animate(ANIM_THEME, value = AppTheme.isDark)
                            accessibility("预警 ${alert.name} ${if (alert.operator == "above") "突破" else "跌破"} ${alert.price}" + if (alert.triggered) "，已触发" else "")
                        }
                        event { click { host.openStock(alert.symbol) } }
                        View {
                            attr { flex(1f); flexDirectionColumn() }
                            Text {
                                attr {
                                    fontSize(AppTypography.fs13); fontWeightMedium()
                                    color(colors.c(colors.textPrimary))
                                    text(alert.name + "  " + (if (alert.operator == "above") "突破 " else "跌破 ") + com.zhiniu.pages.components.fmt2(alert.price))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(1f); fontSize(AppTypography.fs11)
                                    color(colors.c(colors.textTertiary))
                                    text(if (alert.triggered) "已触发 · 点击查看行情" else "监控中 · 点击查看行情")
                                }
                            }
                        }
                        Text {
                            attr {
                                fontSize(AppTypography.fs11); fontWeightMedium(); marginRight(8f)
                                color(colors.c(if (alert.triggered) colors.up else colors.textTertiary))
                                text(if (alert.triggered) "已触发" else "监控中")
                            }
                        }
                        View {
                            attr {
                                width(28f); height(28f); borderRadius(AppRadius.radius6); allCenter()
                                cssClass("zn-click")
                                accessibility("删除预警 ${alert.name}")
                            }
                            event {
                                click { host.removeAlert(alert.id) }
                            }
                            Text {
                                attr {
                                    fontSize(AppTypography.fs14)
                                    color(colors.c(colors.textTertiary)); text("×")
                                }
                            }
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(AppTypography.fs11)
                        color(colors.c(colors.textTertiary))
                        text("在 AI 研究页对知牛说「宁德时代跌破 300 提醒我」即可新增预警")
                    }
                }
            }
            View { attr { height(24f) } }
        }
    }
}
