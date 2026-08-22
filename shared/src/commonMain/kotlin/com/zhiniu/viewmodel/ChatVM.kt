/* 知牛 · ChatVM（Kuikly 响应式，流式对话 + 4 快捷指令）*/
package com.zhiniu.viewmodel

import com.tencent.kuikly.ref.observable.observable
import com.tencent.kuikly.ref.observable.observableList
import com.zhiniu.domain.model.ChatMessage
import com.zhiniu.domain.model.MessageRole
import com.zhiniu.domain.model.StreamChunk
import com.zhiniu.domain.repository.ChatRepository
import com.zhiniu.domain.usecase.AskChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ChatVM(
    private val scope: CoroutineScope,
    repo: ChatRepository,
) {
    private val ask = AskChat(repo)

    val messages = observableList<ChatMessage>()
    val streamingId = observable<String?>(null)
    /** Agent 时间线完成步数（0..4）：由 StreamChunk.Progress 点亮，结束置 4。 */
    val agentDone = observable(0)
    val quickCommands = observableList<String>().apply {
        add("看大盘"); add("诊个股"); add("解释指标"); add("对比两只")
    }

    init {
        messages.add(
            ChatMessage("welcome", MessageRole.ASSISTANT, "你好，我是知牛。可以问我：看大盘 / 诊个股 / 解释指标 / 对比两只。")
        )
    }

    fun send(text: String) {
        val t = text.trim(); if (t.isEmpty()) return
        messages.add(ChatMessage(newId(), MessageRole.USER, t))
        val botId = newId()
        messages.add(ChatMessage(botId, MessageRole.ASSISTANT, "", isStreaming = true))
        streamingId.value = botId
        scope.launch {
            val history = messages.toList().filter { it.content.isNotBlank() }
            val sb = StringBuilder()
            ask.execute("zhiniu/quick", history).collect { chunk ->
                when (chunk) {
                    is StreamChunk.Delta -> {
                        sb.append(chunk.text)
                        update(botId) { it.copy(content = sb.toString()) }
                    }
                    is StreamChunk.Error -> update(botId) {
                        it.copy(content = (it.content.ifBlank { "" } + "\n⚠ ${chunk.msg}").trim(), isStreaming = false)
                    }
                    is StreamChunk.Progress -> agentDone.value = chunk.step.coerceIn(0, 4)
                    is StreamChunk.Done, is StreamChunk.Insight -> { finish(botId); agentDone.value = 4 }
                }
            }
        }
    }

    private fun update(id: String, tf: (ChatMessage) -> ChatMessage) {
        val i = messages.indexOfFirst { it.id == id }
        if (i >= 0) messages[i] = tf(messages[i])
    }

    private fun finish(id: String) {
        if (streamingId.value != id) return
        val i = messages.indexOfFirst { it.id == id }
        if (i >= 0) messages[i] = messages[i].copy(isStreaming = false)
        streamingId.value = null
    }

    private var seq = 0
    private fun newId(): String = "m${System.currentTimeMillis()}_${seq++}"
}