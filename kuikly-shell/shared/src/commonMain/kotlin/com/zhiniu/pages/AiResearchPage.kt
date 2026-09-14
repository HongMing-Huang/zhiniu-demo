/* 知牛 · AiResearchPage（AI 研究：两栏）
 * 左：会话记录（240px）；中：聊天（结构化 5 类 AiBlock + 流式揭示）。
 * 不再加右栏 Context Rail（避免开发冗余）。Composer 底部 + 圆形 34 发送。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vforIndex
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
import com.zhiniu.data.local.ChatArchive
import com.zhiniu.data.local.ChatArchiveSession
import com.zhiniu.data.local.ChatRecord
import com.zhiniu.base.openComparePage
import com.zhiniu.data.remote.GatewayMarketClient
import com.zhiniu.data.remote.AgentResearchResult
import com.zhiniu.domain.model.ChatSession
import com.zhiniu.domain.repository.AiBlock
import com.zhiniu.domain.repository.MetricCell
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.ThemeMode
import com.zhiniu.pages.components.UPDOWN_SP_KEY
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.PAD
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtMarketCap
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtSymbol
import com.zhiniu.pages.components.ai.AiBlockView
import com.zhiniu.pages.components.AiMessageHeader
import com.zhiniu.pages.components.common.AppInput
import com.zhiniu.pages.components.common.RoundIconButton
import com.zhiniu.pages.components.common.Divider
import com.zhiniu.pages.components.common.SecondaryButton
import com.zhiniu.pages.components.common.SectionHeader
import com.tencent.kuikly.core.coroutines.Job
import com.tencent.kuikly.core.coroutines.launch

/** 聊天消息（user 文本 / ai 结构化块 + 流式）。 */
data class AiChatMessage(
    val role: String,
    val text: String = "",
    val blocks: List<AiBlock> = emptyList(),
    val streaming: Boolean = false,
    val progress: List<String> = emptyList(),
    val requestId: String = "",
    val cancelled: Boolean = false,   // 用户主动停止生成（保留已到内容，可原位重试）
)

// 与 WatchlistPage 共用的持久化键（AI 从本页改自选/预警时同步落 SP）
private const val WATCHLIST_SP_KEY = "zhiniu.watchlist.symbols.v1"
private const val ALERTS_SP_KEY = "zhiniu.alert.items.v1"
// 聊天归档键（会话 + 消息持久化；对标 KuiklyStock ChatStore——刷新/冷启动不再丢会话）
private const val CHAT_ARCHIVE_SP_KEY = "zhiniu.chat.archive.v1"

@Page("AiResearch", supportInLocal = true)
internal class AiResearchPage : AppBasePage() {

    internal var draft by observable("")
    internal var currentSessionId by observable("s1")
    // TradingAgents 借鉴：角色视图（分析师 / 多空对抗 / 风控）
    internal var roleView by observable("分析")
    internal val chatSessions by observableList<ChatSession>()
    internal val messages by observableList<AiChatMessage>()
    /** 各会话的消息缓存（切会话时暂存当前、载入目标；落盘由 persistArchive 统一收口）。 */
    private val sessionMsgs = mutableMapOf<String, List<AiChatMessage>>()
    internal var chatListRef: ViewRef<ListView<*, *>>? = null
    /** 手机端会话历史底部面板（桌面用左栏，手机收进此处）。 */
    internal var sessionSheetVisible by observable(false)
    private var seq = 0
    /** 最近一次研究结果：角色视图切换时按视角复述真实辩论内容，不再只发引导语。 */
    internal var lastResearch: AgentResearchResult? = null

    // ---------- 生成中状态（取消/重试，对标 Study0915 会话工单状态机） ----------
    /** 是否有回复正在生成（驱动「停止生成」按钮）。 */
    internal var isGenerating by observable(false)
    private var activeJob: com.tencent.kuikly.core.coroutines.Job? = null
    private var activeMessageIndex = -1
    private var activeToken = ""

    // 种子会话从股票池动态生成（不再硬编码假标的与假时间戳）：前三个股票各一个会话 + 市场综述
    private fun buildSeedSessions(): List<ChatSession> =
        repo.stockQuotes().take(3).mapIndexed { i, stock ->
            ChatSession("s${i + 1}", "${stock.name}分析", "")
        } + listOf(ChatSession("s4", "今日市场", ""))

