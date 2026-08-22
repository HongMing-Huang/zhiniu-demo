/* 知牛 · 错误卡 AppErrorCard（硬约束 §2.4：错误码徽章 + 分级中文文案 + 重试）
 *
 * // Example: appErrorCard(actualErr) { vm.retry() }
 */
package com.zhiniu.pages.components

import com.tencent.kuikly.ref.view.ViewBuilder
import com.zhiniu.domain.model.AppError

/** 生成统一错误卡 DSL：⚠ 图标 + 错误码徽章 + 分级文案 + 重试按钮。 */
fun appErrorCard(error: AppError, onRetry: () -> Unit): ViewBuilder = {
    View {
        attr { flex(1f); flexDirection(FLEX_DIRECTION_COLUMN); allCenter() }
        // ⚠ 图标（Material Symbols "priority_high"）
        Text { attr { text("⚠"); color(colorOf(Palette.UP)); fontSize(22f) } }
        // 错误码徽章
        Text { attr { text(error.code); color(colorOf("#ffffff")); background(colorOf(Palette.UP)); fontSize(11f); marginTop(6f) } }
        // 分级中文文案
        Text { attr { text(error.message); color(colorOf(Palette.SUB)); fontSize(12f); marginTop(6f); textAlign(TEXT_ALIGN_CENTER) } }
        // 重试
        Text {
            attr {
                text("重试"); color(colorOf(Palette.UP)); fontSize(13f); marginTop(10f)
                onClick { onRetry() }
            }
        }
    }
}