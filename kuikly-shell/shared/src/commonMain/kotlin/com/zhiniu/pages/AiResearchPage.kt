/* 知牛 · AiResearchPage（AI 研究：两栏）
 * 左：会话记录（240px）；中：聊天（结构化 5 类 AiBlock + 流式揭示）。
 * 不再加右栏 Context Rail（避免开发冗余）。Composer 底部 + 圆形 34 发送。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.data.mock.MarketStore
import com.zhiniu.domain.model.ChatSession
import com.zhiniu.domain.repository.AiBlock
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.PAD
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.ai.AiBlockView
import com.zhiniu.pages.components.AiMessageHeader
import com.zhiniu.pages.components.DemoDataNote
import com.zhiniu.pages.components.common.ActivityIndicatorRow
import com.zhiniu.pages.components.common.AppInput
import com.zhiniu.pages.components.common.Divider
import com.zhiniu.pages.components.common.PrimaryButton
import com.zhiniu.pages.components.common.SectionHeader

/** 聊天消息（user 文本 / ai 结构化块 + 流式）。 */
data class AiChatMessage(
    val role: String,
    val text: String = "",
    val blocks: List<AiBlock> = emptyList(),
    val streaming: Boolean = false,
)

@Page("AiResearch", supportInLocal = true)
internal class AiResearchPage : AppBasePage() {

    internal var draft by observable("")
    internal var currentSessionId by observable("s1")
    internal val chatSessions by observableList<ChatSession>()
    internal val messages by observableList<AiChatMessage>()
    private var seq = 0

    private val seedSessions = listOf(
        ChatSession("s1", "贵州茅台分析", "08-28 14:32"),
        ChatSession("s2", "今日市场", "08-28 11:05"),
        ChatSession("s3", "宁德时代", "08-27 16:10"),
        ChatSession("s4", "五粮液", "08-27 09:12"),
    )

    override fun created() {
        super.created()
        if (chatSessions.isEmpty()) {
            seedSessions.forEach { chatSessions.add(it) }
        }
        messages.add(AiChatMessage("ai", blocks = listOf(
            AiBlock.Text("你好，我是知牛 AI。可以分析个股、解读指标、聊聊市场。试试输入：分析贵州茅台。")
        )))
    }

    internal fun send(question: String) {
        val text = question.trim()
        if (text.isEmpty()) return
        messages.add(AiChatMessage("user", text = text))
        draft = ""
        val blocks = MarketStore.aiService.chatReply(currentSessionId, text)
        val aiMsg = AiChatMessage("ai", blocks = emptyList(), streaming = true)
        messages.add(aiMsg)
        var idx = 0
        val step = object { fun go() {
            if (idx < blocks.size) {
                val shown = blocks.subList(0, idx + 1)
                messages[messages.size - 1] = AiChatMessage("ai", blocks = shown, streaming = true)
                idx += 1
                setTimeout(180) { go() }
            } else {
                messages[messages.size - 1] = AiChatMessage("ai", blocks = blocks, streaming = false)
            }
        } }
        setTimeout(200) { step.go() }
        seq += 1
    }

    internal fun selectSession(id: String) {
        currentSessionId = id
        messages.diffUpdate(emptyList())
        messages.add(AiChatMessage("ai", blocks = listOf(
            AiBlock.Text("已切换到会话「${seedSessions.firstOrNull { it.id == id }?.title ?: ""}」。继续提问即可。")
        )))
    }

    internal fun newSession() {
        val id = "s${++seq + 100}"
        chatSessions.add(0, ChatSession(id, "新会话", "刚刚"))
        selectSession(id)
    }

