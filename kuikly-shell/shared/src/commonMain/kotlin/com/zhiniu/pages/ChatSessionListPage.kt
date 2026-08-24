package com.zhiniu.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.zhiniu.base.BasePager
import com.zhiniu.base.openZhiniuPage
import com.zhiniu.pages.components.Tokens
import com.zhiniu.pages.components.hexInt

private data class Session(val title: String, val preview: String, val time: String)

/** 会话列表页：历史会话 + 新建入口（B7，Light Token，响应式列表）。 */
@Page("ChatSessionList", supportInLocal = true)
internal class ChatSessionListPage : BasePager() {

    private val sessions by observableList<Session>()

    override fun willInit() {
        super.willInit()
        listOf(
            Session("贵州茅台诊股", "AI：震荡偏强，关注 1300 关口…", "10:32"),
            Session("对比招商银行与平安银行", "招行息差韧性更优…", "09:15"),
            Session("解释 RSI 指标", "RSI=62，短期偏强…", "昨天"),
            Session("看大盘", "上证 3245，温和放量上行…", "昨天"),
        ).forEach { sessions.add(it) }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr { flexDirectionRow(); marginTop(Tokens.space3); marginLeft(Tokens.space3); marginRight(Tokens.space3) }
                Text { attr { flex(1f); fontSize(Tokens.fsH2); color(Color(hexInt(Tokens.textPrimary))); text("会话") } }
                Text {
                    attr { fontSize(Tokens.fsBody); color(Color(hexInt(Tokens.brand))); text("＋ 新建") }
                    event { click { ctx.openZhiniuPage("ChatHome") } }
                }
            }
            List {
                attr { height(520f); marginTop(Tokens.space2) }
                vforLazy({ ctx.sessions }) { s, _, _ ->
                    View {
                        attr { margin(Tokens.space2); backgroundColor(Color(hexInt(Tokens.bgCard))) }
                        Text { attr { marginLeft(Tokens.space2); marginTop(Tokens.space2); fontSize(Tokens.fsH3); color(Color(hexInt(Tokens.textPrimary))); text(s.title) } }
                        Text { attr { marginLeft(Tokens.space2); marginTop(Tokens.space1); fontSize(Tokens.fsCaption); color(Color(hexInt(Tokens.textTertiary))); text("${s.time} · ${s.preview}") } }
                    }
                }
            }
        }
    }
}