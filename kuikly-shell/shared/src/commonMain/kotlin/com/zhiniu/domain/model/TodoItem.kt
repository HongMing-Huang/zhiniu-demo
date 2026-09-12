/* 知牛 · 待办清单领域模型与纯逻辑（三端基线：新增/编辑/删除/持久化）。
 * 持久化由页面层经 Kuikly 官方 SharedPreferencesModule 落地（Android/iOS/鸿蒙/H5 各端原生实现），
 * 本文件只做可单测的纯逻辑：JSON 序列化与增删改运算。
 */
package com.zhiniu.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 单条待办。id 为存储集合内自增正整数，删除后不复用。 */
@Serializable
data class TodoItem(
    val id: Long,
    val title: String,
    val done: Boolean = false,
    @SerialName("created_at") val createdAt: Long = 0L,
)

/** 待办集合的纯逻辑运算与 JSON 序列化（commonTest 覆盖）。 */
object TodoStore {

    private val json = Json { ignoreUnknownKeys = true }
    private const val MAX_TITLE_LENGTH = 60

    fun serialize(items: List<TodoItem>): String = json.encodeToString(items)

    /** 损坏/历史脏数据一律回退为空列表（绝不因存储内容抛错白屏）。 */
    fun deserialize(raw: String?): List<TodoItem> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return emptyList()
        return runCatching { json.decodeFromString<List<TodoItem>>(text) }.getOrDefault(emptyList())
    }

    /** 规范化新增/编辑输入：trim、限长、空串拒绝。返回 null 表示无效输入。 */
    fun normalizeTitle(raw: String): String? =
        raw.trim().takeIf { it.isNotEmpty() }?.take(MAX_TITLE_LENGTH)

    fun nextId(items: List<TodoItem>): Long = (items.maxOfOrNull { it.id } ?: 0L) + 1

    fun add(items: List<TodoItem>, title: String, nowMs: Long = 0L): List<TodoItem> {
        val normalized = normalizeTitle(title) ?: return items
        return items + TodoItem(id = nextId(items), title = normalized, createdAt = nowMs)
    }

    fun rename(items: List<TodoItem>, id: Long, title: String): List<TodoItem> {
        val normalized = normalizeTitle(title) ?: return items
        return items.map { if (it.id == id) it.copy(title = normalized) else it }
    }

    fun toggle(items: List<TodoItem>, id: Long): List<TodoItem> =
        items.map { if (it.id == id) it.copy(done = !it.done) else it }

    fun remove(items: List<TodoItem>, id: Long): List<TodoItem> =
        items.filterNot { it.id == id }

    fun clearDone(items: List<TodoItem>): List<TodoItem> =
        items.filterNot { it.done }
}