    override fun body(): ViewBuilder = {
        attr {
            flexDirectionColumn()
            backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        renderCommonOverlays(this@AiResearchPage, "AI研究")
        View {
            attr {
                flex(1f); flexDirectionRow()
                backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            sessionColumn(this@AiResearchPage)
            chatColumn(this@AiResearchPage)
        }
    }
}

// ============== 左：会话 ==============
private fun ViewContainer<*, *>.sessionColumn(host: AiResearchPage) {
    val colors = AppTheme.colors
    View {
        attr {
            width(240f)
            flexDirectionColumn()
            backgroundColor(colors.c(colors.surface))
            borderRight(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr {
                height(56f); flexDirectionRow(); alignItemsCenter()
                padding(left = 16f, right = 10f)
                borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            }
            SectionHeader("会话")
            View { attr { flex(1f) } }
            View {
                attr {
                    height(28f); borderRadius(6f); padding(left = 10f, right = 10f)
                    allCenter()
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    cssClass("zn-click")
                    highlightBackgroundColor(colors.ca(colors.textSecondary, 8))
                }
                event { click { host.newSession() } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textSecondary))
                        text("+ 新建")
                    }
                }
            }
        }
        List {
            attr { flex(1f) }
            vfor({ host.chatSessions }) { session ->
                View {
                    attr {
                        height(48f); flexDirectionRow(); alignItemsCenter()
                        padding(left = 16f, right = 16f)
                        backgroundColor(colors.c(if (session.id == host.currentSessionId) colors.surfaceHover else colors.surface))
                        cssClass("zn-row zn-click")
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                    event { click { host.selectSession(session.id) } }
                    Text {
                        attr {
                            fontSize(AppTypography.fs13)
                            color(colors.c(if (session.id == host.currentSessionId) colors.textPrimary else colors.textSecondary))
                            text(session.title)
                        }
                    }
                    View { attr { flex(1f) } }
                    Text {
                        attr {
                            fontSize(AppTypography.fs11)
                            color(colors.c(colors.textTertiary))
                            text(session.createdAt)
                        }
                    }
                }
            }
        }
    }
}

// ============== 中：聊天 ==============
private fun ViewContainer<*, *>.chatColumn(host: AiResearchPage) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f); flexDirectionColumn()
            backgroundColor(colors.c(colors.pageBg))
        }
        List {
            attr { flex(1f) }
            vfor({ host.messages }) { msg ->
                View {
                    attr { padding(top = 14f, left = 32f, right = 32f) }
                    if (msg.role == "user") {
                        View {
                            attr {
                                alignSelfFlexEnd()
                                maxWidth(560f)
                                borderRadius(AppRadius.radius6)
                                backgroundColor(colors.c(colors.surfaceSecondary))
                            }
                            View {
                                attr { padding(top = 8f, left = 14f, right = 14f, bottom = 8f) }
                                Text {
                                    attr {
                                        fontSize(AppTypography.fs14); lineHeight(21f)
                                        color(colors.c(colors.textPrimary))
                                        text(msg.text)
                                    }
                                }
                            }
                        }
                    } else {
                        View { attr { alignSelfStretch(); maxWidth(820f) }
                            AiMessageHeader("知牛 AI")
                            View { attr { height(8f) } }
                            vif({ msg.streaming && msg.blocks.isEmpty() }) {
                                ActivityIndicatorRow("正在分析…")
                            }
                            msg.blocks.forEach { block ->
                                View { attr { marginTop(10f) } }
                                AiBlockView(
                                    block,
                                    onOpenStock = { sym -> host.openStock(sym) },
                                    onAsk = { q -> host.send(q) },
                                )
                            }
                            View { attr { height(10f) } }
                            DemoDataNote()
                        }
                    }
                }
            }
        }
        // Composer
        View {
            attr {
                backgroundColor(colors.c(colors.surface))
                borderTop(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                padding(top = 12f, bottom = 12f, left = 32f, right = 32f)
                flexDirectionRow(); alignItemsCenter()
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            AppInput(
                placeholder = "继续提问，如：分析贵州茅台 / 解释 RSI",
                text = host.draft, height = 38f,
                onTextChange = { host.draft = it },
                onReturn = { host.send(host.draft) },
            )
            View { attr { width(8f) } }
            // 圆形 34 发送
            View {
                attr {
                    width(34f); height(34f)
                    borderRadius(allBorderRadius = 17f)
                    allCenter()
                    backgroundColor(colors.c(if (host.draft.isBlank()) colors.surfaceSecondary else colors.textPrimary))
                    cssClass("zn-click")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                event { click { if (host.draft.isNotBlank()) host.send(host.draft) } }
                Icon(IconKind.SEND, 16f) {
                    if (host.draft.isBlank()) colors.textTertiary else colors.surface
                }
            }
        }
    }
}
