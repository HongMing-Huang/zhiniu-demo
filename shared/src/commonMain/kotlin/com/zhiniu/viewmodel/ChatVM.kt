/* 知牛 · ChatVM（流式对话 + 4 快捷指令）*/
package com.zhiniu.viewmodel

import com.zhiniu.domain.model.ChatMessage
import com.zhiniu.domain.model.MessageRole
import com.zhiniu.domain.model.StreamChunk
import com.zhiniu.domain.repository.ChatRepository
import com.zhiniu.domain.usecase.AskChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatVM(
    private val scope: CoroutineScope,
    repo: ChatRepository,
) {
    private val askChat = AskChat(repo)
    private val _ui = MutableStateFlow(ChatUiState())
    val ui: StateFlow<ChatUiState> = _ui.asStateFlow()

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val userMsg = ChatMessage(newId(), MessageRole.USER, trimmed)
        val botId = newId()
        val botMsg = ChatMessage(botId, MessageRole.ASSISTANT, "", isStreaming = true)
        _ui.value = _ui.value.let {
            it.copy(
                messages = it.messages + userMsg + botMsg,
                streamingId = botId,
            )
        }
        scope.launch {
            val history = _ui.value.messages.filter { it.content.isNotBlank() }
            val sb = StringBuilder()
            askChat.execute("zhiniu/quick", history).collect { chunk ->
                when (chunk) {
                    is StreamChunk.Delta -> {
                        sb.append(chunk.text)
                        updateBot(botId) { it.copy(content = sb.toString()) }
                    }
                    is StreamChunk.Error -> updateBot(botId) {
                        it.copy(content = (it.content.ifBlank { "" } + "\n⚠ ${chunk.msg}").trim(), isStreaming = false)
                    }
                    is StreamChunk.Done, is StreamChunk.Insight -> finishBot(botId)
                    is StreamChunk.Progress -> Unit
                }
            }
        }
    }

    private fun updateBot(id: String, transform: (ChatMessage) -> ChatMessage) {
        _ui.value = _ui.value.copy(messages = _ui.value.messages.map { if (it.id == id) transform(it) else it })
    }

    private fun finishBot(id: String) {
        if (_ui.value.streamingId != id) return
        _ui.value = _ui.value.copy(
            messages = _ui.value.messages.map { if (it.id == id) it.copy(isStreaming = false) else it },
            streamingId = null,
        )
    }

    private var seq = 0
    private fun newId(): String = "m${System.currentTimeMillis()}_${seq++}"
}