// 知牛 · AiInsightPanel（个股 AI 解读面板；原地替换 Right Rail）
// 宽 400，原地替换 KeyData + AiQuickInsight；不盖整页。
// 结构：Header(72) / 综合判断 / 趋势·信号·量能·技术指标·风险 / FollowUp chips / Composer(42~44) + 圆形 34 发送。
package com.zhiniu.pages.components.ai

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.List
import com.zhiniu.domain.model.AiInsight
import com.zhiniu.domain.model.StockQuote
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
import com.zhiniu.pages.components.fmtSymbol
import com.zhiniu.pages.components.common.AppInput
import com.zhiniu.pages.components.common.Divider
import com.zhiniu.pages.components.common.PrimaryButton

/** 追问问答行。 */
data class AiPanelChatLine(val role: String, val text: String)

/**
 * AI 解读面板（宽度 400；由页面决定是否替换 Rail）。
 */
fun ViewContainer<*, *>.AiInsightPanel(
    width: Float,
    height: Float = 0f,
    quote: () -> StockQuote?,
    insight: () -> AiInsight?,
    followUpText: () -> String,
    chatLines: () -> ObservableList<AiPanelChatLine>,
    onFollowUpChange: (String) -> Unit,
    onFollowUpSend: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = AppTheme.colors
    View {
        attr {
            width(width)
            if (height > 0f) height(height)
            flexDirectionColumn()
            backgroundColor(colors.c(colors.surface))
            borderLeft(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            animate(ANIM_THEME, value = AppTheme.isDark)
        }
        // Header 72
        View {
            attr {
                height(72f); flexDirectionRow(); alignItemsCenter()
                padding(left = 18f, right = 10f)
                borderBottom(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            }
            View {
                attr { flex(1f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs15); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text("AI 分析")
                    }
                }
            }
            View {
                attr {
                    width(34f); height(34f); borderRadius(AppRadius.radius6)
                    allCenter(); cssClass("zn-iconbtn zn-click")
                }
                event { click { onClose() } }
                Icon(IconKind.CLOSE, 15f)
            }
        }
        // 内容独立滚动，避免 Follow-up 与问答被固定 Composer 裁切。
        List {
            attr { flex(1f) }
            vif({ quote() != null && insight() != null }) {
            val q = quote()!!
            val view = insight()!!
            View {
                attr { padding(top = 14f, left = 18f, right = 18f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs18); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text(q.name)
                    }
                }
                Text {
                    attr {
                        marginTop(3f)
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textTertiary))
                        text(fmtSymbol(q.symbol))
                    }
                }
            }
            View { attr { height(8f) } }
            // 综合判断
            View {
                attr { padding(left = 18f, right = 18f) }
                Text {
                    attr {
                        fontSize(AppTypography.fs12)
                        color(colors.c(colors.textTertiary))
                        text("综合判断")
                    }
                }
                Text {
                    attr {
                        marginTop(4f)
                        fontSize(AppTypography.fs20); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text(view.verdict)
                    }
                }
            }
            View { attr { height(14f) } }
            Divider()
            View { attr { height(10f) } }
            // Sections
            View {
                attr { padding(left = 18f, right = 18f); flex(1f) }
                PanelSection("趋势", view.trend)
                PanelSection("信号", view.indicator)
                PanelSection("量能", view.volume)
                PanelSection("技术指标", "RSI 与 MACD 动能" + (if (view.verdict.contains("弱")) " 减弱" else " 稳定"))
                PanelSection("风险", view.risk, risk = true)
                View { attr { height(14f) } }
                Text {
                    attr {
                        fontSize(AppTypography.fs12); fontWeightSemiBold()
                        color(colors.c(colors.textPrimary))
                        text("继续追问")
                    }
                }
                View { attr { height(8f) } }
                View {
                    attr { flexDirectionRow(); flexWrapWrap() }
                    view.followUps.forEach { q ->
                        View {
                            attr {
                                height(30f); borderRadius(15f)
                                marginRight(8f); marginTop(6f)
                                padding(left = 12f, right = 12f)
                                allCenter()
                                backgroundColor(colors.c(colors.surfaceSecondary))
                                border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                                cssClass("zn-click")
                            }
                            event { click { onFollowUpSend(q) } }
                            Text {
                                attr {
                                    fontSize(AppTypography.fs12); lines(1)
                                    color(colors.c(colors.textSecondary))
                                    text(q)
                                }
                            }
                        }
                    }
                }
                View { attr { height(14f) } }
                Divider()
                View { attr { height(10f) } }
                // 追问问答流
                vfor({ chatLines() }) { line ->
                    View {
                        attr { marginBottom(8f) }
                        if (line.role == "user") {
                            View {
                                attr {
                                    alignSelfFlexEnd()
                                    maxWidth(320f)
                                    borderRadius(AppRadius.radius6)
                                    backgroundColor(colors.c(colors.surfaceSecondary))
                                    padding(top = 6f, left = 10f, right = 10f, bottom = 6f)
                                }
                                Text {
                                    attr {
                                        fontSize(AppTypography.fs12); lineHeight(18f)
                                        color(colors.c(colors.textPrimary))
                                        text(line.text)
                                    }
                                }
                            }
                        } else {
                            View {
                                attr {
                                    alignSelfFlexStart()
                                    maxWidth(320f)
                                    borderRadius(AppRadius.radius6)
                                    backgroundColor(colors.c(colors.surface))
                                    border(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
                                    padding(top = 6f, left = 10f, right = 10f, bottom = 6f)
                                }
                                Text {
                                    attr {
                                        fontSize(AppTypography.fs12); lineHeight(18f)
                                        color(colors.c(colors.textSecondary))
                                        text(line.text)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            View { attr { height(14f) } }
        }
        }
        // Composer (固定底部 42-44)
        var composerRef: ViewRef<InputView>? = null
        View {
            attr {
                flexDirectionRow(); alignItemsCenter()
                height(54f)
                padding(left = 14f, right = 14f, top = 8f, bottom = 8f)
                borderTop(Border(1f, BorderStyle.SOLID, colors.c(colors.border)))
            }
            AppInput(
                placeholder = "继续追问…",
                text = followUpText(),
                height = 36f,
                onTextChange = onFollowUpChange,
                onReturn = {
                    val question = followUpText()
                    if (question.isNotBlank()) {
                        onFollowUpSend(question)
                        composerRef?.view?.setText("")
                    }
                },
                onRef = { composerRef = it },
            )
            View { attr { width(8f) } }
            PrimaryButton("发送", height = 36f, icon = IconKind.SEND) {
                    val question = followUpText()
                    if (question.isNotBlank()) {
                        onFollowUpSend(question)
                        composerRef?.view?.setText("")
                    }
            }
        }
    }
}

private fun ViewContainer<*, *>.PanelSection(label: String, content: String, risk: Boolean = false) {
    val colors = AppTheme.colors
    View {
        attr { marginBottom(12f) }
        Text {
            attr {
                fontSize(AppTypography.fs13); fontWeightSemiBold()
                color(colors.c(if (risk) colors.down else colors.textPrimary))
                text(label)
            }
        }
        Text {
            attr {
                marginTop(4f)
                fontSize(AppTypography.fs13); lineHeight(20.15f)
                color(colors.c(colors.textSecondary))
                text(content)
            }
        }
    }
}
