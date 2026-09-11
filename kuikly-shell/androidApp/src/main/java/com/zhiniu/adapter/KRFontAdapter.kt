package com.zhiniu.adapter

import android.graphics.Typeface
import com.tencent.kuikly.core.render.android.adapter.IKRFontAdapter
import com.zhiniu.KRApplication

/**
 * 官方字体接入（IKRFontAdapter）：fontFamily 字符串 → APK assets 内的 TTF。
 * 优先 app 壳 assets/fonts/，回退 shared 模块 assets/common/fonts/；未命中返回 null 走系统字体。
 */
object KRFontAdapter : IKRFontAdapter {
    override fun getTypeface(fontFamily: String, result: (Typeface?) -> Unit) {
        if (fontFamily.isEmpty()) {
            result(null)
            return
        }
        val assets = KRApplication.application.assets
        val typeface = try {
            Typeface.createFromAsset(assets, "fonts/$fontFamily.ttf")
        } catch (e: Exception) {
            try {
                Typeface.createFromAsset(assets, "common/fonts/$fontFamily.ttf")
            } catch (e: Exception) {
                null
            }
        }
        result(typeface)
    }
}