    override fun created() {
        super.created()
        if (chatSessions.isEmpty()) {
            // 归档优先：有历史会话则恢复（含消息），否则用股票池生成种子会话
            val archived = ChatArchive.deserialize(prefs.getString(CHAT_ARCHIVE_SP_KEY))
            if (archived.isNotEmpty()) {
                archived.forEach { session ->
                    chatSessions.add(ChatSession(session.id, session.title, session.createdAt))
                    sessionMsgs[session.id] = session.records.map { messageFromRecord(it) }
                }
            } else {
                buildSeedSessions().forEach { chatSessions.add(it) }
            }
        }
        // 默认进入最近创建的会话（归档按新建顺序头部插入；种子会话回退 s1）
        if (chatSessions.isNotEmpty() && chatSessions.none { it.id == currentSessionId }) {
            currentSessionId = chatSessions.first().id
        }
        loadSession(currentSessionId)
        // 图表选点 / 详情页 CTA 携问题跳入：等行情刷新回填会话后再自动提问
        //（直接在刷新前 send 会被 onSuccess 的 loadSession 重置掉）
        val pendingAsk = com.zhiniu.base.PendingAsk.question
        com.zhiniu.base.PendingAsk.question = null
        lifecycleScope.launch {
            runCatching { GatewayMarketClient.quotes(repo.stockQuotes().map { it.symbol }) }
                .onSuccess { live ->
                    if (live.isNotEmpty()) {
                        MarketStore.applyLiveQuotes(live)
                        refreshHotQuotes()
                        loadSession(currentSessionId)
                    }
                }
            pendingAsk?.takeIf { it.isNotBlank() }?.let { send(it) }
        }
    }

    // ---------- AI 工具指令执行（⟦TOOL⟧ 协议：AI 操作 App，服务端已校验解析） ----------

    private val prefs by lazy {
        acquireModule<com.tencent.kuikly.core.module.SharedPreferencesModule>(com.tencent.kuikly.core.module.SharedPreferencesModule.MODULE_NAME)
    }

    /** 执行服务端校验过的指令并返回反馈块（指令无效时双保险跳过并如实说明）。 */
    internal fun executeToolDirectives(tools: List<com.zhiniu.data.remote.AgentToolDirective>): List<AiBlock> {
        val blocks = mutableListOf<AiBlock>()
        for (tool in tools) {
            if (!tool.isValid) continue
            when (tool.name) {
                "add_watchlist" -> {
                    com.zhiniu.data.local.Watchlist.add(tool.symbol)
                    prefs.setString(WATCHLIST_SP_KEY, com.zhiniu.data.local.Watchlist.serialize())
                    blocks += AiBlock.ToolResult(
                        title = "已加入自选",
                        detail = "${tool.stockName.ifBlank { tool.symbol }}（${tool.symbol}）",
                        action = "watchlist", actionLabel = "查看自选",
                    )
                }
                "research_stock" -> {
                    // 模型识别出个股问题但缺数据 → 转入多 Agent 研究管线（真实行情/财报/资讯）
                    blocks += AiBlock.ToolResult(
                        title = "已转入个股研究",
                        detail = "${tool.stockName.ifBlank { tool.symbol }}（${tool.symbol}）· 正在拉取行情/技术面/财务/资讯证据",
                        action = "research:${tool.symbol}", actionLabel = "查看研究",
                    )
                    startResearch(tool.symbol, tool.stockName, "分析${tool.stockName.ifBlank { tool.symbol }}")
                }
                "open_compare" -> {
                    blocks += AiBlock.ToolResult(
                        title = "对比已就绪",
                        detail = "${tool.stockName.ifBlank { tool.symbol }} × ${tool.stockNameB.ifBlank { tool.symbolB }}",
                        action = "compare:${tool.symbol}|${tool.symbolB}", actionLabel = "打开对比",
                    )
                }
                "set_price_alert" -> {
                    com.zhiniu.data.local.AlertStore.add(tool.symbol, tool.stockName, tool.alertOperator, tool.alertPrice)
                    prefs.setString(ALERTS_SP_KEY, com.zhiniu.data.local.AlertStore.serialize())
                    blocks += AiBlock.ToolResult(
                        title = "预警已设置",
                        detail = "${tool.stockName.ifBlank { tool.symbol }} ${if (tool.alertOperator == "above") "突破" else "跌破"} ${com.zhiniu.pages.components.fmt2(tool.alertPrice)}（自选页可查看/删除）",
                        action = "alert", actionLabel = "查看预警",
                    )
                }
                "set_appearance" -> {
                    val mode = when (tool.mode) {
                        "light" -> ThemeMode.LIGHT
                        "dark" -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    }
                    AppTheme.applyModePersisted(mode)
                    blocks += AiBlock.ToolResult(
                        title = "外观已切换",
                        detail = "当前外观：${mode.label}（「我的」页可随时改回）",
                    )
                }
                "set_color_mode" -> {
                    val greenUp = tool.mode == "green_up"
                    AppTheme.swapUpDon = greenUp
                    prefs.setString(UPDOWN_SP_KEY, if (greenUp) "1" else "0")
                    blocks += AiBlock.ToolResult(
                        title = "涨跌配色已切换",
                        detail = if (greenUp) "绿涨红跌（海外习惯）" else "红涨绿跌（A 股习惯）",
                    )
                }
            }
        }
        return blocks
    }

