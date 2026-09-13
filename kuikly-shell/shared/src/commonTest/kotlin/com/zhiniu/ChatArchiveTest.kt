/* 知牛 · 聊天归档纯逻辑单元测试（commonTest，无 Kuikly 依赖）
 * 覆盖：会话序列化 roundtrip（含特殊字符转义）、AI 块→记录投影、记录→块恢复。
 */
package com.zhiniu

import com.zhiniu.data.local.ChatArchive
import com.zhiniu.data.local.ChatArchiveSession
import com.zhiniu.data.local.ChatRecord
import com.zhiniu.domain.repository.AiBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatArchiveTest {

    @Test fun `sessions roundtrip preserves fields and order`() {
        val sessions = listOf(
            ChatArchiveSession(
                id = "s101", title = "贵州茅台分析", createdAt = "刚刚",
                records = listOf(
                    ChatRecord(role = "user", text = "分析贵州茅台"),
                    ChatRecord(
                        role = "ai", text = "### 研究归纳",
                        toolTitle = "已加入自选", toolDetail = "贵州茅台（sh600519）", toolAction = "watchlist",
                        verdictRisk = "中", verdictAction = "观望",
                    ),
                ),
            ),
            ChatArchiveSession(id = "s102", title = "新会话", createdAt = "刚刚"),
        )
        val restored = ChatArchive.deserialize(ChatArchive.serialize(sessions))
        assertEquals(2, restored.size)
        assertEquals("s101", restored[0].id)
        assertEquals("贵州茅台分析", restored[0].title)
        assertEquals(2, restored[0].records.size)
        assertEquals("分析贵州茅台", restored[0].records[0].text)
        assertEquals("已加入自选", restored[0].records[1].toolTitle)
        assertEquals("观望", restored[0].records[1].verdictAction)
        assertEquals(0, restored[1].records.size)
    }

    @Test fun `quotes newlines and backslashes survive roundtrip`() {
        val tricky = "他说：\"看「支撑位」\\n第二行\t制表符"
        val sessions = listOf(
            ChatArchiveSession(
                id = "s1", title = tricky, createdAt = "",
                records = listOf(ChatRecord(role = "user", text = tricky)),
            ),
        )
        val restored = ChatArchive.deserialize(ChatArchive.serialize(sessions))
        assertEquals(tricky, restored[0].title)
        assertEquals(tricky, restored[0].records[0].text)
    }

    @Test fun `corrupted payload deserializes to empty not crash`() {
        assertTrue(ChatArchive.deserialize("not a json {").isEmpty())
        assertTrue(ChatArchive.deserialize("").isEmpty())
    }

    @Test fun `ai blocks project to plain text with card summaries`() {
        val blocks = listOf(
            AiBlock.Text("知牛 AI · 通用问答"),
            AiBlock.Text("RSI 高于 70 视为超买。"),
            AiBlock.ToolResult(title = "已加入自选", detail = "宁德时代（sz300750）"),
            AiBlock.Verdict(risk = "中", action = "观望"),
            AiBlock.Metrics(title = "AI 解读", rows = emptyList()),
        )
        val text = ChatArchive.aiTextOf(blocks)
        assertTrue(text.contains("RSI 高于 70"))
        assertTrue(text.contains("（已加入自选：宁德时代（sz300750））"))
        assertTrue(text.contains("【AI观点】风险：中｜操作建议：观望"))
        // 非文本卡片（Metrics）不进投影
        assertTrue(!text.contains("AI 解读"))
    }

    @Test fun `record restores text verdict and tool blocks`() {
        val record = ChatRecord(
            role = "ai", text = "正文",
            toolTitle = "预警已设置", toolDetail = "宁德时代 跌破 300", toolAction = "alert",
            verdictRisk = "低", verdictAction = "买入",
        )
        val blocks = ChatArchive.blocksOf(record)
        assertEquals(3, blocks.size)
        assertEquals("正文", (blocks[0] as AiBlock.Text).content)
        val verdict = blocks[1] as AiBlock.Verdict
        assertEquals("低", verdict.risk)
        assertEquals("买入", verdict.action)
        assertEquals("预警已设置", (blocks[2] as AiBlock.ToolResult).title)
    }

    @Test fun `degraded record restores with fallback header`() {
        val blocks = ChatArchive.blocksOf(ChatRecord(role = "ai", text = "规则回复", degraded = true))
        assertEquals(1, blocks.size)
        val risk = blocks[0] as AiBlock.Risk
        assertTrue(risk.title.contains("规则降级"))
        assertEquals("规则回复", risk.content)
    }
}
