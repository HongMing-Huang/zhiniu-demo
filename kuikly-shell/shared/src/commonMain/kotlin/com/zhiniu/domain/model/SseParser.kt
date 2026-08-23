/* 知牛 · SSE 行解析纯逻辑（KMP 安全，可单测）
 *
 * 后端 /v1/chat/completions 用 "data: ..."（OpenAI 风格）与可选 "event: ..." 双行格式
 * （见 backend/app/main.py 的 _sse(data, event=None)，T2-3 为 agent_progress 打底）。
 * 本层把单行 SSE 文本规整为 (event, data)，客户端据此分发 Delta/Progress/Error/Done。
 */
package com.zhiniu.domain.model

object SseParser {

    /** 一帧 SSE：event 可为空（默认 OpenAI "message"），data 为未转义的 payload 字符串。 */
    data class Frame(val event: String?, val data: String?)

    private const val EVENT_PREFIX = "event:"
    private const val DATA_PREFIX = "data:"

    /**
     * 解析一行 SSE（已 trim）。返回规则：
     * - 空行 / 注释行 → null（客户端据此判断帧边界，通常忽略）
     * - "event: xxx" → Frame("xxx", null)
     * - "data: [DONE]" → Frame(null, "[DONE]")
     * - "data: {...}" → Frame(null, "{...}")
     * - 其它非 SSE 行 → null
     */
    fun parseLine(line: String): Frame? {
        val s = line.trim()
        if (s.isEmpty() || s.startsWith(":")) return null
        return when {
            s.startsWith(EVENT_PREFIX) -> Frame(s.substring(EVENT_PREFIX.length).trim().ifEmpty { null }, null)
            s.startsWith(DATA_PREFIX) -> Frame(null, s.substring(DATA_PREFIX.length).trim())
            else -> null
        }
    }

    /** data payload 是否为结束哨兵 [DONE]。 */
    fun isDone(data: String?): Boolean = data?.trim() == "[DONE]"

    /**
     * 从 event+data 组装业务事件类型：
     * - event == "agent_progress" → STEP_PROGRESS（工具/Agent 阶段进度）
     * - event == "error" 或 data 为含 "error" 的 JSON → ERROR
     * - 其余 → DATA
     */
    fun kind(event: String?, data: String?): EventKind = when {
        isDone(data) -> EventKind.DONE
        event == "agent_progress" -> EventKind.STEP_PROGRESS
        event == "error" -> EventKind.ERROR
        data?.contains("\"error\"") == true -> EventKind.ERROR
        else -> EventKind.DATA
    }

    enum class EventKind { DATA, STEP_PROGRESS, ERROR, DONE }
}