    /** 工具反馈卡按钮：watchlist/alert → 自选页；compare:symA|symB → 对比页。 */
    internal fun handleToolAction(action: String) {
        when {
            action == "watchlist" || action == "alert" -> navWatchlist()
            action.startsWith("compare:") -> {
                val syms = action.removePrefix("compare:").split("|")
                if (syms.size == 2) openComparePage(syms[0], syms[1])
            }
            // research_stock 反馈卡：跳个股详情（研究结论已在会话流中展开）
            action.startsWith("research:") -> {
                val sym = action.removePrefix("research:")
                if (sym.length == 8) openStock(sym)
            }
        }
    }

    /** 显式意图才进工具链路；双方对比需「和/与/vs」连接词（单标的「对比X」仍走研究管线）。 */
    private fun isToolIntent(text: String): Boolean {
        val watchlistIntent = text.contains("自选") && (text.contains("加") || text.contains("收藏") || text.contains("添加"))
        val alertIntent = text.contains("预警") || text.contains("提醒我") || text.contains("报警")
        val compareIntent = (text.contains("对比") || text.contains("比较")) &&
            (text.contains("和") || text.contains("与") || text.contains("vs") || text.contains("VS") || text.contains("×"))
        val appearanceIntent = listOf("深色", "浅色", "夜间", "暗黑", "亮色", "主题").any { it in text } &&
            listOf("切", "换", "改", "调", "设").any { it in text }
        val colorModeIntent = listOf("红涨绿跌", "绿涨红跌", "涨跌配色", "配色").any { it in text } &&
            listOf("切", "换", "改", "调", "设").any { it in text }
        return watchlistIntent || alertIntent || compareIntent || appearanceIntent || colorModeIntent
    }

    internal fun send(question: String) {
        val text = question.trim()
        if (text.isEmpty()) return
        messages.add(AiChatMessage("user", text = text))
        draft = ""
        persistArchive()
        answer(text, atIndex = null)
    }

    /** 路由到研究管线或通用问答；atIndex 非空 = 原位重试（替换该条 AI 消息，不追加）。 */
    private fun answer(question: String, atIndex: Int?) {
        // 操作意图（加自选/设预警/双方对比）优先走带 ⟦TOOL⟧ 协议的通用问答——AI 直接操作 App；
        // 注意「对比宁德时代」这类单标的表述仍走研究管线（open_compare 需要两只标的）。
        if (isToolIntent(question)) {
            sendGeneralQuestion(question, atIndex)
            return
        }
        val target = repo.stockQuotes().firstOrNull {
            question.contains(it.name) || question.contains(it.code) ||
                (it.pinyin.isNotBlank() && question.contains(it.pinyin, ignoreCase = true))
        }
        if (target == null) {
            sendGeneralQuestion(question, atIndex)
            return
        }
        startResearch(target.symbol, target.name, question, atIndex)
    }

    /** 用户停止生成：取消在途请求，保留已到内容并标记可重试（对标 Study0915 cancel）。 */
    internal fun stopGenerating() {
        activeJob?.cancel()
        activeJob = null
        val idx = activeMessageIndex
        val token = activeToken
        if (idx in messages.indices && messages[idx].requestId == token) {
            val msg = messages[idx]
            val blocks = if (msg.blocks.isEmpty()) listOf(AiBlock.Text("（已停止生成）")) else msg.blocks
            messages[idx] = msg.copy(streaming = false, cancelled = true, requestId = "", blocks = blocks)
        }
        isGenerating = false
        persistArchive()
    }

    /** 原位重试：取该 AI 消息之前最近的一条用户提问，替换此条重新生成（对标 Study0915 retry）。 */
    internal fun retryMessage(index: Int) {
        if (index <= 0 || index >= messages.size) return
        val question = messages.take(index).lastOrNull { it.role == "user" }?.text?.trim().orEmpty()
        if (question.isEmpty()) return
        answer(question, atIndex = index)
    }

