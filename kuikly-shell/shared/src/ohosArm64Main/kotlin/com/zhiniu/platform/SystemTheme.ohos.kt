// 知牛 · HarmonyOS actual：系统主题（DevEco 环境前按浅色处理，回调不触发；接 napi 后补 KRBridge 事件）。
package com.zhiniu.platform

actual fun systemPrefersDark(): Boolean = false

actual fun watchSystemTheme(callback: (dark: Boolean) -> Unit) {
    // 鸿蒙深浅色事件经 KRBridgeModule 接入时补实现；当前回调不触发（App 内仍可手动切主题）
}

actual fun applyHostTheme(dark: Boolean) {
    // 非 Web 端 no-op
}
