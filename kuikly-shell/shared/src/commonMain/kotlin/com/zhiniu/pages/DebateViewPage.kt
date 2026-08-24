package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.base.BasePager
import com.zhiniu.pages.components.Tokens
import com.zhiniu.pages.components.hexInt

private data class DebatePoint(val speaker: String, val text: String)

/** 多空辩论页：多/空视角气泡 + 分歧点 + 总结卡（B8，Light Token，响应式）。 */
@Page("DebateView", supportInLocal = true)
internal class DebateViewPage : BasePager() {

    private var side by observable("多方")
    private val points by observableList<DebatePoint>()
    private val divergences by observableList<String>()

    override fun willInit() {
        super.willInit()
        listOf(
            DebatePoint("多方", "5 日线金叉 10 日线，短线动能向上"),
            DebatePoint("多方", "量能温和放大，主力资金净流入"),
            DebatePoint("多方", "北向资金连续加仓，估值处于中低位"),
        ).forEach { points.add(it) }
        listOf(
            "分歧①：技术面偏多 vs 基本面增速放缓",
            "分歧②：资金面乐观 vs 估值已透支预期",
            "分歧③：短期情绪强 vs 中期压力未消",
        ).forEach { divergences.add(it) }
        rebuild()
    }

    private fun rebuild() {
        points.clear()
        if (side == "多方") {
            listOf(
                DebatePoint("多方", "5 日线金叉 10 日线，短线动能向上"),
                DebatePoint("多方", "量能温和放大，主力资金净流入"),
                DebatePoint("多方", "北向资金连续加仓，估值处于中低位"),
            ).forEach { points.add(it) }
        } else {
            listOf(
                DebatePoint("空方", "上方套牢盘密集，1300 整数关口压力显著"),
                DebatePoint("空方", "行业景气度边际回落，盈利预期下修"),
                DebatePoint("空方", "量价背离，冲高动能不足"),
            ).forEach { points.add(it) }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr { marginTop(Tokens.space3); marginLeft(Tokens.space3); fontSize(Tokens.fsH2); color(Color(hexInt(Tokens.textPrimary))); text("多空辩论") }
            }
            View {
                attr { flexDirectionRow(); marginTop(Tokens.space2); marginLeft(Tokens.space3) }
                listOf("多方", "空方").forEach { s ->
                    Text {
                        attr {
                            marginRight(Tokens.space4)
                            fontSize(Tokens.fsH3)
                            text(s)
                            color(if (ctx.side == s) Color(hexInt(if (s == "多方") Tokens.up else Tokens.down)) else Color(hexInt(Tokens.textTertiary)))
                        }
                        event { click { ctx.side = s; ctx.rebuild() } }
                    }
                }
            }
            // 该方观点气泡
            List {
                attr { height(220f); marginTop(Tokens.space2) }
                vforLazy({ ctx.points }) { p, _, _ ->
                    View {
                        attr { margin(Tokens.space2); backgroundColor(Color(hexInt(if (p.speaker == "多方") Tokens.bgHover else Tokens.bgCard))) }
                        Text {
                            attr {
                                margin(Tokens.space2); fontSize(Tokens.fsBody)
                                color(Color(hexInt(if (p.speaker == "多方") Tokens.up else Tokens.down)))
                                text("${p.speaker} · ${p.text}")
                            }
                        }
                    }
                }
            }
            // 分歧点高亮
            Text {
                attr { marginLeft(Tokens.space3); marginTop(Tokens.space2); fontSize(Tokens.fsH3); color(Color(hexInt(Tokens.warn))); text("⚠ 分歧点") }
            }
            List {
                attr { height(130f); marginTop(Tokens.space2) }
                vforLazy({ ctx.divergences }) { d, _, _ ->
                    Text { attr { margin(Tokens.space2); fontSize(Tokens.fsBody); color(Color(hexInt(Tokens.textSecondary))); text(d) } }
                }
            }
            // 总结卡
            View {
                attr { marginTop(Tokens.space2); marginLeft(Tokens.space3); marginRight(Tokens.space3); backgroundColor(Color(hexInt(Tokens.bgCard))) }
                Text {
                    attr {
                        margin(Tokens.space2); fontSize(Tokens.fsBody)
                        color(Color(hexInt(Tokens.textPrimary)))
                        text("总结：短线多方占优，但上方 1300 压力明显。建议控制仓位，关注量能能否持续。")
                    }
                }
            }
        }
    }
}