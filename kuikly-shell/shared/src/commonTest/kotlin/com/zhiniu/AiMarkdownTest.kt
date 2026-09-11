/* 知牛 · Markdown 解析单元测试（commonTest，纯函数无 Kuikly 依赖）
 * 覆盖：标题/段落/列表/表格/代码块/引用/分隔线/行内样式与未闭合容错。
 */
package com.zhiniu

import com.zhiniu.pages.components.ai.MdBlock
import com.zhiniu.pages.components.ai.parseInlineSpans
import com.zhiniu.pages.components.ai.parseMarkdown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiMarkdownTest {

    @Test fun `heading levels parsed`() {
        val blocks = parseMarkdown("# 标题一\n## 标题二\n### 标题三")
        assertEquals(3, blocks.size)
        assertEquals(1, (blocks[0] as MdBlock.Heading).level)
        assertEquals(2, (blocks[1] as MdBlock.Heading).level)
        assertEquals(3, (blocks[2] as MdBlock.Heading).level)
    }

    @Test fun `paragraph merges consecutive lines`() {
        val blocks = parseMarkdown("第一行\n第二行\n\n新段落")
        assertEquals(2, blocks.size)
        val p1 = blocks[0] as MdBlock.Paragraph
        assertTrue(p1.spans.first().text.contains("第一行 第二行"))
    }

    @Test fun `bullets and ordered lists`() {
        val blocks = parseMarkdown("- 甲\n- 乙\n\n1. one\n2. two")
        val bullets = blocks[0] as MdBlock.Bullets
        val ordered = blocks[1] as MdBlock.OrderedList
        assertEquals(listOf("甲", "乙"), bullets.items.map { it.first().text })
        assertEquals(listOf("one", "two"), ordered.items.map { it.first().text })
    }

    @Test fun `table with header separator and rows`() {
        val src = "| 指标 | 值 |\n| --- | --- |\n| RSI | 54.7 |\n| MA20 | 1301 |"
        val table = parseMarkdown(src).single() as MdBlock.Table
        assertEquals(listOf("指标", "值"), table.header)
        assertEquals(2, table.rows.size)
        assertEquals("54.7", table.rows[0][1])
    }

    @Test fun `pipe line without separator is paragraph`() {
        val blocks = parseMarkdown("| 单行不是表格 |")
        assertTrue(blocks.single() is MdBlock.Paragraph)
    }

    @Test fun `code fence captures lines and language`() {
        val src = "```python\nprint(1)\nprint(2)\n```"
        val code = parseMarkdown(src).single() as MdBlock.Code
        assertEquals("python", code.language)
        assertEquals(listOf("print(1)", "print(2)"), code.lines)
    }

    @Test fun `unclosed code fence runs to end`() {
        val code = parseMarkdown("```\nx=1").single() as MdBlock.Code
        assertEquals(listOf("x=1"), code.lines)
    }

    @Test fun `quote joins lines and divider recognized`() {
        val blocks = parseMarkdown("> 引用一\n> 引用二\n\n---")
        assertEquals(listOf("引用一 引用二"), (blocks[0] as MdBlock.Quote).spans.map { it.text })
        assertTrue(blocks[1] is MdBlock.Divider)
    }

    @Test fun `inline bold and code spans`() {
        val spans = parseInlineSpans("看 **RSI 超买** 与 `ma20` 值")
        assertEquals(4, spans.size)
        assertTrue(spans[1].bold)
        assertTrue(spans[3].code)
        assertEquals("RSI 超买", spans[1].text)
    }

    @Test fun `unclosed markers fall back to plain text`() {
        val spans = parseInlineSpans("a ** b ` c")
        assertEquals(1, spans.size)
        assertEquals("a ** b ` c", spans[0].text)
    }
}
