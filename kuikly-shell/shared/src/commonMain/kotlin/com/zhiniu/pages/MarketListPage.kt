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
import kotlin.math.roundToInt

// 行情分类
private enum class MarketMode(val label: String) {
    WATCHLIST("自选"), ALL("全部"), GAINERS("涨幅"), LOSERS("跌幅");
}

/** 行情列表页：指数条 + 分类 Tab + 股票行列表（Kuikly 官方 core DSL）。 */
@Page("MarketList", supportInLocal = true)
internal class MarketListPage : BasePager() {

    private var mode by observable(MarketMode.ALL)

    private val quotes by observableList<Quote>()

    override fun willInit() {
        super.willInit()
        MockDataSource().quotes().forEach { quotes.add(it) }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 指数条
            Text {
                attr {
                    marginTop(10f); marginLeft(10f)
                    fontSize(13f)
                    color(Color(0xFF888888))
                    text("知牛 · 行情")
                }
            }
            // 分类 Tab
            View {
                attr { flexDirectionRow(); marginTop(10f) }
                MarketMode.entries.forEach { m ->
                    Text {
                        attr {
                            marginLeft(12f)
                            fontSize(15f)
                            text(m.label)
                            color(if (ctx.mode == m) Color(0xFFE53935) else Color(0xFF999999))
                        }
                        event { click { ctx.mode = m } }
                    }
                }
            }
            // 股票行列表（Lazy）
            List {
                attr { height(500f); marginTop(6f) }
                vforLazy({ ctx.mode; ctx.quotes }) { q, _, _ ->
                    View {
                        attr {
                            flexDirectionRow()
                            padding(8f)
                            backgroundColor(Color.WHITE)
                        }
                        Text { attr { flex(1f); text("${q.name} ${q.symbol}") } }
                        Text { attr { text(q.price.toString()); color(ctx.priceColor(q)) } }
                        Text { attr { marginLeft(6f); text(ctx.pct(q)); color(ctx.priceColor(q)) } }
                    }
                    // 行点击 → 详情
                    event { click { ctx.openZhiniuPage("StockDetail", mapOf("symbol" to q.symbol, "name" to q.name)) } }
                }
            }
        }
    }

    private fun priceColor(q: Quote): Color =
        if (q.price >= q.prevClose) Color(0xFFE53935) else Color(0xFF2E7D32)

    private fun pct(q: Quote): String {
        val p = (q.price - q.prevClose) / q.prevClose * 100
        val rounded = (p * 100).roundToInt() / 100.0
        return (if (rounded >= 0) "+" else "") + rounded.toString() + "%"
    }
}