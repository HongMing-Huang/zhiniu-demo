// 知牛 · AI 结构化块渲染（AiStockCard / Text / Metrics / Risk / FollowUps）
// 股票卡点击进入 StockDetailPage；追问点击直接发送。
package com.zhiniu.pages.components.ai

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.domain.model.StockQuote
import com.zhiniu.domain.repository.AiBlock
import com.zhiniu.pages.components.ANIM_THEME
import com.zhiniu.pages.components.AppRadius
import com.zhiniu.pages.components.AppTheme
import com.zhiniu.pages.components.AppTypography
import com.zhiniu.pages.components.Icon
import com.zhiniu.pages.components.IconKind
import com.zhiniu.pages.components.NUM_FONT
import com.zhiniu.pages.components.c
import com.zhiniu.pages.components.ca
import com.zhiniu.pages.components.common.Divider
import com.zhiniu.pages.components.cssClass
import com.zhiniu.pages.components.drawSparkLine
import com.zhiniu.pages.components.fmt2
import com.zhiniu.pages.components.fmtPct
import com.zhiniu.pages.components.fmtSymbol

/** 按序渲染一块（AI 消息内容）。 */
fun ViewContainer<*, *>.AiBlockView(
    block: AiBlock,
    onOpenStock: (String) -> Unit,
    onAsk: (String) -> Unit,
) {
    when (block) {
        is AiBlock.Text -> AiTextBlock(block.content)
        is AiBlock.StockCard -> AiStockCard(
            block.symbol, block.trend, block.rsi, block.summary,
        ) { onOpenStock(block.symbol) }
        is AiBlock.Metrics -> AiMetricsBlock(block.title, block.rows)
        is AiBlock.Risk -> AiRiskBlock(block.title, block.content)
        is AiBlock.FollowUps -> AiFollowUps(block.questions, onAsk)
        is AiBlock.TradeAdvice -> AiTradeAdviceCard(block)
        is AiBlock.RiskLevel -> AiRiskLevelChip(block)
        is AiBlock.KLine -> AiKLineCard(block)
    }
}

/** 风险五档 → 语义色。 */
internal fun riskColorOf(level: String): Color {
    val colors = AppTheme.colors
    return colors.c(
        when (level) {
            "low" -> colors.riskLow
            "medium_low" -> colors.riskMediumLow
            "medium" -> colors.riskMedium
            "medium_high" -> colors.riskMediumHigh
            else -> colors.riskHigh
        }
    )
}

