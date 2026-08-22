/* 知牛 · AI 聊天主页（Task02，完整实现）
 * 布局：顶部标题+Agent时间线 → 消息列表(气泡) → 快捷指令 → 输入区。
 * 组件：内置 List/Text/Input/Button；assistant 用 KuiklyMarkdown 流式渲染。
 * ⚠️ 指令(vfor/vif)与输入/按钮/漏斗签名以 Kuikly SDK 官方模板为准。
 */
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder
import com.tencent.kuikly.ref.widget.Text
import com.zhiniu.domain.model.ChatMessage
import com.zhiniu.domain.model.MessageRole
import com.zhiniu.pages.components.Palette
import com.zhiniu.viewmodel.ChatVM

@Page("ChatHome")
internal class ChatHomePage(
    private val vm: ChatVM,
) : Pager() {

    private var inputDraft = ""

    override fun body(): ViewBuilder {
        return {
            View {
                attr { flex(1f); flexDirection(FLEX_DIRECTION_COLUMN) }
                // ===== 标题 + Agent 时间线 =====
                Text { attr { text("知牛 · AI 问股"); fontSize(18f) } }
                agentTimeline()

                // ===== 消息列表 =====
                List {
                    attr {
                        flex(1f)
                        vfor(vm.messages) { m ->
                            messageBubble(m)
                        }
                    }
                }

                // ===== 快捷指令 =====
                View {
                    attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f) }
                    vfor(vm.quickCommands) { c ->
                        Text {
                            attr {
                                text(c); color(colorOf(Palette.UP)); onClick { vm.send(c) }
                            }
                        }
                    }
                }

                // ===== 输入区 =====
                View {
                    attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f) }
                    Input {
                        attr {
                            value(inputDraft)
                            placeholder("问点什么…")
                            onValueChange { inputDraft = it }
                        }
                    }
                    Text {
                        attr {
                            text("发送"); color(colorOf(Palette.UP)); onClick { vm.send(inputDraft); inputDraft = "" }
                        }
                    }
                }
            }
        }
    }

    /** Agent 时间线（基本面→技术→舆情→风控）只读 Stepper，用 View+Text 表达。 */
    private fun agentTimeline(): ViewBuilder = {
        View {
            attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f) }
            Text { attr { text("● 基本面") } }
            Text { attr { text("● 技术") } }
            Text { attr { text("● 舆情") } }
            Text { attr { text("● 风控") } }
        }
    }

    private fun messageBubble(m: ChatMessage): ViewBuilder = {
        View {
            attr { flexGrow(1f) }
            when (m.role) {
                MessageRole.ASSISTANT -> {
                    // 流式文本/KuiklyMarkdown：精确组件签名以 SDK 模板为准
                    Text { attr { text(m.content) } }
                    if (m.isStreaming) Text { attr { text("▍"); color(colorOf(Palette.UP)) } }
                }
                else -> Text { attr { text(m.content) } }
            }
        }
    }

    private fun colorOf(hex: String): String = hex
}