/* 知牛 · 快捷指令 Chip QuickChip（T2-2.3：快捷指令 = GHOST + 16 圆角，按钮语义而非 Text 冒充）
 *
 * // Example: quickChip("看大盘") { vm.send("看大盘") }
 */
package com.zhiniu.pages.components

import com.tencent.kuikly.ref.view.ViewBuilder

/** 生成快捷指令 Chip（GHOST 描边 + 16 圆角 + 点击回调）。 */
fun quickChip(label: String, onClick: () -> Unit): ViewBuilder = {
    View {
        attr {
            flexGrow(0f)
            height(32f)
            marginEnd(8f)
            borderRadius(16f)
            background(colorOf("#ffffff"))
            border(1f, colorOf(Palette.SUB))
            justifyContent(FLEX_JUSTIFY_CONTENT_CENTER)
        }
        Text {
            attr {
                text(label); color(colorOf(Palette.TEXT)); fontSize(13f)
                onClick { onClick() }
            }
        }
    }
}