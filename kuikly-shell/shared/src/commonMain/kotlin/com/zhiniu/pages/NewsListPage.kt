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
import com.zhiniu.pages.components.Tokens
import com.zhiniu.pages.components.hexInt

private data class NewsItem(val time: String, val tag: String, val title: String)

/** 资讯页：7×24 快讯列表（B6，Light Token，响应式列表；数据与后端 /news/list 结构对齐）。 */
@Page("NewsList", supportInLocal = true)
internal class NewsListPage : BasePager() {

    private var tab by observable("快讯")
    private val items by observableList<NewsItem>()

    override fun willInit() {
        super.willInit()
        rebuild()
    }

    private fun rebuild() {
        items.clear()
        if (tab == "快讯") {
            flashNews().forEach { items.add(it) }
        } else {
            stockNews().forEach { items.add(it) }
        }
    }

    private fun flashNews(): List<NewsItem> = listOf(
        NewsItem("14:52", "宏观", "央行开展 5000 亿 MLF 操作，利率持平"),
        NewsItem("14:30", "行业", "半导体板块午后异动拉升，多股涨停"),
        NewsItem("14:05", "公司", "贵州茅台：上半年营收同比增约 8%，符合预期"),
        NewsItem("13:40", "资金", "北向资金今日净流入超 60 亿，加仓食品饮料与新能源"),
        NewsItem("13:15", "政策", "证监会：进一步优化并购重组审核流程"),
        NewsItem("12:40", "宏观", "6 月规模以上工业增加值同比增 5.3%，好于预期"),
        NewsItem("12:00", "行业", "新能源汽车 6 月销量创新高，渗透率突破 50%"),
    )

    private fun stockNews(): List<NewsItem> = listOf(
        NewsItem("13:05", "公司", "机构：茅台批价企稳，全年目标可期"),
        NewsItem("11:30", "公司", "平安银行：零售转型见效，净息差保持韧性"),
        NewsItem("10:20", "公司", "宁德时代：与全球车企深化合作，市占率稳居第一"),
        NewsItem("09:50", "公告", "招商银行：拟派发中期股息，股东回报提升"),
    )

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr { marginTop(Tokens.space3); marginLeft(Tokens.space3); fontSize(Tokens.fsH2); color(Color(hexInt(Tokens.textPrimary))); text("资讯") }
            }
            View {
                attr { flexDirectionRow(); marginTop(Tokens.space2); marginLeft(Tokens.space3) }
                listOf("快讯", "个股").forEach { t ->
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
                vforLazy({ ctx.items }) { n, _, _ ->
                    View {
                        attr {
                            flexDirectionRow()
                            margin(Tokens.space2)
                            backgroundColor(Color(hexInt(Tokens.bgCard)))
                        }
                        Text { attr { fontSize(Tokens.fsCaption); color(Color(hexInt(Tokens.textMuted))); text(n.time) } }
                        Text {
                            attr {
                                marginLeft(Tokens.space2); fontSize(Tokens.fsCaption)
                                color(if (n.tag == "宏观" || n.tag == "政策") Color(hexInt(Tokens.info)) else Color(hexInt(Tokens.brand)))
                                text(n.tag)
                            }
                        }
                        Text {
                            attr {
                                flex(1f); marginLeft(Tokens.space2); fontSize(Tokens.fsBody)
                                color(Color(hexInt(Tokens.textPrimary)))
                                text(n.title)
                            }
                        }
                    }
                }
            }
        }
    }
}