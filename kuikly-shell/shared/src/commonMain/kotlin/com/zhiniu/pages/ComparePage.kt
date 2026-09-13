/* 知牛 · ComparePage（双股对比 · 课题「AI 输出形态丰富」交互项）
 * 并排对比两只股票的关键指标：最新价/涨跌幅（色块）/总市值/成交额/换手率/振幅。
 * 数据走网关 /quote/realtime（真实行情），失败回落本地快照；左右卡点击进各自详情。
 * 快捷入口：AI 研究页快捷指令「对比宁德时代」→ 路由携带 symbolA/symbolB。
 */
package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.coroutines.launch
import com.zhiniu.data.remote.AgentCompareResult
import com.zhiniu.data.remote.GatewayMarketClient
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtSymbol
import com.zhiniu.pages.components.fmtVolHand
import com.zhiniu.pages.components.fmtAmplitude
import com.zhiniu.pages.components.fmtMarketCap
import com.zhiniu.pages.components.fmtOptional
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtVolHand
import com.zhiniu.pages.components.common.SectionHeader

private val DEFAULT_PAIR = "sh600519" to "sz300750"

@Page("Compare", supportInLocal = true)
internal class ComparePage : AppBasePage() {

    internal val sideA by observableList<StockQuote>()
    internal val sideB by observableList<StockQuote>()
    internal var loading by observable(true)
    internal var sourceLabel by observable("同步中…")

    /** AI 对比（/agent/compare/stream）：LLM 定性归纳 + 规则基准兜底（来源如实标注）。 */
    internal var aiResult by observable<AgentCompareResult?>(null)
    internal var aiLoading by observable(false)
    internal var aiStage by observable("")

    override fun created() {
        super.created()
        // 路由参数：symbolA/symbolB 可指定对比对（缺省 茅台 vs 宁德）
        pageData.params.optString("symbolA", "").takeIf { it.isNotBlank() }?.let { pairA = it }
        pageData.params.optString("symbolB", "").takeIf { it.isNotBlank() }?.let { pairB = it }
        refresh()
    }

    private var pairA: String = DEFAULT_PAIR.first
    private var pairB: String = DEFAULT_PAIR.second

    internal fun refresh() {
        loading = true
        sourceLabel = "同步中…"
        lifecycleScope.launch {
                val live = runCatching { GatewayMarketClient.quotes(listOf(pairA, pairB)) }.getOrNull()
            val byCode = live?.associateBy { it.symbol } ?: emptyMap()
            fun rowOf(symbol: String): StockQuote =
                byCode[symbol] ?: repo.quoteOf(symbol)?.copy(symbol = symbol)
                ?: StockQuote(symbol, symbol, open = 0.0, prevClose = 0.0, price = 0.0, high = 0.0, low = 0.0, volume = 0L, amount = 0.0)
            sideA.diffUpdate(listOf(rowOf(pairA)))
            sideB.diffUpdate(listOf(rowOf(pairB)))
            sourceLabel = if (live != null) "实时行情" else "本地快照 · 可重试"
            loading = false
        }
        refreshAiCompare()
    }

    /** 双股 AI 对比：首选 SSE 流式（阶段可见、不受移动端 12s 离线超时限制），回退一次性接口。
     *  开始时置空 aiResult：vif 的 null→非空转换强制内容重建（避免捕获旧结果不刷新）。 */
    internal fun refreshAiCompare() {
        aiResult = null
        aiLoading = true
        aiStage = ""
        lifecycleScope.launch {
            val streamed = runCatching {
                GatewayMarketClient.compareStream(pairA, pairB) { stage -> aiStage = stage }
            }.getOrNull()
            val result = streamed ?: runCatching {
                GatewayMarketClient.agentCompare(pairA, pairB)
            }.getOrNull()
            if (result != null) aiResult = result
            aiLoading = false
        }
    }

