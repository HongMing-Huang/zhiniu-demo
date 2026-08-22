/* 知牛 · AppError 纯逻辑单元测试（commonTest，无 Kuikly 依赖）
 * 覆盖：后端 error.code → AppError 映射 / 异常兜底 / display 中文文案含错误码徽章。
 */
package com.zhiniu

import com.zhiniu.domain.model.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppErrorTest {

    @Test fun `backend code maps to rate limit`() {
        val err = AppError.fromBackendCode("rate_limited")
        assertEquals("RATE_LIMIT", err.code)
        assertTrue(err.message.contains("限流"))
    }

    @Test fun `backend invalid_key maps to auth`() {
        val err = AppError.fromBackendCode("invalid_key")
        assertEquals("AUTH_INVALID", err.code)
        assertTrue(err.message.contains("Key"))
    }

    @Test fun `backend no_key maps to auth`() {
        val err = AppError.fromBackendCode("no_key_configured")
        assertEquals("AUTH_INVALID", err.code)
    }

    @Test fun `backend upstream_5xx maps to upstream`() {
        val err = AppError.fromBackendCode("upstream_5xx")
        assertEquals("UPSTREAM_5XX", err.code)
        assertTrue(err.message.contains("上游服务异常"))
    }

    @Test fun `backend timeout maps to no network`() {
        val err = AppError.fromBackendCode("timeout")
        assertEquals("NO_NETWORK", err.code)
    }

    @Test fun `unknown backend code maps to UNKNOWN`() {
        val err = AppError.fromBackendCode("no_such_code")
        assertEquals("UNKNOWN", err.code)
    }

    @Test fun `fromThrowable passes through AppError`() {
        val err = AppError.fromThrowable(AppError.NO_NETWORK)
        assertEquals(AppError.NO_NETWORK, err)
    }

    @Test fun `fromThrowable maps plain exception to UNKNOWN with details`() {
        val err = AppError.fromThrowable(RuntimeException("boom"))
        assertEquals("UNKNOWN", err.code)
        assertTrue(err.message.contains("boom"))
    }

    @Test fun `display embeds badge and chinese text`() {
        assertEquals("[AUTH_INVALID] 认证失败（厂商 Key 无效或未配置），请在后台检查 DEEPSEEK/ZHIPUAI/HUNYUAN Key", AppError.AUTH_INVALID.display())
    }
}