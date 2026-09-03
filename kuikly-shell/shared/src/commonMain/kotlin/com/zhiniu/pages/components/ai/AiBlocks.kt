// 知牛 · AI 结构化块渲染（AiStockCard / Text / Metrics / Risk / FollowUps）
// 股票卡点击进入 StockDetailPage；追问点击直接发送。
package com.zhiniu.pages.components.ai

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewContainer
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
import com.zhiniu.pages.components.cssClass
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
    }
}

private fun ViewContainer<*, *>.AiTextBlock(content: String) {
    val colors = AppTheme.colors
    content.split("\n").forEach { line ->
        Text {
            attr {
                fontSize(AppTypography.fs13); lineHeight(21f)
                color(colors.c(colors.textSecondary))
                text(line)
                animate(ANIM_THEME, value = AppTheme.isDark)
            }
        }
        View { attr { height(4f) } }
    }
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
            View { attr { height(10f) } }
            // 查看详情 →
            View { attr { flexDirectionRow(); alignItemsCenter() }
                Text {
                    attr {
                        fontSize(AppTypography.fs12); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text("查看详情")
                    }
                }
                Text {
                    attr {
                        marginLeft(4f)
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textTertiary))
                        text("→")
                    }
                }
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
        Text {
            attr {
                fontSize(AppTypography.fs13)
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
            View {
                attr { flexDirectionRow(); alignItemsCenter(); height(24f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs13)
                        color(colors.c(colors.textSecondary))
                        text(row.label)
                    }
                }
                View { attr { flex(1f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs13); fontWeightMedium()
                        fontFamily(NUM_FONT)
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
