// 知牛 · AI 回复 Markdown 渲染（Task2 评分点：段落/列表/表格/代码块 + 标题组织）
// 解析器为纯函数（commonTest 覆盖）；渲染只用 Kuikly 官方组件（RichText/Span/View/Text）。
package com.zhiniu.pages.components.ai

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c

// ---------- 行内片段 ----------
/** 行内片段：普通文本 / **加粗** / `行内代码`。 */
data class MdSpan(val text: String, val bold: Boolean = false, val code: Boolean = false)

// ---------- 块级结构 ----------
sealed class MdBlock {
    /** `#`/`##`/`###` 标题。 */
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock()
    /** 普通段落（连续非空行合并）。 */
    data class Paragraph(val spans: List<MdSpan>) : MdBlock()
    /** `- `/`* ` 无序列表。 */
    data class Bullets(val items: List<List<MdSpan>>) : MdBlock()
    /** `1.` 有序列表。 */
    data class OrderedList(val items: List<List<MdSpan>>) : MdBlock()
    /** `> ` 引用（连续行合并为一段）。 */
    data class Quote(val spans: List<MdSpan>) : MdBlock()
    /** ``` 围栏代码块。 */
    data class Code(val language: String, val lines: List<String>) : MdBlock()
    /** `| a | b |` 表格（第 2 行为 `---` 分隔线）。 */
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock()
    /** `---` 分隔线。 */
    object Divider : MdBlock()
}

/** 行内解析：识别 `**加粗**` 与 `` `行内代码` ``，未闭合按普通文本。 */
fun parseInlineSpans(s: String): List<MdSpan> {
    val spans = mutableListOf<MdSpan>()
    val plain = StringBuilder()
    var i = 0
    fun flush() {
        if (plain.isNotEmpty()) {
            spans += MdSpan(plain.toString())
            plain.clear()
        }
    }
    while (i < s.length) {
        when {
            s.startsWith("**", i) -> {
                val end = s.indexOf("**", i + 2)
                if (end > i + 1) {
                    flush()
                    spans += MdSpan(s.substring(i + 2, end), bold = true)
                    i = end + 2
                } else {
                    plain.append(s[i]); i++
                }
            }
            s[i] == '`' -> {
                val end = s.indexOf('`', i + 1)
                if (end > i) {
                    flush()
                    spans += MdSpan(s.substring(i + 1, end), code = true)
                    i = end + 1
                } else {
                    plain.append(s[i]); i++
                }
            }
            else -> {
                plain.append(s[i]); i++
            }
        }
    }
    flush()
    return spans.ifEmpty { listOf(MdSpan("")) }
}

private fun isBullet(line: String) = line.startsWith("- ") || line.startsWith("* ")
private fun isOrdered(line: String): Boolean {
    val dot = line.indexOf(". ")
    if (dot <= 0) return false
    val n = line.substring(0, dot)
    return n.isNotEmpty() && n.all { it.isDigit() }
}

private fun isTableRow(line: String) = line.startsWith("|") && line.endsWith("|") && line.length >= 2

private fun splitTableRow(line: String): List<String> =
    line.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

private fun isTableSeparator(line: String): Boolean =
    isTableRow(line) && splitTableRow(line).all { it.matches(Regex(":?-{3,}:?")) }

/**
 * 行级 Markdown 解析（面向 LLM 输出的常用子集）：
 * 标题 / 段落 / 无序有序列表 / 引用 / 围栏代码块 / 表格 / 分隔线。
 */
