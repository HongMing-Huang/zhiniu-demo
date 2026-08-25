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
import com.zhiniu.data.mock.MockDataSource
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.Tokens
import com.zhiniu.pages.components.hexInt
import kotlin.math.roundToInt

private data class Row(val label: String, val value: String, val pct: Float)

/** 指数/板块看板页：指数 Tab + 板块 Tab（B5b，Light Token，响应式列表）。 */
@Page("IndexBoard", supportInLocal = true)
internal class IndexBoardPage : BasePager() {

    private var tab by observable("指数")
    private val rows by observableList<Row>()

    override fun willInit() {
        super.willInit()
        rebuild()
    }

    private fun rebuild() {
        rows.clear()
        if (tab == "指数") {
            MockDataSource().quotes().filter { it.symbol.startsWith("sh000") || it.symbol.startsWith("sz399") }
                .forEach { q -> rows.add(rowOf(q)) }
        } else {
            listOf(
                "半导体" to 2.4f, "软件" to 1.9f, "军工" to 1.6f, "银行" to 1.3f,
                "煤炭" to 1.1f, "证券" to 0.9f, "新能源" to 0.7f, "白酒" to 0.4f,
                "医药" to -0.3f, "地产" to -0.8f, "光伏" to -1.2f, "汽车" to -1.6f,
            ).forEach { (name, pct) ->
                rows.add(Row(name, "板块", pct))
            }
        }
    }

    private fun rowOf(q: Quote): Row = Row(q.name, "${q.price}", pctFloat(q))

    private fun pctFloat(q: Quote): Float = ((q.price - q.prevClose) / q.prevClose * 100).toFloat()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr { marginTop(Tokens.space3); marginLeft(Tokens.space3); fontSize(Tokens.fsH2); color(Color(hexInt(Tokens.textPrimary))); text("指数 · 板块") }
            }
            View {
                attr { flexDirectionRow(); marginTop(Tokens.space2); marginLeft(Tokens.space3) }
                listOf("指数", "板块").forEach { t ->
                    Text {
                        attr {
                            marginRight(Tokens.space4)
                            fontSize(Tokens.fsH3)
                            text(t)
                            color(if (ctx.tab == t) Color(hexInt(Tokens.textPrimary)) else Color(hexInt(Tokens.textMuted)))
                        }
                        event { click { ctx.tab = t; ctx.rebuild() } }
                    }
                }
            }
            List {
                attr { height(520f); marginTop(Tokens.space2) }
                vforLazy({ ctx.rows }) { r, _, _ ->
                    View {
                        attr {
                            flexDirectionRow()
                            margin(Tokens.space2)
                            backgroundColor(Color(hexInt(Tokens.bgCard)))
                        }
                        Text { attr { flex(1f); fontSize(Tokens.fsH3); color(Color(hexInt(Tokens.textPrimary))); text(r.label) } }
                        Text { attr { fontSize(Tokens.fsBody); color(ctx.valColor(r.pct)); text(ctx.valText(r)) } }
                    }
                }
            }
        }
    }

    private fun valColor(pct: Float): Color = if (pct >= 0f) Color(hexInt(Tokens.up)) else Color(hexInt(Tokens.down))

    private fun valText(r: Row): String {
        val rnd = (r.pct * 100).roundToInt() / 100f
        val suffix = r.value
        return if (r.pct >= 0f) "+$rnd% $suffix" else "$rnd% $suffix"
    }
}