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
import com.zhiniu.data.remote.LlmGatewayClient
import com.zhiniu.domain.model.ChatMessage
import com.zhiniu.domain.model.MessageRole
import com.zhiniu.domain.model.StreamChunk
import com.zhiniu.pages.components.Tokens
import com.zhiniu.pages.components.hexInt
import com.zhiniu.platform.createPlatformHttpClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** AI 聊天页：快捷指令 + 消息列表 + 输入区，真实调用后端 LLM 网关（B3，SSE 流式）。 */
@Page("ChatHome", supportInLocal = true)
internal class ChatHomePage : BasePager() {

    private var draft by observable("")
    private var seq = 0

    private val messages by observableList<String>()

    private val quickCmds = listOf("看大盘", "诊个股", "解释指标", "对比两只")

    // B3：H5 端真实后端网关客户端（Ktor JS engine → localhost:8000）
    private val llm = LlmGatewayClient(createPlatformHttpClient(), "http://localhost:8000")

    override fun willInit() {
        super.willInit()
        messages.add("你好，我是知牛。可以问我：看大盘 / 诊个股 / 解释指标 / 对比两只。")
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr { marginTop(Tokens.space3); marginLeft(Tokens.space3); fontSize(Tokens.fsH2); color(Color(hexInt(Tokens.textPrimary))); text("知牛 · AI 问股") }
            }
            View {
                attr { flexDirectionRow(); marginTop(Tokens.space3); marginLeft(Tokens.space3); marginRight(Tokens.space3) }
                Text {
                    attr { fontSize(Tokens.fsBody); color(Color(hexInt(Tokens.brand))); text("模型：zhiniu/quick") }
                }
                Text {
                    attr { marginLeft(Tokens.space4); fontSize(Tokens.fsBody); color(Color(hexInt(Tokens.textMuted))); text("看行情 →") }
                    event { click { ctx.openZhiniuPage("MarketList") } }
                }
            }

            List {
                attr { height(300f); marginTop(Tokens.space2) }
                vforLazy({ ctx.messages }) { m, _, _ ->
                    Text { attr { margin(Tokens.space2); fontSize(Tokens.fsBody); lineHeight(22f); color(ctx.msgColor(m)); text(m) } }
                }
            }

            View {
                attr { flexDirectionRow(); marginTop(Tokens.space2); marginLeft(Tokens.space2) }
                ctx.quickCmds.forEach { c ->
                    Text {
                        attr {
                            marginRight(Tokens.space3); fontSize(Tokens.fsH3); color(Color(hexInt(Tokens.brand)))
                            text(ctx.chip(c))
                        }
                        event { click { ctx.onSend(c) } }
                    }
                }
            }

            View {
                attr { flexDirectionRow(); marginTop(Tokens.space3); marginLeft(Tokens.space2); marginRight(Tokens.space2) }
                Text {
                    attr { flex(1f); fontSize(Tokens.fsBody); color(Color(hexInt(if (ctx.draft.isEmpty()) Tokens.textMuted else Tokens.textPrimary))); text(ctx.draft.ifBlank { "问点什么…" }) }
                    event { click { ctx.draft = "诊一下贵州茅台 sh600519" } }
                }
                Text {
                    attr { fontSize(Tokens.fsBody); color(Color(hexInt(Tokens.brand))); text("发送") }
                    event { click { ctx.onSend(ctx.draft) } }
                }
            }
        }
    }

    private fun chip(c: String): String = "「$c」"
    private fun msgColor(m: String): Color = if (m.startsWith("你")) Color(hexInt(Tokens.textPrimary)) else Color(hexInt(Tokens.brandDeep))

    private fun onSend(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        // 异步发送，立即回显用户消息
        messages.add("你: $t")
        draft = ""
        val botId = "bot-" + (seq++)
        messages.add(botId)

        GlobalScope.launch {
            val history = listOf(
                ChatMessage("u1", MessageRole.USER, t),
            )
            val sb = StringBuilder()
            try {
                llm.chatStream("zhiniu/quick", history).collect { chunk ->
                    when (chunk) {
                        is StreamChunk.Delta -> {
                            sb.append(chunk.text)
                            val full = sb.toString()
                            replace(botId, full)
                        }
                        is StreamChunk.Error -> replace(botId, chunk.msg)
                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                replace(botId, "网关调用失败: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    private fun replace(id: String, text: String) {
        val i = messages.indexOfFirst { it == id }
        if (i >= 0) messages[i] = text
    }
}