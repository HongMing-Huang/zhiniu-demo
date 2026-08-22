/* 知牛 · 骨架屏 Skeleton（T2-2.5：替换各处 "加载中…" 文字，抗白屏）
 *
 * // Example: skeletonRow() / skeletonText() / skeletonKline()
 * 说明：灰色占位块，视觉动效（呼吸闪烁）以 Kuikly 动画构建块接入；此处先给静态度量。
 */
package com.zhiniu.pages.components

import com.tencent.kuikly.ref.view.ViewBuilder

private const val SKELETON_BG = "#e7e9ee"

/** 列表行骨架。 */
fun skeletonRow(): ViewBuilder = {
    View {
        attr { flexDirection(FLEX_DIRECTION_ROW); flexGrow(1f); marginTop(10f) }
        View { attr { width(80f); height(14f); background(colorOf(SKELETON_BG)) } }
        View { attr { flexGrow(1f); height(14f); marginStart(8f); background(colorOf(SKELETON_BG)) } }
    }
}

/** 文本块骨架。 */
fun skeletonText(width: Float = 160f): ViewBuilder = {
    View { attr { width(width); height(14f); background(colorOf(SKELETON_BG)) } }
}

/** K 线骨架（一个灰色区块兜底，加载完成即替换为真实 Canvas K 线）。 */
fun skeletonKline(height: Float = 120f): ViewBuilder = {
    View { attr { flex(1f); height(height); background(colorOf(SKELETON_BG)) } }
}