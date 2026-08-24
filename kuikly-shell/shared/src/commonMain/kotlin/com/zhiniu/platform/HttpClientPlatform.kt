package com.zhiniu.platform

import io.ktor.client.HttpClient

/**
 * 平台 Ktor HttpClient（乘 B3 Web 前端调后端网关）。
 * js actual = 浏览器 Js engine；其他端由各自壳注入。
 */
expect fun createPlatformHttpClient(): HttpClient