/** 买卖观察区间卡：买入（低位·绿）/ 卖出（高位·红）双 chip + 依据（服务端规则推导）。 */
private fun ViewContainer<*, *>.AiTradeAdviceCard(block: AiBlock.TradeAdvice) {
    val colors = AppTheme.colors
    View {
        attr {
            borderRadius(AppRadius.radius8)
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            backgroundColor(colors.c(colors.surface))
            padding(top = 12f, bottom = 12f, left = 14f, right = 14f)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            Text {
                attr {
                    fontSize(AppTypography.fs14); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary)); text("买卖观察区间")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { flex(1f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs11)
                    color(colors.c(colors.textTertiary)); text("规则推导 · 非投资建议")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        View { attr { height(10f) } }
        AdviceRow("买入观察", block.buyRange, colors.c(colors.down), colors.c(colors.fallBackground))
        View { attr { height(6f) } }
        AdviceRow("卖出观察", block.sellRange, colors.c(colors.up), colors.c(colors.riseBackground))
        View { attr { height(8f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12); lineHeight(18f)
                color(colors.c(colors.textSecondary)); text(block.rationale)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        block.basis.forEach { b ->
            View { attr { height(2f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs11); color(colors.c(colors.textTertiary))
                    text("· " + b)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.AdviceRow(label: String, range: String, tint: Color, bg: Color) {
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        View {
            attr {
                height(22f); padding(left = 8f, right = 8f); allCenter()
                borderRadius(4f); backgroundColor(bg)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
            Text {
                attr {
                    fontSize(AppTypography.fs11); fontWeightMedium(); color(tint); text(label)
                }
            }
        }
        View { attr { width(10f) } }
        Text {
            attr {
                fontSize(AppTypography.fs14); fontWeightSemiBold()
                fontFamily(NUM_FONT); color(tint); text(range)
            }
        }
    }
}

/** 风险分级：五档色 chip + 规则依据。 */
private fun ViewContainer<*, *>.AiRiskLevelChip(block: AiBlock.RiskLevel) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter() }
        Text {
            attr {
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textTertiary))
                text("风险分级"); marginRight(8f)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View {
            attr {
                height(20f); padding(left = 8f, right = 8f); allCenter()
                borderRadius(4f); backgroundColor(riskColorOf(block.level))
            }
            Text {
                attr {
                    fontSize(AppTypography.fs11); fontWeightSemiBold()
                    color(Color(0xFFFFFFFF)); text(block.label)
                }
            }
        }
        View { attr { width(8f) } }
        Text {
            attr {
                fontSize(AppTypography.fs12); flex(1f); lineHeight(18f)
                color(colors.c(colors.textSecondary)); text(block.rationale)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

/** 迷你走势卡：服务端研究管线带出的近 60 根收盘价（不二次请求）。 */
private fun ViewContainer<*, *>.AiKLineCard(block: AiBlock.KLine) {
    if (block.closes.size < 2) return
    val colors = AppTheme.colors
    val isUp = block.closes.last() >= block.closes.first()
    val lineColor = colors.c(if (isUp) colors.up else colors.down)
    View {
        attr { flexDirectionColumn() }
        View {
            attr { flexDirectionRow(); alignItemsCenter() }
            Text {
                attr {
                    fontSize(AppTypography.fs12)
                    color(colors.c(colors.textTertiary))
                    text("近 " + block.closes.size + " 日收盘")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { flex(1f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12); fontWeightMedium(); fontFamily(NUM_FONT)
                    color(lineColor); text(fmt2(block.closes.last()))
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
        View { attr { height(6f) } }
        Canvas({ attr { width(480f); height(96f) } }) { context, w, h ->
            drawSparkLine(context, block.closes, lineColor, w, h, endDot = true)
        }
    }
}

private fun ViewContainer<*, *>.AiTextBlock(content: String) {
    // Task2 评分点「AI 返回内容渲染」：Markdown（段落/列表/表格/代码块），
    // 纯文本按普通段落渲染，向后兼容结构化摘要。
    MarkdownView(content)
}

/** 股票卡片（AiResearch 聊天流）。 */
fun ViewContainer<*, *>.AiStockCard(
    symbol: String,
    trend: String,
    rsi: Double,
    summary: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val q = com.zhiniu.data.mock.MarketStore.repository.quoteOf(symbol)
    View {
        attr {
            borderRadius(AppRadius.radius8)
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            backgroundColor(colors.c(colors.surface))
            overflow(true)
            cssClass("zn-card zn-click")
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        event { click { onClick() } }
        View {
            attr { padding(top = 14f, left = 14f, right = 14f) }
            View {
                attr { flexDirectionRow(); alignItemsCenter() }
                Text {
                    attr {
                        fontSize(AppTypography.fs16); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text(q?.name ?: symbol)
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
                Text {
                    attr {
                        marginLeft(10f); marginTop(3f
                        )
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textTertiary))
                        text(q?.let { fmtSymbol(it.symbol) } ?: "")
                    }
                }
                View { attr { flex(1f) } }
            }
            View { attr { height(8f) } }
            View {
                attr { flexDirectionRow(); alignItemsCenter() }
                Text {
                    attr {
                        fontSize(AppTypography.fs24); fontWeightSemiBold()
                        fontFamily(NUM_FONT)
                        color(colors.c(if (q?.isUp == true) colors.up else colors.down))
                        text(q?.let { fmt2(it.price) } ?: "--")
                        animate(ANIM_THEME, value = AppTheme.isDark)
                    }
                }
                Text {
                    attr {
                        marginLeft(10f); marginTop(6f)
                        fontSize(AppTypography.fs13)
                        fontFamily(NUM_FONT)
                        color(colors.c(if (q?.isUp == true) colors.up else colors.down))
                        text(q?.let { fmtPct(it.changePercent) } ?: "")
                    }
                }
            }
            View { attr { height(10f) } }
            AiKeyValue("趋势", trend)
            AiKeyValue("RSI", fmt2(rsi))
            View { attr { height(8f) } }
            Text {
                attr {
                    fontSize(AppTypography.fs12); lineHeight(19f)
                    color(colors.c(colors.textSecondary))
                    text(summary)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
            View { attr { height(12f) } }
            Divider()
            View { attr { height(10f) } }
            // 查看详情：卡片页脚行（左文案 + 右箭头，行可供性）
            View { attr { flexDirectionRow(); alignItemsCenter() }
                Icon(IconKind.CHART, 13f)
                View { attr { width(5f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text("查看详情")
                    }
                }
                View { attr { flex(1f) } }
                Icon(IconKind.CHEVRON_RIGHT, 11f)
            }
        }
    }
}

private fun ViewContainer<*, *>.AiKeyValue(label: String, value: String) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); alignItemsCenter(); marginBottom(4f) }
        Text {
            attr {
                width(60f)
                fontSize(AppTypography.fs12)
                color(colors.c(colors.textTertiary))
                text(label)
            }
        }
        // flex(1f) 吃满剩余行宽：窄容器下数字不再被挤压折行（如 63.90 → 63.9/0）
        // 等宽字体仅用于含数字的值；纯中文值走系统字体，避免中英混排字号不一
        Text {
            attr {
                flex(1f)
                fontSize(AppTypography.fs13); fontWeightMedium()
                if (value.any { it.isDigit() }) fontFamily(NUM_FONT)
                color(colors.c(colors.textPrimary))
                text(value)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
    }
}

private fun ViewContainer<*, *>.AiMetricsBlock(title: String, rows: List<com.zhiniu.domain.repository.MetricCell>) {
    val colors = AppTheme.colors
    View {
        attr {
            borderRadius(AppRadius.radius8)
            backgroundColor(colors.c(colors.surfaceSecondary))
            padding(top = 12f, left = 14f, right = 14f, bottom = 12f)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        Text {
            attr {
                fontSize(AppTypography.fs13); fontWeightSemiBold()
                color(colors.c(colors.textPrimary))
                text(title)
            }
        }
        View { attr { height(6f) } }
        rows.forEach { row ->
            // 标签固定宽不折行；值占满剩余宽度右对齐，长文本（辩论观点）允许多行、非等宽字体
            View {
                attr { flexDirectionRow(); alignItemsFlexStart(); minHeight(24f); padding(top = 2f, bottom = 2f) }
                Text {
                    attr {
                        width(76f); lines(1)
                        fontSize(AppTypography.fs13); lineHeight(20f)
                        color(colors.c(colors.textSecondary))
                        text(row.label)
                    }
                }
                View { attr { width(8f) } }
                Text {
                    attr {
                        flex(1f); textAlignRight()
                        fontSize(AppTypography.fs13); fontWeightMedium(); lineHeight(20f)
                        if (row.value.length <= 16) fontFamily(NUM_FONT)
                        color(colors.c(colors.textPrimary))
                        text(row.value)
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.AiRiskBlock(title: String, content: String) {
    val colors = AppTheme.colors
    View {
        attr {
            flexDirectionRow()
            borderRadius(AppRadius.radius8)
            border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            backgroundColor(colors.c(colors.surface))
            overflow(true)
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        View { attr { width(3f); backgroundColor(colors.ca(colors.down, 80)) } }
        View {
            attr { flex(1f); padding(top = 10f, left = 12f, right = 12f, bottom = 10f) }
            Text {
                attr {
                    fontSize(AppTypography.fs12); fontWeightSemiBold()
                    color(colors.c(colors.textPrimary))
                    text(title)
                }
            }
            Text {
                attr {
                    marginTop(4f)
                    fontSize(AppTypography.fs12); lineHeight(19f)
                    color(colors.c(colors.textSecondary))
                    text(content)
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.AiFollowUps(questions: List<String>, onAsk: (String) -> Unit) {
    val colors = AppTheme.colors
    View {
        attr { flexDirectionRow(); flexWrapWrap() }
        questions.forEach { question ->
            View {
                attr {
                    height(30f); borderRadius(15f)
                    marginRight(8f); marginTop(6f)
                    padding(left = 12f, right = 12f)
                    allCenter()
                    backgroundColor(colors.c(colors.surfaceSecondary))
                    border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                    cssClass("zn-click")
                    animate(ANIM_THEME, value = AppTheme.isDark)
                }
                event { click { onAsk(question) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textSecondary))
                        text(question)
                    }
                }
            }
        }
    }
}