fun parseMarkdown(src: String): List<MdBlock> {
    val lines = src.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> i++

            trimmed.startsWith("```") -> {
                val language = trimmed.removePrefix("```").trim()
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code += lines[i]
                    i++
                }
                i++ // 跳过闭合 ```（缺失则到文末）
                blocks += MdBlock.Code(language, code)
            }

            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                blocks += MdBlock.Heading(level, parseInlineSpans(trimmed.dropWhile { it == '#' }.trim()))
                i++
            }

            isTableSeparator(lines.getOrNull(i + 1)?.trim() ?: "") && isTableRow(trimmed) -> {
                val header = splitTableRow(trimmed)
                i += 2 // 跳过表头 + 分隔行
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && isTableRow(lines[i].trim())) {
                    rows += splitTableRow(lines[i].trim())
                    i++
                }
                blocks += MdBlock.Table(header, rows)
            }

            isBullet(trimmed) -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val t = lines[i].trim()
                    if (isBullet(t)) {
                        items += t.substring(2).trim(); i++
                    } else if (t.isNotEmpty() && items.isNotEmpty() && !isOrdered(t) &&
                        !t.startsWith("#") && !isTableRow(t)
                    ) {
                        items[items.size - 1] += " " + t; i++ // 列表项折行
                    } else break
                }
                blocks += MdBlock.Bullets(items.map { parseInlineSpans(it) })
            }

            isOrdered(trimmed) -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val t = lines[i].trim()
                    if (isOrdered(t)) {
                        items += t.substring(t.indexOf(". ") + 2).trim(); i++
                    } else if (t.isNotEmpty() && items.isNotEmpty() && !isBullet(t) &&
                        !t.startsWith("#") && !isTableRow(t)
                    ) {
                        items[items.size - 1] += " " + t; i++
                    } else break
                }
                blocks += MdBlock.OrderedList(items.map { parseInlineSpans(it) })
            }

            trimmed.startsWith("> ") || trimmed == ">" -> {
                val quote = mutableListOf<String>()
                while (i < lines.size) {
                    val t = lines[i].trim()
                    if (t.startsWith("> ")) {
                        quote += t.substring(2).trim(); i++
                    } else if (t == ">") {
                        quote += ""; i++
                    } else break
                }
                blocks += MdBlock.Quote(parseInlineSpans(quote.joinToString(" ").trim()))
            }

            trimmed == "---" || trimmed == "***" -> {
                blocks += MdBlock.Divider
                i++
            }

            else -> {
                // 表格行但下一行不是分隔线：按普通段落文本
                val para = mutableListOf(trimmed)
                i++
                while (i < lines.size) {
                    val t = lines[i].trim()
                    if (t.isEmpty() || t.startsWith("#") || isBullet(t) || isOrdered(t) ||
                        t.startsWith("```") || t == "---" || t == "***" ||
                        t.startsWith("> ") || (isTableRow(t) && isTableSeparator(lines.getOrNull(i + 1)?.trim() ?: ""))
                    ) break
                    para += t
                    i++
                }
                blocks += MdBlock.Paragraph(parseInlineSpans(para.joinToString(" ")))
            }
        }
    }
    return blocks
}

// ---------- 渲染（Kuikly 官方组件） ----------

