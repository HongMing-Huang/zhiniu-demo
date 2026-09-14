package com.zhiniu.base

import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal class BridgeModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun closePage() {
        callNativeMethod(CLOSE_PAGE, null, null)
    }

    fun log(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod(LOG, methodArgs, null)
    }

    fun toast(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod("toast", methodArgs, null)
    }

    fun openPage(url: String, closeCurPage: Boolean = false, closeSamePage: Boolean = false, userData: JSONObject? = null, callbackFn: CallbackFn? = null) {
        val methodArgs = JSONObject()
        methodArgs.put("url", url)
        methodArgs.put("closeCurPage", closeCurPage.toInt())
        methodArgs.put("closeSamePage", closeSamePage.toInt())
        userData?.also {
            methodArgs.put("userData", it)
        }
        callNativeMethod(OPEN_PAGE, methodArgs, callbackFn)
    }

    suspend fun ssoRequest(cmd: String, reqParams: JSONObject): JSONObject? {
        return suspendCoroutine<JSONObject?> { continuation ->
            ssoRequest(cmd, reqParams) {
                continuation.resume(it)
            }
        }
    }

    fun ssoRequest(cmd: String, reqParams: JSONObject, responseCallbackFn: CallbackFn) {
        val methodArgs = JSONObject()
        methodArgs.put("cmd", cmd)
        methodArgs.put("reqParam", reqParams)
        callNativeMethod(SSO_REQUEST, methodArgs, responseCallbackFn)
    }

    fun currentTimeStamp(): Long {
        val timestamp = syncCallNativeMethod(CURRENT_TIMESTAMP, null, null)
        return if (timestamp.isNotEmpty()) timestamp.toLong() else 0
    }

    fun dateFormatter(timeStamp: Long, format: String): String {
        val params = JSONObject()
        params.put("timeStamp", timeStamp)
        params.put("format", format)
        return syncCallNativeMethod(DATE_FORMATTER, params, null)
    }

    /**
     * HTTP 请求（iOS 壳桥实现：NSURLSession；回调由 Kuikly 编组回 Context 线程）。
     * @return 响应文本；非 2xx 或传输错误抛异常（调用方按既有降级逻辑处理）。
     */
    fun httpRequest(url: String, method: String, body: String?, callbackFn: CallbackFn) {
        val methodArgs = JSONObject()
        methodArgs.put("url", url)
        methodArgs.put("method", method)
        body?.also { methodArgs.put("body", it) }
        callNativeMethod(HTTP_REQUEST, methodArgs, callbackFn)
    }

    /**
     * HTTP 同步请求（iOS：syncCallNative 直返，Context 线程阻塞至响应/超时）。
     * 仅用于短请求（localhost 行情/搜索）；返回解析后的包装 JSON，error 非空 = 失败。
     */
    fun httpRequestSync(url: String, method: String, body: String?): JsonObject {
        val methodArgs = JSONObject()
        methodArgs.put("url", url)
        methodArgs.put("method", method)
        body?.also { methodArgs.put("body", it) }
        val raw = syncCallNativeMethod(HTTP_REQUEST_SYNC, methodArgs, null)
        return runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrElse { JsonObject(emptyMap()) }
    }

    /** 回调风格（供平台 actual 在 suspendCoroutine 内使用）；Result = 成功响应文本 / 失败异常。 */
    fun httpRequestAwaitVia(url: String, method: String, body: String?, callbackFn: (Result<String>) -> Unit) {
        httpRequest(url, method, body) { data ->
            val result = data?.optString("result").orEmpty()
            val error = data?.optString("error").orEmpty()
            val statusCode = data?.optInt("statusCode") ?: 0
            if (error.isNotEmpty() || statusCode !in 200..299) {
                callbackFn(Result.failure(RuntimeException("bridge http $statusCode: $error")))
            } else {
                callbackFn(Result.success(result))
            }
        }
    }

    suspend fun httpRequestAwait(url: String, method: String, body: String?): String {
        return suspendCoroutine { continuation ->
            httpRequestAwaitVia(url, method, body) { result ->
                result.fold(continuation::resume, continuation::resumeWithException)
            }
        }
    }

    private fun callNativeMethod(methodName: String, data: JSONObject?, callbackFn: CallbackFn?) {
        toNative(false, methodName, data?.toString(), callbackFn, false)
    }

    private fun syncCallNativeMethod(methodName: String, data: JSONObject?, callbackFn: CallbackFn?): String {
        return toNative(false, methodName, data?.toString(), callbackFn, true).toString()
    }

    companion object {
        const val MODULE_NAME = "HRBridgeModule"
        const val OPEN_PAGE = "openPage"
        const val CLOSE_PAGE = "closePage"
        const val LOG = "log"
        const val SSO_REQUEST = "ssoRequest"
        const val CURRENT_TIMESTAMP = "currentTimestamp"
        const val DATE_FORMATTER = "dateFormatter"
        const val HTTP_REQUEST = "httpRequest"
        const val HTTP_REQUEST_SYNC = "httpRequestSync"
    }
}
