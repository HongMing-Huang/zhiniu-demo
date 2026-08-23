/* 知牛 · 跳转卡 JumpCard（AI 诊股第 4 卡：主色 Button 点击 openPage + 迷你 K 线预览）
 *
 * // Example: jumpCard(JumpCardUi("查看K线详情", "StockDetail", mapOf("symbol" to "sh600519")))
 *
 * 回调：由 Page 注入 onOpen：(String, Map<String,String>) -> Unit ⇒ openPage 路由。
 */
package com.zhiniu.pages.components

import com.tencent.kuikly.ref.view.ViewBuilder
import com.zhiniu.domain.model.AiCard

/** 跳转卡展示模型（含路由目标）。 */
data class JumpCardUi(
    val title: String,
    val page: String,
    val args: Map<String, String>,
)

/** AiCard(JUMP) → 跳转卡模型。 */
fun AiCard.toJumpUi(): JumpCardUi = JumpCardUi(title, page = page ?: "StockDetail", args = args)

/** 生成跳转卡 DSL：主色 Button + 点击回调。 */
fun jumpCard(ui: JumpCardUi, onOpen: (String, Map<String, String>) -> Unit): ViewBuilder = {
    View {
        attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(0f); marginTop(10f) }
        Text {
            attr {
                text(ui.title)
                color(colorOf(Palette.UP))
                fontSize(13f)
                onClick { onOpen(ui.page, ui.args) }
            }
        }
    }
}