/** 段落 / 标题 / 引用的行内片段统一渲染入口。 */
private fun ViewContainer<*, *>.richSpans(
    spans: List<MdSpan>,
    baseSize: Float,
    baseColor: Color,
    lineHeight: Float = baseSize * 1.55f,
) {
    val colors = AppTheme.colors
    RichText {
        attr {
            fontSize(baseSize)
            lineHeight(lineHeight)
            color(baseColor)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        spans.forEach { s ->
            Span {
                text(s.text)
                color(baseColor)
                if (s.bold) {
                    fontWeightSemiBold()
                    color(colors.c(colors.textPrimary))
                }
                if (s.code) fontFamily(NUM_FONT)
            }
        }
    }
}

/** Markdown 整体渲染：AI 消息正文入口（纯文本也是合法 Markdown）。 */
fun ViewContainer<*, *>.MarkdownView(content: String) {
    val colors = AppTheme.colors
    parseMarkdown(content).forEachIndexed { index, block ->
        if (index > 0) View { attr { height(8f) } }
        when (block) {
            is MdBlock.Heading -> {
                View { attr { marginTop(if (index > 0) 4f else 0f) } }
                richSpans(
                    block.spans,
                    baseSize = when (block.level) {
                        1 -> AppTypography.fs16
                        2 -> AppTypography.fs15
                        else -> AppTypography.fs14
                    },
                    baseColor = colors.c(colors.textPrimary),
                )
            }

            is MdBlock.Paragraph -> richSpans(block.spans, AppTypography.fs13, colors.c(colors.textSecondary))

            is MdBlock.Bullets -> block.items.forEach { item ->
                View {
                    attr { flexDirectionRow(); marginTop(2f) }
                    Text {
                        attr {
                            fontSize(AppTypography.fs13); lineHeight(21f)
                            color(colors.c(colors.textTertiary)); text("·")
                            marginRight(6f)
                        }
                    }
                    View {
                        attr { flex(1f) }
                        richSpans(item, AppTypography.fs13, colors.c(colors.textSecondary))
                    }
                }
            }

            is MdBlock.OrderedList -> block.items.forEachIndexed { n, item ->
                View {
                    attr { flexDirectionRow(); marginTop(2f) }
                    Text {
                        attr {
                            fontSize(AppTypography.fs13); lineHeight(21f)
                            color(colors.c(colors.textTertiary)); text("${n + 1}.")
                            marginRight(6f)
                        }
                    }
                    View {
                        attr { flex(1f) }
                        richSpans(item, AppTypography.fs13, colors.c(colors.textSecondary))
                    }
                }
            }

            is MdBlock.Quote -> View {
                attr {
                    flexDirectionRow(); marginTop(2f)
                    borderRadius(AppRadius.radius6)
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                View {
                    attr {
                        width(2f); alignSelfStretch()
                        backgroundColor(colors.c(colors.borderStrong))
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
                View {
                    attr { flex(1f); padding(top = 8f, bottom = 8f, left = 10f, right = 10f) }
                    richSpans(block.spans, AppTypography.fs13, colors.c(colors.textSecondary))
                }
            }

            is MdBlock.Code -> View {
                attr {
                    marginTop(2f); borderRadius(AppRadius.radius6)
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    padding(top = 8f, bottom = 8f, left = 10f, right = 10f)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                block.lines.forEach { codeLine ->
                    Text {
                        attr {
                            fontSize(AppTypography.fs12); lineHeight(19f)
                            fontFamily(NUM_FONT)
                            color(colors.c(colors.textPrimary)); text(codeLine)
                        }
                    }
                }
            }

            is MdBlock.Table -> View {
                attr { marginTop(2f); borderRadius(AppRadius.radius6); overflow(false) }
                View {
                    attr {
                        flexDirectionRow()
                        backgroundColor(colors.c(colors.surfaceSecondary))
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                    block.header.forEach { cell ->
                        View {
                            attr { flex(1f); padding(top = 6f, bottom = 6f, left = 8f, right = 8f) }
                            Text {
                                attr {
                                    fontSize(AppTypography.fs12); lineHeight(18f)
                                    fontWeightSemiBold()
                                    color(colors.c(colors.textPrimary)); text(cell)
                                }
                            }
                        }
                    }
                }
                block.rows.forEachIndexed { rowIndex, row ->
                    View {
                        attr {
                            flexDirectionRow()
                            borderTop(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                            backgroundColor(colors.c(colors.surface))
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                        row.forEach { cell ->
                            View {
                                attr { flex(1f); padding(top = 6f, bottom = 6f, left = 8f, right = 8f) }
                                richSpans(
                                    parseInlineSpans(cell),
                                    AppTypography.fs12,
                                    colors.c(colors.textSecondary),
                                    lineHeight = 18f,
                                )
                            }
                        }
                    }
                }
            }

            MdBlock.Divider -> View {
                attr {
                    marginTop(4f); height(1f)
                    backgroundColor(colors.c(colors.border))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
    }
}