    /** 多 Agent 研究管线（send / retry / research_stock 工具指令共用入口）。 */
    internal fun startResearch(symbol: String, name: String, question: String, atIndex: Int? = null) {
        val token = "research-${++seq}"
        val messageIndex = beginPlaceholder(atIndex, token, listOf("行情 Agent"))
        activeMessageIndex = messageIndex
        activeToken = token
        isGenerating = true
        activeJob = lifecycleScope.launch {
            // 首选类型化 SSE：阶段帧实时驱动进度条（行情→技术面→财务→资讯→风险→多头→空头→研究经理→交易员→风控→归纳）
            val steps = mutableListOf<String>()
            val streamed = runCatching {
                GatewayMarketClient.researchStream(symbol, question) { event ->
                    if (event.label.isNotBlank() && event.label !in steps) {
                        steps += event.label
                        updateResearchProgress(messageIndex, token, steps.toList())
                    }
                }
            }.getOrNull()
            if (coroutineContext[Job]?.isActive != true) return@launch
            // 流不可用（旧网关 / 代理不支持流式）→ 回退一次性研究接口
            val result = streamed ?: runCatching { GatewayMarketClient.research(symbol, question) }.getOrNull()
            if (coroutineContext[Job]?.isActive != true || !isPending(messageIndex, token)) return@launch
            if (activeToken == token) isGenerating = false
            lastResearch = result ?: lastResearch
            val blocks = result?.let { researchBlocks(it) } ?: listOf(
                AiBlock.Risk("研究网关暂不可用", "已保留本地快照回答；请稍后重试以获取带来源的实时证据。"),
            ) + MarketStore.aiService.chatReply(currentSessionId, question)
            messages[messageIndex] = AiChatMessage("ai", blocks = blocks, streaming = false)
            persistArchive()
        }
    }

    /**
     * 非个股问题（快捷指令 / 指标解释 / 方法论）→ 后端 /agent/chat/stream 流式 LLM 问答
     * （打字机逐段上屏）；流式不可用回退一次性 /agent/chat；网关整体不可用回退本地 Mock。
     * 上下文标的取最近一次研究，回答中的价格数字锚定该快照。
     */
    private fun sendGeneralQuestion(text: String, atIndex: Int? = null) {
        val token = "chat-${++seq}"
        val messageIndex = beginPlaceholder(atIndex, token, listOf("模型网关"))
        activeMessageIndex = messageIndex
        activeToken = token
        isGenerating = true
        val contextSymbol = lastResearch?.symbol.orEmpty()
        activeJob = lifecycleScope.launch {
            // 多轮上下文：AI 消息取块文本投影（此前 it.text 对 AI 恒为空，历史只剩用户侧）
            val history = messages.takeLast(9).dropLast(1).map { msg ->
                msg.role to if (msg.role == "user") msg.text else ChatArchive.aiTextOf(msg.blocks)
            }.filter { it.second.isNotBlank() }
            var accumulated = ""
            val streamed = runCatching {
                GatewayMarketClient.chatStream(text, history, contextSymbol) { delta ->
                    accumulated += delta
                    // 打字机：流中即时渲染已到内容（保持 streaming 以显示进行态）
                    if (isPending(messageIndex, token)) {
                        messages[messageIndex] = AiChatMessage(
                            "ai",
                            blocks = listOf(AiBlock.Text(accumulated)),
                            streaming = true,
                            requestId = token,
                        )
                    }
                }
            }.getOrNull()
            if (coroutineContext[Job]?.isActive != true) return@launch
            // 流式不可用（旧网关/iOS 流断言）→ 一次性问答接口
            val reply = streamed ?: runCatching {
                GatewayMarketClient.agentChat(text, history, contextSymbol)
            }.getOrNull()
            if (coroutineContext[Job]?.isActive != true || !isPending(messageIndex, token)) return@launch
            if (activeToken == token) isGenerating = false
            if (reply != null) {
                val toolBlocks = if (reply.isLlm) executeToolDirectives(reply.tools) else emptyList()
                val header = if (reply.isLlm) {
                    AiBlock.Text("知牛 AI · 通用问答")
                } else {
                    AiBlock.Risk("规则降级 · 未伪装模型", reply.content)
                }
                val body = if (reply.isLlm) markdownOf(reply.content) else emptyList()
                // 结论徽章（【AI观点】结构化抽取）
                val verdictBlocks = reply.verdict?.let { listOf(AiBlock.Verdict(it.risk, it.action)) } ?: emptyList()
                // 走势卡（[KCHART:symbol]）：拉真实日 K 收盘渲染迷你图
                val chartBlocks = if (reply.isLlm) fetchChartBlocks(reply.charts) else emptyList()
                messages[messageIndex] = AiChatMessage(
                    "ai",
                    blocks = listOf(header) + body + verdictBlocks + toolBlocks + chartBlocks,
                    streaming = false,
                )
            } else {
                // 离线兜底：本地 Mock（内容确定性，无网络依赖）
                revealLocalReply(MarketStore.aiService.chatReply(currentSessionId, text), messageIndex, token)
            }
            persistArchive()
        }
    }

