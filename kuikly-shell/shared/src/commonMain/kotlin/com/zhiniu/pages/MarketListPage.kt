package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.base.BasePager
import com.zhiniu.base.openZhiniuPage
import com.zhiniu.data.mock.MockDataSource
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.Tokens
import com.zhiniu.pages.components.hexInt
import kotlin.math.roundToInt

// 行情分类
private enum class MarketMode(val label: String) {
    WATCHLIST("自选"), ALL("全部"), GAINERS("涨幅"), LOSERS("跌幅");
}

/** 行情列表页：指数条 + 分类 Tab + 股票行列表（Kuikly 官方 core DSL + Light Token 皮肤）。 */
@Page("MarketList", supportInLocal = true)
internal class MarketListPage : BasePager() {

    private var mode by observable(MarketMode.ALL)

    private val all by observableList<Quote>()
    private val quotes by observableList<Quote>()

    override fun willInit() {
        super.willInit()
        MockDataSource().quotes().forEach { all.add(it) }
        refreshed()
    }

    private fun refreshed() {
        quotes.clear()
        when (mode) {
            MarketMode.WATCHLIST -> all.take(3).forEach { quotes.add(it) }
            MarketMode.ALL -> all.forEach { quotes.add(it) }
            MarketMode.GAINERS -> all.sortedByDescending { it.changePercent }.forEach { quotes.add(it) }
            MarketMode.LOSERS -> all.sortedBy { it.changePercent }.forEach { quotes.add(it) }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 标题
            Text {
                attr {
                    marginTop(Tokens.space3); marginLeft(Tokens.space3)
                    fontSize(Tokens.fsBody)
                    color(Color(hexInt(Tokens.textTertiary)))
                    text("知牛 · 行情")
                }
            }
            // 分类 Tab（选中=品牌绿强读或主文字；未选中=次要文字）
            View {
                attr { flexDirectionRow(); marginTop(Tokens.space2) }
                MarketMode.entries.forEach { m ->
                    Text {
                        attr {
                            marginLeft(Tokens.space3)
                            fontSize(Tokens.fsH3)
                            text(m.label)
                            color(if (ctx.mode == m) Color(hexInt(Tokens.textPrimary)) else Color(hexInt(Tokens.textTertiary)))
                        }
                        event { click { ctx.mode = m; ctx.refreshed() } }
                    }
                }
            }
            // 股票行列表（Lazy）
            List {
                attr { height(500f); marginTop(Tokens.space2); backgroundColor(Color(hexInt(Tokens.bgCard))) }
                vforLazy({ ctx.mode; ctx.quotes }) { q, _, _ ->
                    View {
                        attr {
                            flexDirectionRow()
                            padding(Tokens.space2)
                            backgroundColor(Color(hexInt(Tokens.bgCard)))
                        }
                        Text {
                            attr { flex(1f); fontSize(Tokens.fsH3); color(Color(hexInt(Tokens.textPrimary))); text("${q.name}") }
                            event { click { ctx.openZhiniuPage("StockDetail", mapOf("symbol" to q.symbol, "name" to q.name)) } }
                        }
                        Text { attr { fontSize(Tokens.fsBody); color(ctx.qColor(q)); text(ctx.mono(q.price)) } }
                        Text { attr { marginLeft(Tokens.space2); fontSize(Tokens.fsBody); color(ctx.qColor(q)); text(ctx.pct(q)) } }
                    }
                }
            }
        }
    }

    private fun qColor(q: Quote): Color =
        if (q.price >= q.prevClose) Color(hexInt(Tokens.up)) else Color(hexInt(Tokens.down))

    private fun mono(v: Double): String {
        val r = (kotlin.math.round(v * 100)) / 100
        return r.toString()
    }

    private fun pct(q: Quote): String {
        val p = (q.price - q.prevClose) / q.prevClose * 100
        val r = (p * 100).roundToInt() / 100.0
        return (if (r >= 0) "+" else "") + r.toString() + "%"
    }
}

