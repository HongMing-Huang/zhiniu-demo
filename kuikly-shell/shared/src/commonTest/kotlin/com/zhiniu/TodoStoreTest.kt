/* 知牛 · TodoStore 单元测试（commonTest，纯函数无 Kuikly 依赖）
 * 覆盖：新增/编辑/删除/切换完成/清除已完成/自增 id/输入规范化/JSON 往返与脏数据容错。
 */
package com.zhiniu

import com.zhiniu.domain.model.TodoItem
import com.zhiniu.domain.model.TodoStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodoStoreTest {

    private val seed = listOf(TodoItem(1, "写周报"), TodoItem(2, "复盘行情", done = true))

    @Test fun `add normalizes title and increments id`() {
        val next = TodoStore.add(seed, "  提交验收材料  ")
        assertEquals(3, next.size)
        assertEquals(3, next.last().id) // max(2)+1，删除后不复用
        assertEquals("提交验收材料", next.last().title)
    }

    @Test fun `add rejects blank input`() {
        assertTrue(TodoStore.add(seed, "   ").size == seed.size)
        assertNull(TodoStore.normalizeTitle(" 　"))
    }

    @Test fun `add truncates overlong title`() {
        val next = TodoStore.add(emptyList(), "长".repeat(100))
        assertEquals(60, next.single().title.length)
    }

    @Test fun `rename keeps others untouched and rejects blank`() {
        val next = TodoStore.rename(seed, 1, "写月报")
        assertEquals("写月报", next.first { it.id == 1L }.title)
        assertEquals(seed, TodoStore.rename(seed, 1, "  "))
    }

    @Test fun `toggle flips only target`() {
        val next = TodoStore.toggle(seed, 1)
        assertTrue(next.first { it.id == 1L }.done)
        assertTrue(next.first { it.id == 2L }.done) // 原本已完成保持不变
        assertEquals(2, next.size)
    }

    @Test fun `remove and clearDone`() {
        assertEquals(1, TodoStore.remove(seed, 1).size)
        assertEquals(1, TodoStore.clearDone(seed).size)
        assertEquals("写周报", TodoStore.clearDone(seed).single().title)
    }

    @Test fun `json round trip preserves items`() {
        val raw = TodoStore.serialize(seed)
        assertEquals(seed, TodoStore.deserialize(raw))
    }

    @Test fun `deserialize tolerates dirty storage`() {
        assertEquals(emptyList(), TodoStore.deserialize(null))
        assertEquals(emptyList(), TodoStore.deserialize(""))
        assertEquals(emptyList(), TodoStore.deserialize("not-json{{{"))
        // 未知字段忽略、缺省字段回默认
        val legacy = """[{"id":9,"title":"旧数据","extra":1}]"""
        assertEquals(listOf(TodoItem(9, "旧数据")), TodoStore.deserialize(legacy))
    }
}