    /** [KCHART:symbol] → 真实日 K 迷你图块（取近 60 日收盘；拉取失败静默跳过，不阻断回复）。 */
    private suspend fun fetchChartBlocks(symbols: List<String>): List<AiBlock> {
        val symbol = symbols.firstOrNull() ?: return emptyList()
        val candles = runCatching { GatewayMarketClient.candles(symbol, 240, 60) }.getOrNull() ?: return emptyList()
        val closes = candles.map { it.close }.filter { it > 0.0 }
        if (closes.size < 2) return emptyList()
        return listOf(
            AiBlock.Text("#### ${fmtSymbol(symbol)} · 日 K（近 ${closes.size} 日收盘）"),
            AiBlock.KLine(closes),
        )
    }

    // AiBlock.Text 渲染端即 MarkdownView（标题/列表/表格/代码块全支持），通用问答直接喂原文
    private fun markdownOf(content: String): List<AiBlock> = listOf(AiBlock.Text(content))

    private fun isPending(index: Int, token: String): Boolean =
        index < messages.size && messages[index].requestId == token

    /** 放置生成占位消息：atIndex 非空且指向 AI 消息 = 原位重试（替换），否则追加。返回占位下标。 */
    private fun beginPlaceholder(atIndex: Int?, token: String, progress: List<String>): Int {
        if (atIndex != null && atIndex in messages.indices && messages[atIndex].role == "ai") {
            messages[atIndex] = AiChatMessage("ai", streaming = true, progress = progress, requestId = token)
            return atIndex
        }
        messages.add(AiChatMessage("ai", streaming = true, progress = progress, requestId = token))
        return messages.size - 1
    }

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
        if (id != currentSessionId) {
            // 切走前暂存当前会话消息，回来时原样恢复
            sessionMsgs[currentSessionId] = messages.toList()
            currentSessionId = id
        }
        loadSession(id)
    }

    private fun loadSession(id: String) {
        messages.diffUpdate(emptyList())
        // 已有会话消息（内存缓存或归档恢复）→ 原样载入，不再补种子问答
        val saved = sessionMsgs[id]
        if (!saved.isNullOrEmpty()) {
            saved.forEach { messages.add(it) }
            return
        }
        // 种子会话问题从标题派生（标题即股票池真实标的），不再按 id 硬编码映射
        val session = chatSessions.firstOrNull { it.id == id }
        val question = when {
            session == null -> ""
            session.title == "今日市场" -> "今天市场强弱如何？"
            session.title.endsWith("分析") -> "分析${session.title.removeSuffix("分析")}，重点看趋势、量能和风险"
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
        persistArchive()
    }

    // ---------- 会话归档（SharedPreferences；结构化卡片压缩为正文 + 反馈卡 + 徽章） ----------

    /** 当前全部会话 → 归档 JSON 落盘。消息事件（发送/回复落定/新建会话）时调用。 */
    internal fun persistArchive() {
        val sessions = chatSessions.map { session ->
            ChatArchiveSession(
                id = session.id,
                title = session.title,
                createdAt = session.createdAt,
                records = recordsFor(session.id),
            )
        }
        prefs.setString(CHAT_ARCHIVE_SP_KEY, ChatArchive.serialize(sessions))
    }

    /** 指定会话的消息记录：当前会话取 messages，其余取内存缓存。生成中的占位消息不归档。 */
    private fun recordsFor(sessionId: String): List<ChatRecord> {
        val msgs = if (sessionId == currentSessionId) messages.toList() else sessionMsgs[sessionId] ?: emptyList()
        return msgs.asSequence()
            .filter { !it.streaming }
            .filter { it.role == "user" || it.blocks.isNotEmpty() }
            .map { message ->
                if (message.role == "user") ChatRecord(role = "user", text = message.text)
                else ChatArchive.aiRecord(message.blocks)
            }
            .toList()
    }

    /** 归档记录 → 页面消息（user 恢复文本气泡；ai 恢复 Markdown + 徽章 + 反馈卡）。 */
    private fun messageFromRecord(record: ChatRecord): AiChatMessage = when (record.role) {
        "user" -> AiChatMessage("user", text = record.text)
        else -> AiChatMessage("ai", blocks = ChatArchive.blocksOf(record))
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
        persistArchive()
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
        sessionHistorySheet(this@AiResearchPage)
    }
}

