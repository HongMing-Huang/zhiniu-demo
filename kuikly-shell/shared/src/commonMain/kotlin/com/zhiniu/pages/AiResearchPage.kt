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
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.data.mock.MarketStore
import com.zhiniu.data.remote.GatewayMarketClient
import com.zhiniu.data.remote.AgentResearchResult
import com.zhiniu.domain.model.ChatSession
import com.zhiniu.domain.repository.AiBlock
import com.zhiniu.domain.repository.MetricCell
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
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtMarketCap
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.ai.AiBlockView
import com.zhiniu.pages.components.AiMessageHeader
import com.zhiniu.pages.components.common.AppInput
import com.zhiniu.pages.components.common.Divider
import com.zhiniu.pages.components.common.PrimaryButton
import com.zhiniu.pages.components.common.SecondaryButton
import com.zhiniu.pages.components.common.SectionHeader
import com.tencent.kuikly.core.coroutines.launch

/** 聊天消息（user 文本 / ai 结构化块 + 流式）。 */
data class AiChatMessage(
    val role: String,
    val text: String = "",
    val blocks: List<AiBlock> = emptyList(),
    val streaming: Boolean = false,
    val progress: List<String> = emptyList(),
    val requestId: String = "",
)

@Page("AiResearch", supportInLocal = true)
internal class AiResearchPage : AppBasePage() {

