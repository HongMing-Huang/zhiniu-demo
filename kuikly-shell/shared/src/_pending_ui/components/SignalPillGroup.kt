/* 知牛 · 信号标签组 SignalPillGroup（AI 诊股第 2 卡：≤6 个 Pill，红涨绿跌）
 *
 * // Example: signalPillGroup(listOf(SignalPillUi("MA_BREAK","5日均线金叉", isUp=true)))
 */
package com.zhiniu.pages.components

import com.tencent.kuikly.ref.view.ViewBuilder
import com.zhiniu.domain.model.AiSignal

/** 信号 Pill 展示模型。 */
data class SignalPillUi(val label: String, val up: Boolean)

/** ≤6 个信号 → Pill 列表（A 股红涨绿跌）。 */
fun signalsToPills(value: List<AiSignal>): List<SignalPillUi> =
    value.take(6).map { SignalPillUi("${it.type} ${it.detail}", up = true) }

/** 生成信号标签组 DSL（横向排列的 Pill View）。 */
fun signalPillGroup(pills: List<SignalPillUi>): ViewBuilder = {
    View {
        attr { flexDirection(FLEX_DIRECTION_COLUMN); flexGrow(1f); marginTop(8f) }
        pills.forEach { p ->
            View {
                attr {
                    flexGrow(0f)
                    marginTop(4f)
                    height(22f)
                    background(colorOf(if (p.up) Palette.UP else Palette.DOWN))
                    justifyContent(FLEX_JUSTIFY_CONTENT_CENTER)
                }
                Text { attr { text(p.label); color(colorOf("#ffffff")); fontSize(12f) } }
            }
        }
    }
}