/* 知牛 · AI 聊天主页（接线：QuickChip 快捷指令 + Agent 时间线 Stepper 点亮）
 * 消息流式渲染最终接 KuiklyMarkdown（契约见组件/文档）；此处保留 List 容器。
 * ⚠️ 指令(vfor/vif)与 Input/发送按钮签名以 Kuikly SDK 官方模板为准。
 */
package com.zhiniu.pages

import com.tencent.kuikly.ref.pager.Pager
import com.tencent.kuikly.ref.view.ViewBuilder
import com.tencent.kuikly.ref.widget.Text
import com.zhiniu.domain.model.ChatMessage
import com.zhiniu.domain.model.MessageRole
import com.zhiniu.pages.components.Palette
import com.zhiniu.pages.components.colorOf
import com.zhiniu.pages.components.quickChip
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

                // ===== 标题 + Agent 时间线（Stepper，由 vm.agentDone 驱动）=====
                Text { attr { text("知牛 · AI 问股"); fontSize(18f); color(colorOf(Palette.TEXT)) } }
                agentTimeline(vm.agentDone.value)

                // ===== 消息列表（assistant 最终 KuiklyMarkdown 流式）=====
                List {
                    attr {
                        flex(1f)
                        vfor(vm.messages) { m -> messageBubble(m) }
                    }
                }

                // ===== 快捷指令（QuickChip = GHOST+16 圆角按钮语义）=====
                View {
                    attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f); marginTop(6f) }
                    vfor(vm.quickCommands) { c -> quickChip(c) { vm.send(c) } }
                }

                // ===== 输入区 =====
                View {
                    attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f); marginTop(8f) }
                    Input {
                        attr {
                            flex(1f)
                            value(inputDraft)
                            placeholder("问点什么…")
                            onValueChange { inputDraft = it }
                        }
                    }
                    Text {
                        attr {
                            text("发送")
                            color(colorOf(Palette.UP))
                            onClick { vm.send(inputDraft); inputDraft = "" }
                        }
                    }
                }
            }
        }
    }

    /** Agent 时间线（基本面→技术→舆情→风控）：未达=空心○，进行中=实心●，完成=✓。 */
    private fun agentTimeline(done: Int): ViewBuilder = {
        View {
            attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f) }
            step("基本面", 0, done)
            step("技术", 1, done)
            step("舆情", 2, done)
            step("风控", 3, done)
        }
    }

    private fun step(label: String, idx: Int, done: Int): ViewBuilder = {
        Text {
            attr {
                text(mark(idx, done) + " " + label)
                color(colorOf(if (idx < done) Palette.DOWN else Palette.SUB))
                fontSize(12f)
                marginEnd(8f)
            }
        }
    }

    private fun mark(idx: Int, done: Int): String = when {
        idx < done -> "✓"
        idx == done && vm.streamingId.value != null -> "●"
        else -> "○"
    }

    private fun messageBubble(m: ChatMessage): ViewBuilder = {
        View {
            attr { flexGrow(1f) }
            when (m.role) {
                MessageRole.ASSISTANT -> {
                    // 流式文本：最终以 KuiklyMarkdown 渲染（T2-1.1，见 DEVELOPMENT-ISSUES）
                    Text { attr { text(m.content); color(colorOf(Palette.TEXT)) } }
                    if (m.isStreaming) Text { attr { text("▍"); color(colorOf(Palette.UP)) } }
                }
                else -> Text { attr { text(m.content); color(colorOf(Palette.TEXT)) } }
            }
        }
    }
}