    override fun body(): ViewBuilder = {
        attr {
            flexDirectionColumn()
            backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        renderCommonOverlays(this@ComparePage, "市场", showBack = true)
        // 内容放 List：小屏（AI 归纳块较高）可滚动，且与市场/自选页同结构
        List {
            attr {
                flex(1f)
                backgroundColor(AppTheme.colors.c(AppTheme.colors.pageBg))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            compareContent(this@ComparePage)
            View { attr { height(32f) } }
        }
        renderBottomTab(this@ComparePage, "市场")
    }
}

// ============== 内容区 ==============
private fun ViewContainer<*, *>.compareContent(host: ComparePage) {
    val colors = AppTheme.colors
    View {
        attr {
            flexDirectionColumn()
            paddingLeft(if (host.isCompact()) 16f else 32f)
            paddingRight(if (host.isCompact()) 16f else 32f)
            paddingBottom(24f)
            backgroundColor(colors.c(colors.pageBg))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            // 手机：撑满 padding 内区域（contentWidth=全屏宽，再叠 padding 会右溢出屏幕）
            // 桌面：显式 1360 上限居中
            attr {
                if (host.isCompact()) flexDirectionColumn()
                else { width(host.contentWidth()); flexDirectionColumn() }
            }
            View { attr { height(24f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs24); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary)); text("对比")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(4f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textSecondary))
                    text(host.sourceLabel + " · 点击任意一侧进个股详情")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(16f) } }
            vif({ host.sideA.isNotEmpty() && host.sideB.isNotEmpty() }) {
                compareCard(host, host.sideA.first(), host.sideB.first())
            }
            View { attr { height(24f) } }
            aiCompareCard(host)
            View { attr { height(24f) } }
        }
    }
}

/** AI 对比卡：LLM 定性归纳（summary/stronger/双列依据/结论）；规则降级与加载态如实标注。
 *  响应性约定：vif 条件直接读 host observable（不得捕获局部 val，否则数据到达不刷新）。 */
private fun ViewContainer<*, *>.aiCompareCard(host: ComparePage) {
    val colors = AppTheme.colors
    View {
        attr {
            borderRadius(AppRadius.radius8)
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            backgroundColor(colors.c(colors.surface))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr {
                flexDirectionRow(); alignItemsCenter()
                padding(left = 14f, right = 14f, top = 12f, bottom = 12f)
            }
            SectionHeader("AI 对比归纳", action = "重新生成", onAction = { host.refreshAiCompare() })
        }
        vif({ host.aiLoading && host.aiResult == null }) {
            View {
                attr { padding(all = 16f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs13)
                        color(colors.c(colors.textTertiary))
                        text(if (host.aiStage.isBlank()) "正在生成 AI 对比…" else "正在生成 AI 对比 · ${host.aiStage}…")
                    }
                }
            }
        }
        vif({ host.aiResult != null }) {
            val r = host.aiResult!!
            View {
                attr { padding(left = 14f, right = 14f, bottom = 14f); flexDirectionColumn() }
                Text {
                    attr {
                        fontSize(AppTypography.fs11)
                        color(colors.c(colors.textTertiary))
                        text(if (r.isLlm) "LLM 对比 · ${r.provider} · 数字均来自服务端行情证据" else "规则降级 · 未伪装模型")
                    }
                }
                View { attr { height(6f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs13); lineHeight(20f)
                        color(colors.c(colors.textSecondary)); text(r.summary)
                    }
                }
                vif({ r.pointsA.isNotEmpty() || r.pointsB.isNotEmpty() }) {
                    View { attr { height(10f) } }
                    View {
                        attr { flexDirectionRow() }
                        comparePointsColumn("A · ${r.stocks[0].name}", r.pointsA, r.stronger == "A")
                        View { attr { width(12f) } }
                        comparePointsColumn("B · ${r.stocks[1].name}", r.pointsB, r.stronger == "B")
                    }
                }
                vif({ r.conclusion.isNotBlank() }) {
                    View { attr { height(8f) } }
                    Text {
                        attr {
                            fontSize(AppTypography.fs12); lineHeight(18f)
                            color(colors.c(colors.textTertiary)); text("结论：${r.conclusion}")
                        }
                    }
                }
                View { attr { height(8f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs11)
                        color(colors.c(colors.textTertiary)); text("仅供信息分析，不构成投资建议")
                    }
                }
            }
        }
        vif({ !host.aiLoading && host.aiResult == null }) {
            View {
                attr { padding(all = 16f); flexDirectionRow(); alignItemsCenter() }
                Text {
                    attr {
                        flex(1f); fontSize(AppTypography.fs13)
                        color(colors.c(colors.textTertiary)); text("AI 对比网关暂不可用")
                    }
                }
            }
        }
    }
}

