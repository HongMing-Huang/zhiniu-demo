// 知牛 · 系统外观偏好（H5 actual）
package com.zhiniu.platform

/** H5(JS) actual：matchMedia prefers-color-scheme 检测。 */
actual fun systemPrefersDark(): Boolean {
    val mq = js("window.matchMedia('(prefers-color-scheme: dark)')")
    return mq.matches == true
}

/** H5(JS) actual：监听系统深浅色切换。 */
@Suppress("unused")
actual fun watchSystemTheme(callback: (dark: Boolean) -> Unit) {
    val mq = js("window.matchMedia('(prefers-color-scheme: dark)')")
    mq.addEventListener("change", { e ->
        callback((e.matches == true))
    })
}

/** 同步 host CSS（web-host/index.html 的 data-theme 钩子，用于 hover 色等）。 */
@Suppress("unused")
actual fun applyHostTheme(dark: Boolean) {
    if (dark) {
        js("document.documentElement.setAttribute('data-theme','dark')")
    } else {
        js("document.documentElement.setAttribute('data-theme','light')")
    }
}
