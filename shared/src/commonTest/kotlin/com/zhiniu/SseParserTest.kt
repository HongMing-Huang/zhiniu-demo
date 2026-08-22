/* 知牛 · SseParser 纯逻辑单元测试（commonTest，无 Kuikly 依赖）
 * 覆盖：data/event/DONE/空行/错误帧分类。
 */
package com.zhiniu

import com.zhiniu.domain.model.SseParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SseParserTest {

    @Test fun `data line parsed`() {
        val f = SseParser.parseLine("data: {\"a\":1}")
        assertEquals(null, f?.event)
        assertEquals("{\"a\":1}", f?.data)
    }

    @Test fun `event line parsed`() {
        val f = SseParser.parseLine("event: agent_progress")
        assertEquals("agent_progress", f?.event)
        assertNull(f?.data)
    }

    @Test fun `done line parsed`() {
        val f = SseParser.parseLine("data: [DONE]")
        assertNull(f?.event)
        assertEquals("[DONE]", f?.data)
    }

    @Test fun `blank and comment ignored`() {
        assertNull(SseParser.parseLine(""))
        assertNull(SseParser.parseLine("  "))
        assertNull(SseParser.parseLine(": keepalive"))
    }

    @Test fun `isDone detects sentinel`() {
        assertEquals(true, SseParser.isDone("[DONE]"))
        assertEquals(false, SseParser.isDone("{\"a\":1}"))
    }

    @Test fun `kind classification`() {
        assertEquals(SseParser.EventKind.DONE, SseParser.kind(null, "[DONE]"))
        assertEquals(
            SseParser.EventKind.STEP_PROGRESS,
            SseParser.kind("agent_progress", "{\"step\":1}"),
        )
        assertEquals(SseParser.EventKind.ERROR, SseParser.kind("error", "{\"error\":{}}"))
        assertEquals(SseParser.EventKind.ERROR, SseParser.kind(null, "{\"error\":{\"code\":\"rate_limited\"}}"))
        assertEquals(SseParser.EventKind.DATA, SseParser.kind(null, "{\"choices\":[]}"))
    }
}