/** 双列依据：stronger 一侧高亮标记。 */
private fun ViewContainer<*, *>.comparePointsColumn(title: String, points: List<String>, stronger: Boolean) {
    val colors = AppTheme.colors
    View {
        attr { flex(1f); flexDirectionColumn() }
        Text {
            attr {
                fontSize(AppTypography.fs12); fontWeightMedium()
                color(colors.c(if (stronger) colors.up else colors.textSecondary))
                text(title + if (stronger) " ★" else "")
            }
        }
        points.forEach { p ->
            Text {
                attr {
                    marginTop(4f); fontSize(AppTypography.fs12); lineHeight(17f)
                    color(colors.c(colors.textSecondary)); text("· $p")
                }
            }
        }
    }
}

/** 对比卡：表头双列（名称+价格+色块），下方指标行（label 居中 + 左右值）。 */
private fun ViewContainer<*, *>.compareCard(host: ComparePage, a: StockQuote, b: StockQuote) {
    val colors = AppTheme.colors
    View {
        attr {
            borderRadius(AppRadius.radius8)
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            backgroundColor(colors.c(colors.surface))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // 表头：左右名称
        View {
            attr {
                flexDirectionRow(); alignItemsCenter()
                padding(left = 14f, right = 14f, top = 12f, bottom = 12f)
            }
            CompareHeaderCell(a, onClick = { host.openStock(a.symbol) })
            View { attr { width(12f) } }
            CompareHeaderCell(b, onClick = { host.openStock(b.symbol) })
        }
        View {
            attr {
                height(1f); backgroundColor(colors.c(colors.border))
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        compareRow("最新价", com.zhiniu.pages.components.fmt2(a.price), com.zhiniu.pages.components.fmt2(b.price))
        compareRow("涨跌幅", fmtPct(a.changePercent), fmtPct(b.changePercent))
        compareRow("总市值", fmtMarketCap(a.marketCap), fmtMarketCap(b.marketCap))
        compareRow("成交量", com.zhiniu.pages.components.fmtVolHand(a.volume), com.zhiniu.pages.components.fmtVolHand(b.volume))
        compareRow("换手率", com.zhiniu.pages.components.fmtOptional(a.turnoverRate, "%"), com.zhiniu.pages.components.fmtOptional(b.turnoverRate, "%"))
        compareRow("振幅", fmtAmplitude(a.high, a.low, a.prevClose), fmtAmplitude(b.high, b.low, b.prevClose))
    }
}

/** 表头单元格：名称 + 副行（代码·市场）。 */
private fun ViewContainer<*, *>.CompareHeaderCell(q: StockQuote, onClick: () -> Unit) {
    val colors = AppTheme.colors
    View {
        attr {
            flex(1f); flexDirectionColumn()
            cssClass("zn-click")
            highlightBackgroundColor(colors.ca(colors.textSecondary, 6))
            accessibility("查看 ${q.name} 详情")
        }
        event { click { onClick() } }
        Text {
            attr {
                fontSize(AppTypography.fs15); fontWeightSemiBold()
                color(colors.c(colors.textPrimary)); text(q.name); lines(1); textOverFlowClip()
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        Text {
            attr {
                marginTop(2f); fontSize(AppTypography.fs11)
                color(colors.c(colors.textTertiary))
                text(fmtSymbol(q.symbol))
            }
        }
    }
}

/** 指标对比行：中间 label 居中，左右值右/左对齐。 */
private fun ViewContainer<*, *>.compareRow(label: String, valueA: String, valueB: String) {
    val colors = AppTheme.colors
    View {
        attr {
            flexDirectionRow(); alignItemsCenter()
            padding(top = 8f, bottom = 8f, left = 14f, right = 14f)
        }
        Text {
            attr {
                flex(1f); textAlignRight()
                fontSize(AppTypography.fs13); fontWeightMedium(); fontFamily(NUM_FONT)
                color(colors.c(colors.textPrimary)); text(valueA)
            }
        }
        Text {
            attr {
                width(72f); textAlignCenter()
                fontSize(AppTypography.fs11)
                color(colors.c(colors.textTertiary)); text(label)
            }
        }
        Text {
            attr {
                flex(1f)
                fontSize(AppTypography.fs13); fontWeightMedium(); fontFamily(NUM_FONT)
                color(colors.c(colors.textPrimary)); text(valueB)
            }
        }
    }
}