    internal var draft by observable("")
    internal var currentSessionId by observable("s1")
    // TradingAgents 借鉴：角色视图（分析师 / 多空对抗 / 风控）
    internal var roleView by observable("分析")
    internal val chatSessions by observableList<ChatSession>()
    internal val messages by observableList<AiChatMessage>()
    internal var chatListRef: ViewRef<ListView<*, *>>? = null
    private var seq = 0
    /** 最近一次研究结果：角色视图切换时按视角复述真实辩论内容，不再只发引导语。 */
    internal var lastResearch: AgentResearchResult? = null

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
        loadSession("s1")
        // 图表选点 / 详情页 CTA 携问题跳入：自动开始研究（进程内单例传参，URL 直达不支持自定义参数）
        com.zhiniu.base.PendingAsk.question?.let { pending ->
            com.zhiniu.base.PendingAsk.question = null
            send(pending)
        }
        lifecycleScope.launch {
            runCatching { GatewayMarketClient.quotes(repo.stockQuotes().map { it.symbol }) }
                .onSuccess { live ->
                    if (live.isNotEmpty()) {
                        MarketStore.applyLiveQuotes(live)
                        loadSession(currentSessionId)
                    }
                }
        }
    }

    internal fun send(question: String) {
        val text = question.trim()
        if (text.isEmpty()) return
        messages.add(AiChatMessage("user", text = text))
        draft = ""
        val target = repo.stockQuotes().firstOrNull {
            text.contains(it.name) || text.contains(it.code) ||
                (it.pinyin.isNotBlank() && text.contains(it.pinyin, ignoreCase = true))
        }
        if (target == null) {
            sendGeneralQuestion(text)
            return
        }

        val token = "research-${++seq}"
        val messageIndex = messages.size
        messages.add(AiChatMessage("ai", streaming = true, progress = listOf("行情 Agent"), requestId = token))
        lifecycleScope.launch {
            // 首选类型化 SSE：阶段帧实时驱动进度条（行情→技术面→财务→资讯→风险→多头→空头→研究经理→交易员→风控→归纳）
            val steps = mutableListOf<String>()
            val streamed = runCatching {
                GatewayMarketClient.researchStream(target.symbol, text) { event ->
                    if (event.label.isNotBlank() && event.label !in steps) {
                        steps += event.label
                        updateResearchProgress(messageIndex, token, steps.toList())
                    }
                }
            }.getOrNull()
            // 流不可用（旧网关 / 代理不支持流式）→ 回退一次性研究接口
            val result = streamed ?: runCatching { GatewayMarketClient.research(target.symbol, text) }.getOrNull()
            if (!isPending(messageIndex, token)) return@launch
            lastResearch = result ?: lastResearch
            val blocks = result?.let { researchBlocks(it) } ?: listOf(
                AiBlock.Risk("研究网关暂不可用", "已保留本地快照回答；请稍后重试以获取带来源的实时证据。"),
            ) + MarketStore.aiService.chatReply(currentSessionId, text)
            messages[messageIndex] = AiChatMessage("ai", blocks = blocks, streaming = false)
        }
    }

    /**
     * 非个股问题（快捷指令 / 指标解释 / 方法论）→ 后端 /agent/chat 真 LLM 问答；
     * 网关不可用回退本地 Mock 回复（离线演示不白屏）。
     * 上下文标的取最近一次研究，回答中的价格数字锚定该快照。
     */
    private fun sendGeneralQuestion(text: String) {
        val token = "chat-${++seq}"
        val messageIndex = messages.size
        messages.add(AiChatMessage("ai", streaming = true, progress = listOf("模型网关"), requestId = token))
        val contextSymbol = lastResearch?.symbol.orEmpty()
        lifecycleScope.launch {
            val history = messages.takeLast(9).dropLast(1).map { (it.role to it.text) }
            val reply = runCatching {
                GatewayMarketClient.agentChat(text, history, contextSymbol)
            }.getOrNull()
            if (!isPending(messageIndex, token)) return@launch
            if (reply != null) {
                val header = if (reply.isLlm) {
                    AiBlock.Text("知牛 AI · 通用问答")
                } else {
                    AiBlock.Risk("规则降级 · 未伪装模型", reply.content)
                }
                val body = if (reply.isLlm) markdownOf(reply.content) else emptyList()
                messages[messageIndex] = AiChatMessage("ai", blocks = listOf(header) + body, streaming = false)
            } else {
                // 离线兜底：本地 Mock（内容确定性，无网络依赖）
                revealLocalReply(MarketStore.aiService.chatReply(currentSessionId, text), messageIndex, token)
            }
        }
    }

    // AiBlock.Text 渲染端即 MarkdownView（标题/列表/表格/代码块全支持），通用问答直接喂原文
    private fun markdownOf(content: String): List<AiBlock> = listOf(AiBlock.Text(content))

    private fun isPending(index: Int, token: String): Boolean =
        index < messages.size && messages[index].requestId == token

    private fun updateResearchProgress(index: Int, token: String, steps: List<String>) {
        if (!isPending(index, token)) return
        messages[index] = AiChatMessage("ai", streaming = true, progress = steps, requestId = token)
    }

    private fun revealLocalReply(blocks: List<AiBlock>) {
        val index = messages.size
        messages.add(AiChatMessage("ai", streaming = true, progress = listOf("市场快照")))
        setTimeout(220) {
            if (index < messages.size) messages[index] = AiChatMessage("ai", blocks = blocks)
        }
    }

    /** 已有占位消息（chat token 持有中）的离线兜底：替换为本地 Mock 回复。 */
    private fun revealLocalReply(blocks: List<AiBlock>, index: Int, token: String) {
        if (!isPending(index, token)) return
        messages[index] = AiChatMessage("ai", blocks = blocks, streaming = false)
    }

    /** 研究报告 → 结构化卡片（课题：趋势 / 压力位 / 支撑位 / 投资建议，以卡片、按钮呈现）。 */
    private fun researchBlocks(result: AgentResearchResult): List<AiBlock> {
        val trend = result.trend.ifBlank {
            when (result.direction) {
                "up" -> "上行"
                "down" -> "下行"
                "sideways" -> "震荡"
                else -> "样本不足"
            }
        }
        val sources = result.stages.map { it.source }
            .filter { it.isNotBlank() && it != "upstream-unavailable" }
            .distinct().joinToString(" · ").ifBlank { "研究网关" }
        val risks = if (result.riskFlags.isEmpty()) "当前数据未触发规则风险项；仍需关注市场波动与数据时效。"
        else result.riskFlags.joinToString("；")
        val modeLabel = if (result.synthesisMode == "llm") "${result.synthesisProvider} · 多 Agent 辩论" else "规则降级 · 未伪装模型"
        val levelRows = buildList {
            add(MetricCell("短期趋势", trend))
            add(MetricCell("投资建议", result.rating.ifBlank { result.stance.ifBlank { "数据不足" } }))
            add(MetricCell("压力位", result.pressure?.let { fmt2(it) } ?: "—"))
            add(MetricCell("支撑位", result.support?.let { fmt2(it) } ?: "—"))
            add(MetricCell("置信度", "${(result.confidence * 100).toInt()}%"))
            add(MetricCell("归纳模式", modeLabel))
        }
        return listOf(
            // Task2 评分点「AI 返回内容渲染 / 组织呈现：标题 + 答案来源」：
            // Markdown（标题 / 引用 / 表格）与结构化证据卡混排。
            AiBlock.Text(
                buildString {
                    appendLine("### ${result.name.ifBlank { result.symbol }} · 研究归纳")
                    val metaParts = listOf("来源：$sources", "归纳：$modeLabel", result.reportDate).filter { it.isNotBlank() }
                    appendLine("> " + metaParts.joinToString(" · "))
                    appendLine()
                    appendLine(
                        result.summary.ifBlank {
                            "${result.name.ifBlank { result.symbol }}研究已完成。结论与来源分开展示。"
                        },
                    )
                    appendLine()
                    appendLine("**核心结论速览**")
                    appendLine()
                    appendLine("| 项目 | 值 |")
                    appendLine("| --- | --- |")
                    appendLine(
                        "| 现价 | **${fmt2(result.price)}（${fmtPct(result.changePercent)}）** |",
                    )
                    appendLine("| 短期趋势 | $trend |")
                    appendLine("| 研究经理评级 | ${result.rating.ifBlank { result.stance.ifBlank { "数据不足" } }} |")
                    appendLine(
                        "| 压力 / 支撑 | ${result.pressure?.let { fmt2(it) } ?: "—"} / ${result.support?.let { fmt2(it) } ?: "—"} |",
                    )
                    appendLine("| RSI(14) / MA20 | ${fmt2(result.rsi14)} / ${fmt2(result.ma20)} |")
                },
            ),
            AiBlock.StockCard(
                symbol = result.symbol,
                trend = result.stance.ifBlank { trend },
                rsi = result.rsi14,
                summary = "区间涨跌 ${fmtPct(result.rangeChangePercent)}，MA20 ${fmt2(result.ma20)}。",
            ),
            AiBlock.Metrics(title = "AI 解读", rows = levelRows),
            AiBlock.Metrics(
                title = "研究证据",
                rows = listOf(
                    MetricCell("最新价", fmt2(result.price)),
                    MetricCell("当日涨跌", fmtPct(result.changePercent)),
                    MetricCell("RSI(14)", fmt2(result.rsi14)),
                    MetricCell("MA20", fmt2(result.ma20)),
                    MetricCell("资讯", "${result.newsCount} 条 · ${result.newsSource}"),
                    MetricCell("市值", result.marketCap?.let { fmtMarketCap(it) } ?: "—"),
                ),
            ),
        ) + listOfNotNull(
            // Task2 评分点「结构化卡片」：点位建议 / 风险分级（服务端确定性推导）
            if (result.adviceAvailable) {
                AiBlock.TradeAdvice(result.adviceBuyRange, result.adviceSellRange, result.adviceRationale, result.adviceBasis)
            } else null,
            if (result.riskLevel.isNotBlank()) AiBlock.RiskLevel(result.riskLevel, result.riskLevelLabel, result.riskRationale) else null,
            if (result.klineCloses.size > 1) AiBlock.KLine(result.klineCloses) else null,
        ) + debateBlocks(result) + listOf(
            AiBlock.Risk("风险与时效", "$risks 本内容仅供研究参考，不构成投资建议。"),
            AiBlock.FollowUps(listOf("多空双方分歧在哪", "风控怎么看这只股票", "结合日K分析", "解释 RSI 指标")),
        )
    }

    /** 多空辩论摘要：多头 / 空头各一段（来自 TradingAgents 结构管线或规则引擎的同构输出）。 */
    private fun debateBlocks(result: AgentResearchResult): List<AiBlock> {
        if (result.bullPoints.isEmpty() && result.bearPoints.isEmpty()) return emptyList()
        return listOf(
            AiBlock.Metrics(
                title = "多空对抗",
                rows = result.bullPoints.map { MetricCell("多头", it) } + result.bearPoints.map { MetricCell("空头", it) },
            ),
        )
    }

    /** 风控视角：三方风控辩论 + 交易员价格化观察方案（价位均取自证据，非模型编造）。 */
    private fun riskBlocks(result: AgentResearchResult): List<AiBlock> {
        val notes = result.riskNotes.ifEmpty { result.riskFlags }
        val traderRows = buildList {
            result.traderEntry?.let { add(MetricCell("观察入场", fmt2(it))) }
            result.traderStop?.let { add(MetricCell("止损参考", fmt2(it))) }
            result.pressure?.let { add(MetricCell("压力位", fmt2(it))) }
            result.support?.let { add(MetricCell("支撑位", fmt2(it))) }
            if (result.traderPlan.isNotBlank()) add(MetricCell("观察思路", result.traderPlan))
        }
        return buildList {
            add(AiBlock.Risk("风控辩论", notes.joinToString("\n").ifBlank { "未触发规则风险项，仍需关注波动与数据时效。" }))
            if (traderRows.isNotEmpty()) add(AiBlock.Metrics("交易员观察方案（非交易指令）", traderRows))
        }
    }

    internal fun selectSession(id: String) {
        currentSessionId = id
        loadSession(id)
    }

    private fun loadSession(id: String) {
        messages.diffUpdate(emptyList())
        val question = when (id) {
            "s1" -> "分析贵州茅台，重点看趋势、量能和风险"
            "s2" -> "今天市场强弱如何？"
            "s3" -> "分析宁德时代当前技术结构"
            "s4" -> "五粮液当前有哪些风险信号？"
            else -> ""
        }
        if (question.isEmpty()) {
            messages.add(AiChatMessage("ai", blocks = listOf(AiBlock.Text("新会话已创建。输入股票名称、代码或指标问题开始研究。"))))
            return
        }
        messages.add(AiChatMessage("user", text = question))
        messages.add(AiChatMessage("ai", blocks = MarketStore.aiService.chatReply(id, question)))
    }

    internal fun newSession() {
        val id = "s${++seq + 100}"
        chatSessions.add(0, ChatSession(id, "新会话", "刚刚"))
        selectSession(id)
    }

    /** 角色视图切换：有研究结果时按视角复述真实辩论内容；否则给出该角色的引导说明。 */
    internal fun selectRoleView(view: String) {
        val research = lastResearch
        val blocks: List<AiBlock> = when {
            research != null && view == "多空对抗" -> listOf(
                AiBlock.Text("「多空对抗」视角 · ${research.name.ifBlank { research.symbol }}：多头与空头研究员基于同一组证据辩论，研究经理裁决为「${research.rating.ifBlank { research.stance }}」。"),
            ) + debateBlocks(research).ifEmpty { listOf(AiBlock.Text("本次研究未产生辩论文本（规则降级模式）。")) }
            research != null && view == "风控" -> listOf(
                AiBlock.Text("「风控」视角 · ${research.name.ifBlank { research.symbol }}：激进 / 中性 / 保守三方风控逐条回应研究结论。"),
            ) + riskBlocks(research)
            research != null -> listOf(
                // Task2 评分点「组织呈现：标题 + 答案来源」：用 Markdown 组织研究归纳
                AiBlock.Text(
                    buildString {
                        appendLine("### ${research.name.ifBlank { research.symbol }} · 研究归纳")
                        appendLine(
                            "> 来源：新浪行情 / 东方财富资讯 · 归纳：${research.synthesisProvider.ifBlank { "规则降级" }} · ${research.reportDate}"
                        )
                        appendLine()
                        appendLine(research.summary.ifBlank { "综合行情、技术面、财务与资讯证据。" })
                        appendLine()
                        appendLine("**核心结论速览**")
                        appendLine()
                        appendLine("| 项目 | 值 |")
                        appendLine("| --- | --- |")
                        appendLine(
                            "| 现价 | **${fmt2(research.price)}（${if (research.changePercent >= 0) "+" else ""}${fmt2(research.changePercent)}%）** |",
                        )
                        appendLine("| 短期趋势 | ${research.trend.ifBlank { research.stance }} |")
                        appendLine("| 研究经理评级 | ${research.rating.ifBlank { research.stance }} |")
                        appendLine(
                            "| 压力 / 支撑 | ${research.pressure?.let { fmt2(it) } ?: "—"} / ${research.support?.let { fmt2(it) } ?: "—"} |",
                        )
                        appendLine("| RSI(14) / MA20 | ${fmt2(research.rsi14)} / ${fmt2(research.ma20)} |")
                        if (research.bullPoints.isNotEmpty()) {
                            appendLine()
                            appendLine("**多头要点**")
                            research.bullPoints.take(3).forEach { appendLine("- $it") }
                        }
                        if (research.bearPoints.isNotEmpty()) {
                            appendLine()
                            appendLine("**空头要点**")
                            research.bearPoints.take(3).forEach { appendLine("- $it") }
                        }
                        if (research.riskNotes.isNotEmpty()) {
                            appendLine()
                            appendLine("**风控观察**")
                            research.riskNotes.take(3).forEach { appendLine("- $it") }
                        }
                    },
                ),
            )
            view == "多空对抗" -> listOf(AiBlock.Text("已切换到「多空对抗」：输入个股后，多/空两方研究员将就同一标的辩论，研究经理给出五档评级。"))
            view == "风控" -> listOf(AiBlock.Text("已切换到「风控」：输入个股后，激进 / 中性 / 保守三方风控将评估研究结论并给出价格化观察方案。"))
            else -> listOf(AiBlock.Text("已切换到「分析」：综合基本面、技术面与资讯证据给出个股分析。"))
        }
        messages.add(AiChatMessage("ai", blocks = blocks))
    }

    override fun body(): ViewBuilder = {
        attr {
            flex(1f); flexDirectionColumn()
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
            if (!this@AiResearchPage.isCompact()) sessionColumn(this@AiResearchPage)
            chatColumn(this@AiResearchPage)
        }
        renderBottomTab(this@AiResearchPage, "AI研究")
    }
}