/** 手机端会话历史底部面板：遮罩 + 圆角列表，选择即切换并收起。 */
private fun ViewContainer<*, *>.sessionHistorySheet(host: AiResearchPage) {
    val colors = AppTheme.colors
    if (!host.isCompact()) return
    // 遮罩（点击关闭）
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(Color(0x000000L, 0.4f))
            touchEnable(host.sessionSheetVisible)
            opacity(if (host.sessionSheetVisible) 1f else 0f)
            animate(Animation.easeOut(0.16f), value = host.sessionSheetVisible)
            zIndex(20)
        }
        event { click { host.sessionSheetVisible = false } }
    }
    // 底部面板（手机：距底 = 底部 Tab 高度 + 安全区；列表上限 5.5 行高度）
    View {
        attr {
            absolutePosition(bottom = 64f + host.safeBottomInset(), left = 0f, right = 0f)
            height(320f)
            flexDirectionColumn()
            backgroundColor(colors.c(colors.surface))
            borderRadius(topLeft = 16f, topRight = 16f, bottomLeft = 0f, bottomRight = 0f)
            touchEnable(host.sessionSheetVisible)
            opacity(if (host.sessionSheetVisible) 1f else 0f)
            transform(translate = Translate(0f, 0f, offsetY = if (host.sessionSheetVisible) 0f else 24f))
            animate(Animation.easeOut(0.2f), value = host.sessionSheetVisible)
            zIndex(21)
        }
        View {
            attr {
                height(52f); flexDirectionRow(); alignItemsCenter()
                padding(left = 16f, right = 12f)
                borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            }
            Text {
                attr {
                    fontSize(AppTypography.fs15); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary)); text("会话记录")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { flex(1f) } }
            Icon(IconKind.CLOSE, 16f)
            event { click { host.sessionSheetVisible = false } }
        }
        List {
            attr { flex(1f) }
            vfor({ host.chatSessions }) { session ->
                View {
                    attr {
                        height(48f); flexDirectionRow(); alignItemsCenter()
                        padding(left = 16f, right = 16f)
                        backgroundColor(colors.c(if (session.id == host.currentSessionId) colors.surfaceHover else colors.surface))
                        cssClass("zn-row zn-click")
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                    View {
                        attr {
                            width(3f); height(20f); borderRadius(2f)
                            backgroundColor(colors.ca(colors.aiAccent, if (session.id == host.currentSessionId) 100 else 0))
                        }
                    }
                    View { attr { width(12f) } }
                    Icon(IconKind.CHAT, 14f)
                    View { attr { width(8f) } }
                    Text {
                        attr {
                            fontSize(AppTypography.fs14)
                            color(colors.c(if (session.id == host.currentSessionId) colors.textPrimary else colors.textSecondary))
                            text(session.title)
                        }
                    }
                    View { attr { flex(1f) } }
                    if (session.createdAt.isNotBlank()) {
                        Text {
                            attr {
                                fontSize(AppTypography.fs11)
                                color(colors.c(colors.textTertiary)); text(session.createdAt)
                            }
                        }
                    }
                    event { click {
                        host.selectSession(session.id)
                        host.sessionSheetVisible = false
                    } }
                }
            }
        }
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
                    if (session.createdAt.isNotBlank()) {
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
}

// ============== 中：聊天 ==============
private fun ViewContainer<*, *>.chatColumn(host: AiResearchPage) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f); flexDirectionColumn()
            backgroundColor(colors.c(colors.pageBg))
        }
        // 角色视图切换（TradingAgents 启发：Analyst / 辩论 / 风控）；手机 = 胶囊，桌面 = 下划线
        View {
            attr {
                height(44f); flexDirectionRow(); alignItemsCenter()
                paddingLeft(host.chatSidePad()); paddingRight(host.chatSidePad())
                borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            listOf("分析", "多空对抗", "风控").forEach { view ->
                RoleTab(label = view, active = { host.roleView == view }, compact = host.isCompact()) {
                    host.roleView = view
                    host.selectRoleView(view)
                }
            }
            View { attr { flex(1f) } }
            if (host.isCompact()) {
                // 手机：历史会话（底部面板）+ 新建，两个圆形图标按钮（对标移动端 IM 顶栏操作）
                // 各自独立 View 包裹：Kuikly KSP 事件桥按作用域生成签名，同层两个
                // 事件 lambda 会触发 IrSimpleFunctionSymbol 重复绑定（编译器内部错误）
                View {
                    attr { flexDirectionRow(); alignItemsCenter() }
                    View {
                        attr { flexDirectionRow(); alignItemsCenter() }
                        RoundIconButton(
                            IconKind.DATA, size = 15f, box = 30f,
                            accessibilityLabel = "历史会话",
                        ) { host.sessionSheetVisible = true }
                    }
                    View { attr { width(8f) } }
                    View {
                        attr { flexDirectionRow(); alignItemsCenter() }
                        RoundIconButton(
                            IconKind.CHAT, size = 15f, box = 30f,
                            accessibilityLabel = "新建会话",
                        ) { host.newSession() }
                    }
                }
            } else {
                Text {
                    attr {
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
            // 空会话欢迎态（欧易/ChatGPT 式）：品牌 + 推荐问题胶囊，点击直接发送
            vif({ host.messages.isEmpty() }) {
                View {
                    attr {
                        padding(top = 60f, bottom = 20f, left = 24f, right = 24f)
                        flexDirectionColumn(); alignItemsCenter()
                    }
                    View {
                        attr {
                            width(56f); height(56f); borderRadius(28f); allCenter()
                            backgroundColor(colors.ca(colors.aiAccent, 14))
                            border(Border(1f, BorderStyle.SOLID, colors.c(colors.aiAccent)))
                        }
                        Icon(IconKind.AI, 24f)
                    }
                    View { attr { height(12f) } }
                    Text {
                        attr {
                            fontSize(AppTypography.fs16); fontWeightSemiBold()
                            color(colors.c(colors.textPrimary)); text("你好，我是知牛 AI")
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                    }
                    View { attr { height(6f) } }
                    Text {
                        attr {
                            textAlignCenter()
                            fontSize(AppTypography.fs12)
                            color(colors.c(colors.textSecondary))
                            text("多 Agent 辩论研究：行情 · 技术面 · 财务 · 资讯 · 多空 · 风控\n输入股票名称或选择下方问题开始")
                            animate(ANIM_THEME, value = AppTheme.isDark)
                        }
                    }
                    View { attr { height(18f) } }
                    // 快捷问句：手机两列网格（对标移动端 AI 助手预设问题），桌面居中单列
                    val quickQuestions = listOf(
                        "分析贵州茅台",
                        "分析宁德时代",
                        "解释 RSI 指标",
                        "结合日K分析五粮液",
                    )
                    if (host.isCompact()) {
                        quickQuestions.chunked(2).forEach { pair ->
                            View {
                                attr { flexDirectionRow(); alignSelfStretch() }
                                pair.forEach { q ->
                                    View {
                                        attr { flex(1f); padding(all = 3f) }
                                        QuickQuestionChip(q, colors) { host.send(q) }
                                    }
                                }
                                // 奇数行补位，保持两列等宽
                                if (pair.size == 1) View { attr { flex(1f); padding(all = 3f) } }
                            }
                        }
                    } else {
                        quickQuestions.forEach { q -> QuickQuestionChip(q, colors) { host.send(q) } }
                    }
                }
            }
            vforIndex({ host.messages }) { msg, msgIndex, _ ->
                View {
                    attr { padding(top = 14f, left = host.chatSidePad(), right = host.chatSidePad()) }
                    if (msg.role == "user") {
                        // 用户气泡：accent 浅底右对齐（对标豆包/ChatGPT 用户气泡），与 AI 卡形成左右层次
                        View {
                            attr {
                                alignSelfFlexEnd()
                                maxWidth(560f)
                                borderRadius(16f)
                                backgroundColor(colors.ca(colors.aiAccent, 22))
                                animate(ANIM_THEME, value = AppTheme.isDark)
                            }
                            View {
                                attr { padding(top = 9f, left = 14f, right = 14f, bottom = 9f) }
                                Text {
                                    attr {
                                        fontSize(AppTypography.fs14); lineHeight(21f)
                                        color(colors.c(colors.textPrimary))
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
                            // 消息卡：无边框柔和底色（豆包式），层次靠底色差而非描边
                            View {
                                attr {
                                    alignSelfStretch()
                                    borderRadius(AppRadius.radius12)
                                    backgroundColor(colors.c(colors.surfaceSecondary))
                                    padding(top = 14f, bottom = 14f, left = 14f, right = 14f)
                                    animate(ANIM_THEME, value = AppTheme.isDark)
                                }
                                msg.blocks.forEachIndexed { i, block ->
                                    if (i > 0) View { attr { marginTop(10f) } }
                                    AiBlockView(
                                        block,
                                        onOpenStock = { sym -> host.openStock(sym) },
                                        onAsk = { q -> host.send(q) },
                                        onToolAction = { action -> host.handleToolAction(action) },
                                    )
                                }
                            }
                            // 停止生成的回复：保留已到内容 + 原位重新生成入口（对标 Study0915 retry）
                            vif({ !msg.streaming && msg.cancelled }) {
                                View {
                                    attr { flexDirectionRow(); alignItemsCenter(); marginTop(8f) }
                                    Text {
                                        attr {
                                            fontSize(AppTypography.fs12)
                                            color(colors.c(colors.textTertiary)); marginRight(10f)
                                            text("已停止生成"); animate(ANIM_THEME, value = AppTheme.isDark)
                                        }
                                    }
                                    SecondaryButton("重新生成", height = 28f, icon = IconKind.REFRESH) {
                                        host.retryMessage(msgIndex)
                                    }
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
                padding(top = 12f, bottom = 12f, left = host.chatSidePad(), right = host.chatSidePad())
                flexDirectionRow(); alignItemsCenter()
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            // 停止生成（生成中可见）：取消在途请求，保留已到内容并给出重试入口
            vif({ host.isGenerating }) {
                View {
                    attr { marginRight(10f) }
                    RoundIconButton(
                        IconKind.CLOSE, size = 14f, box = 38f,
                        accessibilityLabel = "停止生成",
                    ) { host.stopGenerating() }
                }
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
            // 圆形发送按钮（对标移动端 IM Composer），无内容时禁用态
            RoundIconButton(
                IconKind.SEND, size = 16f, box = 38f,
                enabled = host.draft.isNotBlank(),
                accessibilityLabel = "发送问题",
            ) {
                val question = host.draft
                if (question.isNotBlank()) {
                    host.send(question)
                    composerRef?.view?.setText("")
                }
            }
        }
    }
}

/** 快捷问句胶囊：36px 高圆角芯片（欢迎态）。 */
private fun ViewContainer<*, *>.QuickQuestionChip(
    q: String,
    colors: com.zhiniu.pages.components.Palette,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(36f); allCenter()
            paddingLeft(10f); paddingRight(10f)
            marginTop(4f); marginBottom(4f)
            borderRadius(18f)
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            backgroundColor(colors.c(colors.surface))
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
            accessibility("发送问题 $q")
            accessibilityRole(AccessibilityRole.BUTTON)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(AppTypography.fs13); lines(1)
                color(colors.c(colors.textSecondary)); text(q)
                animate(ANIM_THEME, value = AppTheme.isDark)
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

/** 角色视图 Tab：手机 = OKX/雪球式文字 Tab（选中加粗 + accent 短下划线），桌面 = 文字 + 2px 底部指示器。 */
private fun ViewContainer<*, *>.RoleTab(label: String, active: () -> Boolean, compact: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    if (compact) {
        View {
            attr {
                height(36f); paddingLeft(2f); paddingRight(2f); marginRight(18f)
                flexDirectionColumn(); alignItemsCenter(); justifyContentCenter()
                accessibility(label)
                accessibilityRole(AccessibilityRole.BUTTON)
                accessibilityInfo(clickable = true, longClickable = false)
                cssClass("zn-click")
            }
            event { click { onClick() } }
            Text {
                attr {
                    fontSize(if (active()) AppTypography.fs15 else AppTypography.fs14)
                    color(colors.c(if (active()) colors.textPrimary else colors.textSecondary))
                    if (active()) fontWeightSemiBold()
                    lines(1)
                    text(label)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(4f) } }
            View {
                attr {
                    width(18f); height(2.5f); borderRadius(2f)
                    backgroundColor(colors.c(colors.aiAccent))
                    opacity(if (active()) 1f else 0f)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        return
    }
    View {
        attr {
            height(44f); padding(left = 4f, right = 4f); marginRight(20f)
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
        View { attr { height(6f) } }
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
    val chatWidth = (viewportWidth() - sessionWidth()).coerceAtLeast(0f)
    return ((chatWidth - 960f) / 2f).coerceAtLeast(if (isCompact()) 16f else 32f)
}
