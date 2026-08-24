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
import com.zhiniu.domain.model.Quote
import com.zhiniu.pages.components.Tokens
import com.zhiniu.pages.components.hexInt

/** 个股搜索页：搜索联想 + 热门标签 + 历史（B5，Light Token，响应式列表）。 */
@Page("StockSearch", supportInLocal = true)
internal class StockSearchPage : BasePager() {

    private var keyword by observable("")

    private val allStocks by observableList<Quote>()
    private val results by observableList<String>()

    private val hotTags = listOf("贵州茅台", "宁德时代", "平安银行", "五粮液", "上证指数")

    override fun willInit() {
        super.willInit()
        // 用 MockData 股票池做搜索数据源（名称/symbol 可匹配）
        com.zhiniu.data.mock.MockDataSource().quotes().forEach { allStocks.add(it) }
        refresh()
    }

    private fun refresh() {
        results.clear()
        val kw = keyword.trim()
        if (kw.isEmpty()) {
            // 展示推荐
            hotTags.forEach { results.add("🔥 $it") }
            return
        }
        allStocks.forEach { q ->
            if (q.name.contains(kw, ignoreCase = true) || q.symbol.contains(kw, ignoreCase = true)) {
                results.add("${q.name} ${q.symbol}")
            }
        }
        if (results.isEmpty()) results.add("未找到「$kw」相关的标的")
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr { marginTop(Tokens.space3); marginLeft(Tokens.space3); fontSize(Tokens.fsH2); color(Color(hexInt(Tokens.textPrimary))); text("搜个股") }
            }
            // 搜索框（点击依次演示关键词，实际接输入桥接）
            View {
                attr { marginTop(Tokens.space3); marginLeft(Tokens.space2); marginRight(Tokens.space2) }
                Text {
                    attr {
                        fontSize(Tokens.fsBody)
                        color(Color(hexInt(if (ctx.keyword.isEmpty()) Tokens.textTertiary else Tokens.textPrimary)))
                        text(if (ctx.keyword.isEmpty()) "🔍 搜索：茅台 / 平安 / 宁德…" else "🔍 ${ctx.keyword}")
                    }
                    event { click { ctx.keyword = ctx.nextDemo(ctx.keyword); ctx.refresh() } }
                }
            }
            // 结果 / 推荐列表
            List {
                attr { height(480f); marginTop(Tokens.space2) }
                vforLazy({ ctx.results }) { row, _, _ ->
                    Text {
                        attr {
                            margin(Tokens.space2)
                            fontSize(Tokens.fsBody)
                            color(Color(hexInt(if (row.startsWith("未找到")) Tokens.warn else Tokens.textPrimary)))
                            text(row)
                        }
                        event { click { ctx.onPick(row) } }
                    }
                }
            }
        }
    }

    private fun nextDemo(cur: String): String = when (cur) {
        "" -> "茅台"
        "茅台" -> "平安"
        "平安" -> "宁德"
        else -> ""
    }

    private fun onPick(row: String) {
        // 从结果行提取 symbol：形如 "贵州茅台 sh600519"
        val parts = row.split(" ")
        if (parts.size >= 2) {
            val symbol = parts.last()
            val name = parts.dropLast(1).joinToString(" ")
            openZhiniuPage("StockDetail", mapOf("symbol" to symbol, "name" to name))
        }
    }
}