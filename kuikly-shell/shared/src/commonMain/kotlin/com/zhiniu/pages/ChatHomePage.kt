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

/** AI 聊天页：快捷指令 + 消息列表 + 输入区（Kuikly 官方 core DSL）。 */
@Page("ChatHome", supportInLocal = true)
internal class ChatHomePage : BasePager() {

    private var draft by observable("")

    // 与 MarketListPage 一致：用 observableList 委托（by），闭包内访问 ctx.messages 即 ObservableList
    private val messages by observableList<String>()

    private val quickCmds = listOf("看大盘", "诊个股", "解释指标", "对比两只")

    private val mock = MockDataSource()

    override fun willInit() {
        super.willInit()
        messages.add("👋 你好，我是知牛。可以问我：看大盘 / 诊个股 / 解释指标 / 对比两只。")
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 标题
            View {
                attr { flexDirectionRow(); marginTop(12f); marginLeft(10f); marginRight(10f) }
                Text { attr { flex(1f); fontSize(18f); color(Color(0xFF222222)); text("知牛 · AI 问股") } }
                Text {
                    attr { fontSize(14f); color(Color(0xFF888888)); text("看行情 →") }
                    event { click { ctx.openZhiniuPage("MarketList") } }
                }
            }

            // 消息列表
            List {
                attr { height(340f); marginTop(6f) }
                vforLazy({ ctx.messages }) { m, _, _ ->
                    Text { attr { margin(8f); fontSize(14f); lineHeight(22f); color(Color(0xFF333333)); text(m) } }
                }
            }

            // 快捷指令
            View {
                attr { flexDirectionRow(); marginTop(10f); marginLeft(8f) }
                ctx.quickCmds.forEach { c ->
                    Text {
                        attr {
                            marginRight(8f); fontSize(14f); color(Color(0xFFE53935))
                            text(ctx.chip(c))
                        }
                        event { click { ctx.onSend(c) } }
                    }
                }
            }

            // 输入区
            View {
                attr { flexDirectionRow(); marginTop(12f); marginLeft(8f); marginRight(8f) }
                Text {
                    attr { flex(1f); fontSize(15f); color(Color(0xFF333333)); text(ctx.draft.ifBlank { "问点什么…" }) }
                    event { click { ctx.draft = "诊一下贵州茅台 sh600519" } }
                }
                Text {
                    attr { fontSize(15f); color(Color(0xFFE53935)); text("发送") }
                    event { click { ctx.onSend(ctx.draft) } }
                }
            }
        }
    }

    private fun chip(c: String): String = "「$c」"

    private fun onSend(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        messages.add("你: $t")
        val reply = buildString {
            append("🤖 ")
            if (t.contains("大盘")) {
                append("上证 ${ctxPrice("sh000001")}，今日温和放量上行，多方占优。")
            } else if (t.contains("贵州茅台") || t.contains("sh600519")) {
                append("贵州茅台震荡偏强，关注 1300 关口与量能配合。")
            } else {
                append("收到，正在分析「$t」。多空信号：短线中性偏多。")
            }
        }
        messages.add(reply)
        draft = ""
    }

    private fun ctxPrice(symbol: String): String {
        val q = mock.quotes().firstOrNull { it.symbol == symbol } ?: return "-"
        val r = (kotlin.math.round(q.price * 100)) / 100
        return r.toString()
    }
}