// ============== 左：会话 ==============
private fun ViewContainer<*, *>.sessionColumn(host: AiResearchPage) {
    val colors = AppTheme.colors
    View {
        attr {
            width(host.sessionWidth())
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
            SecondaryButton("新建", height = 30f, icon = IconKind.CHAT) { host.newSession() }
        }
        List {
            attr { flex(1f) }
            vfor({ host.chatSessions }) { session ->
                View {
                    attr {
                        height(48f); flexDirectionRow(); alignItemsCenter()
                        backgroundColor(colors.c(if (session.id == host.currentSessionId) colors.surfaceHover else colors.surface))
                        cssClass("zn-row zn-click")
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                    event { click { host.selectSession(session.id) } }
                    // 选中态：AI 强调色指示条（小面积点缀）
                    View {
                        attr {
                            width(3f); alignSelfStretch()
                            backgroundColor(colors.ca(colors.aiAccent, if (session.id == host.currentSessionId) 100 else 0))
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                    }
                    View { attr { width(13f) } }
                    Icon(IconKind.CHAT, 14f)
                    View { attr { width(8f) } }
                    Text {
                        attr {
                            fontSize(AppTypography.fs13)
                            if (session.id == host.currentSessionId) fontWeightSemiBold()
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
        // 角色视图切换（TradingAgents 启发：Analyst / 辩论 / 风控）
        View {
            attr {
                height(40f); flexDirectionRow(); alignItemsCenter()
                padding(left = host.chatSidePad())
                borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            listOf("分析", "多空对抗", "风控").forEach { view ->
                RoleTab(label = view, active = { host.roleView == view }) {
                    host.roleView = view
                    host.selectRoleView(view)
                }
            }
            View { attr { flex(1f) } }
            if (host.isCompact()) {
                SecondaryButton("新建", height = 30f, icon = IconKind.CHAT) { host.newSession() }
                View { attr { width(host.chatSidePad()) } }
            } else {
                Text {
                    attr {
                        marginRight(host.chatSidePad())
                        fontSize(AppTypography.fs11)
                        color(colors.c(colors.textTertiary))
                        text("多 Agent 辩论研究 · 来源可追溯")
                    }
                }
            }
        }
        List {
            ref { host.chatListRef = it }
            attr { flex(1f) }
            event {
                contentSizeChanged { _, height ->
                    host.chatListRef?.view?.setContentOffset(0f, height, false)
                }
            }
            vfor({ host.messages }) { msg ->
                View {
                    attr { padding(top = 14f, left = host.chatSidePad(), right = host.chatSidePad()) }
                    if (msg.role == "user") {
                        // 用户气泡：前景/背景反转（浅色主题深底白字，深色反之），与 AI 卡形成强对比
                        View {
                            attr {
                                alignSelfFlexEnd()
                                maxWidth(560f)
                                borderRadius(AppRadius.radius6)
                                backgroundColor(colors.c(colors.textPrimary))
                                animate(ANIM_THEME, value = AppTheme.isDark)
                            }
                            View {
                                attr { padding(top = 8f, left = 14f, right = 14f, bottom = 8f) }
                                Text {
                                    attr {
                                        fontSize(AppTypography.fs14); lineHeight(21f)
                                        color(colors.c(colors.pageBg))
                                        text(msg.text)
                                    }
                                }
                            }
                        }
                    } else {
                        View { attr { alignSelfStretch(); maxWidth(960f) }
                            AiMessageHeader("知牛 AI")
                            View { attr { height(8f) } }
                            vif({ msg.streaming && msg.blocks.isEmpty() }) {
                                AgentProgressRow(msg.progress)
                            }
                            // 消息卡：内容收敛进 surface 容器，与页面底形成层次
                            View {
                                attr {
                                    alignSelfStretch()
                                    borderRadius(AppRadius.radius8)
                                    border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                                    backgroundColor(colors.c(colors.surface))
                                    padding(top = 12f, bottom = 12f, left = 14f, right = 14f)
                                    animate(ANIM_THEME, value = AppTheme.isDark)
                                }
                                msg.blocks.forEachIndexed { i, block ->
                                    if (i > 0) View { attr { marginTop(10f) } }
                                    AiBlockView(
                                        block,
                                        onOpenStock = { sym -> host.openStock(sym) },
                                        onAsk = { q -> host.send(q) },
                                    )
                                }
                            }
                            View { attr { height(14f) } }
                        }
                    }
                }
            }
        }
        // Composer
        var composerRef: ViewRef<InputView>? = null
        View {
            attr {
                backgroundColor(colors.c(colors.surface))
                borderTop(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                padding(top = 12f, bottom = 12f + host.bottomNavInset(), left = host.chatSidePad(), right = host.chatSidePad())
                flexDirectionRow(); alignItemsCenter()
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            AppInput(
                placeholder = "继续提问，如：分析贵州茅台 / 解释 RSI",
                text = host.draft, height = 38f,
                onTextChange = { host.draft = it },
                onReturn = {
                    val question = host.draft
                    if (question.isNotBlank()) {
                        host.send(question)
                        composerRef?.view?.setText("")
                    }
                },
                onRef = { composerRef = it },
            )
            View { attr { width(8f) } }
            PrimaryButton("发送", height = 36f, icon = IconKind.SEND) {
                    val question = host.draft
                    if (question.isNotBlank()) {
                        host.send(question)
                        composerRef?.view?.setText("")
                    }
            }
        }
    }
}

private fun ViewContainer<*, *>.AgentProgressRow(steps: List<String>) {
    val colors = AppTheme.colors
    View {
        attr {
            flexDirectionRow(); alignItemsCenter(); padding(top = 8f, bottom = 8f, left = 10f, right = 10f)
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            borderRadius(AppRadius.radius6)
            backgroundColor(colors.c(colors.surface))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        Icon(IconKind.DATA, 15f)
        View { attr { width(8f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12); color(colors.c(colors.textSecondary))
                text((steps.ifEmpty { listOf("准备研究") }).joinToString("  →  "))
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                fontSize(AppTypography.fs11); color(colors.c(colors.textTertiary)); text("运行中")
            }
        }
    }
}

/** 角色视图 Tab：文字 + 2px 底部指示器（TradingAgents 角色分工：Analyst/辩论/风控）；active 在 attr 内读取保持响应式。 */
private fun ViewContainer<*, *>.RoleTab(label: String, active: () -> Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            height(40f); padding(left = 4f, right = 4f); marginRight(20f)
            flexDirectionColumn(); alignItemsCenter(); justifyContentCenter()
            cssClass("zn-click")
        }
        event { click { onClick() } }
        Text {
            attr {
                width(if (label == "多空对抗") 64f else 36f); textAlignCenter()
                fontSize(AppTypography.fs13)
                fontWeight600(); lines(1); textOverFlowClip()
                color(colors.c(if (active()) colors.textPrimary else colors.textSecondary))
                text(label)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(4f) } }
        View {
            attr {
                height(2f); width(16f)
                borderRadius(allBorderRadius = 1f)
                backgroundColor(colors.c(colors.aiAccent))
                opacity(if (active()) 1f else 0f)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

private fun AiResearchPage.sessionWidth(): Float = when {
    isCompact() -> 0f
    isMedium() -> 208f
    else -> 240f
}

private fun AiResearchPage.chatSidePad(): Float {
    val chatWidth = (pageData.activityWidth - sessionWidth()).coerceAtLeast(0f)
    return ((chatWidth - 960f) / 2f).coerceAtLeast(if (isCompact()) 16f else 32f)
}
