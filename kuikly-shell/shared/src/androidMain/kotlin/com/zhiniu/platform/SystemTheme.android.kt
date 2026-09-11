// 知牛 · 系统外观偏好（Android actual）
package com.zhiniu.platform

import android.content.res.Configuration
import android.content.res.Resources

/** 免 Context 读取系统夜间模式（Resources.getSystem 反映全局配置）。 */
actual fun systemPrefersDark(): Boolean =
    (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

/** Android 无免 Context 的全局配置监听；宿主 Activity 的 onConfigurationChanged 重建页面即可跟随。 */
actual fun watchSystemTheme(callback: (dark: Boolean) -> Unit) {
}

/** 原生端主题由 Kuikly 视图自身着色，无 host CSS 需同步。 */
actual fun applyHostTheme(dark: Boolean) {
}
