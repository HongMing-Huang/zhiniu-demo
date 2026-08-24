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

/** 模型设置页：14 家服务商 + 状态徽章 + 用量展示（B2，Light Token，响应式列表）。 */
@Page("ModelSettings", supportInLocal = true)
internal class ModelSettingsPage : BasePager() {

    private var tab by observable("模型")

    private val providerRows by observableList<String>()
    private val rows by observableList<String>()

    override fun willInit() {
        super.willInit()
        listOf(
            "OpenAI · 国际", "DeepSeek · 国内", "智谱 GLM · 国内", "腾讯混元 · 国内",
            "阿里通义千问 · 国内", "月之暗面 Kimi · 国内", "字节豆包 · 国内", "百度千帆 · 国内",
            "硅基流动 · 国内", "OpenRouter · 国际", "CherryIN · 国内", "AiHubMix · 国际",
            "DMXAPI · 国内", "Azure OpenAI · 国际", "自定义 · 自定义",
        ).forEach { providerRows.add(it) }
        rebuilt()
    }

    private fun rebuilt() {
        rows.clear()
        if (tab == "用量") {
            rows.add("zhiniu/quick   request 12   p50 320ms  p95 890ms   degraded 0%")
            rows.add("zhiniu/think   request 8    p50 980ms  p95 2200ms  degraded 0%")
            rows.add("zhiniu/flash   request 5    p50 180ms  p95 420ms   degraded 40%")
        } else {
            providerRows.forEach { rows.add(it) }
        }
    }

    private fun rowColor(r: String): Color =
        if (r.contains("国际")) Color(hexInt(Tokens.info)) else Color(hexInt(Tokens.textPrimary))

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr { marginTop(Tokens.space3); marginLeft(Tokens.space3); fontSize(Tokens.fsH2); color(Color(hexInt(Tokens.textPrimary))); text("模型设置") }
            }
            View {
                attr { flexDirectionRow(); marginTop(Tokens.space2); marginLeft(Tokens.space3) }
                listOf("模型", "用量").forEach { t ->
                    Text {
                        attr {
                            marginRight(Tokens.space4)
                            fontSize(Tokens.fsH3)
                            text(t)
                            color(if (ctx.tab == t) Color(hexInt(Tokens.textPrimary)) else Color(hexInt(Tokens.textTertiary)))
                        }
                        event { click { ctx.tab = t; ctx.rebuilt() } }
                    }
                }
            }
            List {
                attr { height(520f); marginTop(Tokens.space2) }
                vforLazy({ ctx.rows }) { row, _, _ ->
                    Text {
                        attr {
                            margin(Tokens.space2)
                            fontSize(Tokens.fsBody)
                            color(ctx.rowColor(row))
                            text(row)
                        }
                    }
                }
            }
        }
    }
}