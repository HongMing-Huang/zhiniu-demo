/* 知牛 · 统一错误模型（前端所有 runCatching/try-catch 的落点）
 *
 * 硬约束 §2.4：禁止裸错误处理（不得只拿 e.message 塞 Text），所有错误走本 sealed class，
 * UI 层显示 "错误码徽章 + 分级中文文案"。
 *
 * 分级映射来源：
 * - 后端 SSE error.code（见 backend/app/main.py + gateway.py 结构化 reason）：
 *   invalid_key / rate_limited / timeout / upstream_5xx / no_key_configured / unknown
 * - 本地异常：多数归 UNKNOWN，网络类由平台 actual 判定（此处给出确定性兜底）。
 */
package com.zhiniu.domain.model

/** 统一错误：code 为稳定徽章码，message 为面向用户的分级中文文案。 */
sealed class AppError(val code: String, val message: String) {
    /** UI 展示串：错误码徽章 + 文案。 */
    fun display(): String = "[$code] $message"

    object NO_NETWORK : AppError("NO_NETWORK", "网络连接不可用，请检查网络后重试")
    object RATE_LIMIT : AppError("RATE_LIMIT", "请求过于频繁，已达限流，请稍后重试")
    object AUTH_INVALID : AppError("AUTH_INVALID", "认证失败（厂商 Key 无效或未配置），请在后台检查 DEEPSEEK/ZHIPUAI/HUNYUAN Key")
    object UPSTREAM_5XX : AppError("UPSTREAM_5XX", "上游服务异常，已自动降级，请稍后重试")
    object JSON_SCHEMA_FAILED : AppError("JSON_SCHEMA_FAILED", "数据格式解析失败，数据源可能已变更")
    object NIL : AppError("NIL", "")  // 无错误占位（非展示用）

    class Unknown(overrideDetails: String? = null) :
        AppError("UNKNOWN", overrideDetails?.takeIf { it.isNotBlank() } ?: "未知错误，请稍后重试")

    companion object {
        /** 后端 SSE error.code → AppError。 */
        fun fromBackendCode(code: String?): AppError = when (code) {
            "invalid_key" -> AUTH_INVALID
            "rate_limited" -> RATE_LIMIT
            "timeout" -> NO_NETWORK
            "upstream_5xx" -> UPSTREAM_5XX
            "no_key_configured" -> AUTH_INVALID
            "json_schema_failed" -> JSON_SCHEMA_FAILED
            else -> Unknown()
        }

        /** 任意异常 → AppError（已是指定 AppError 则原样透传）。 */
        fun fromThrowable(t: Throwable?): AppError = when (t) {
            is AppError -> t
            else -> Unknown(t?.message)
        }

        /** HTTP 状态码 → 统一错误（客户端请求层映射）。 */
        fun fromHttpStatus(status: Int): AppError = when (status) {
            in 401..403 -> AUTH_INVALID
            429 -> RATE_LIMIT
            in 500..599 -> UPSTREAM_5XX
            in 200..299 -> NIL
            else -> NO_NETWORK
        }
    }
}