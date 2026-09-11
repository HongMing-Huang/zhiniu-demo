# 知牛 · 三端（Android / iOS / HarmonyOS）就绪静态审查

> 2026-09-11 只读审查结论（未执行 gradle 原生构建、xcodebuild、pod install、hvigor，未启动任何模拟器——按用户要求等待确认后再做）。
> 版本基线：`buildSrc/.../KotlinBuildVar.kt` KUIKLY_VERSION=2.25.0、KOTLIN_VERSION=2.1.21 → Kuikly `2.25.0-2.1.21`；Ktor client 2.3.12。

## 0. 本轮已静态补齐的项

| 项 | 文件 | 说明 |
|---|---|---|
| Android `actual` ×4 | `shared/src/androidMain/kotlin/com/zhiniu/platform/HttpClientPlatform.android.kt`、`SystemTheme.android.kt` | OkHttp 引擎；`Resources.getSystem()` 读夜间模式；watch/applyHost 为 no-op |
| iOS `actual` ×4 | `shared/src/iosMain/kotlin/com/zhiniu/platform/HttpClientPlatform.ios.kt`、`SystemTheme.ios.kt` | Darwin 引擎；`UIScreen.mainScreen.traitCollection` 读外观 |
| Ktor 引擎依赖 | `shared/build.gradle.kts` androidMain / iosMain | `ktor-client-okhttp:2.3.12` / `ktor-client-darwin:2.3.12` |
| iOS 渲染层版本 | `iosApp/Podfile` | `OpenKuiklyIOSRender` tag 2.16.0 → **2.25.0**（与 core 对齐；GitHub 已确认存在该 tag） |

> 以上改动不影响 H5：`:shared:compileKotlinJs` 复验通过。原生端编译结果待用户确认后验证。

## 1. Android（`kuikly-shell/androidApp`）— 配置齐备，可尝试构建

- `applicationId com.zhiniu`，minSdk 23 / targetSdk 30 / compileSdk 34；`implementation(project(":shared"))`，render 由 shared androidMain `core-render-android:2.25.0-2.1.21` 传递（与 core 一致）。
- `AndroidManifest.xml`：`INTERNET` 权限 + `networkSecurityConfig`（`cleartextTrafficPermitted="true"`）→ 可访问 `http://127.0.0.1:8000` / 局域网网关。
- KR 适配器齐全并在 `KuiklyRenderActivity` 注册：Image / Font（JetBrains Mono）/ Log / Router / Thread / UncaughtException；KRBridgeModule / KRShareModule 已导出。
- **待办**：真机/模拟器上网关地址应指向电脑 IP（H5 用 `?gateway=`；原生壳可在 Router 参数里传同名 `gateway` 参数，`AppBasePage.created()` 已读取 `pageData.params.optString("gateway")`）。

## 2. iOS（`kuikly-shell/iosApp`）— 配置基本齐备，两项注意

- `Info.plist` `NSAllowsArbitraryLoads=true` → http 网关可访问。
- 壳代码齐：`AppDelegate.swift`、`KuiklyRenderViewController.m`、Handlers（Font/Router/ComponentExpand）、Modules（KRBridgeModule）、Bridging Header。
- `shared.podspec` vendored framework：pod install 前需 `./gradlew :shared:generateDummyFramework`；脚本阶段走 `:shared:syncFramework`。
- ⚠️ `iosApp/project.yml`（xcodegen）`dependencies: []`、`postBuildScripts: []`，而现有 `iosApp.xcodeproj` 里含手工 CocoaPods 集成（xcconfig + `[CP] Check Pods Manifest.lock`）——**重跑 xcodegen 会抹掉 Pods 集成**。建议要么在 project.yml 建模 Pods，要么不再重跑 xcodegen。
- ⚠️ Podfile 已升 2.25.0，需重新 `pod install` 并检查 `Podfile.lock`。

## 3. HarmonyOS（`kuikly-shell/ohosApp`）— 骨架在，阻塞最多（需先做架构调整）

- ETS 侧完整：`Index.ets`、`MyNativeManager.ets`（`Napi.initKuikly()`）、`KuiklyViewDelegate.ets`、`module.json5` 含 `ohos.permission.INTERNET`、`build-profile.json5` compatibleSdkVersion 5.0.0(12)。
- 独立构建入口：`settings.ohos.gradle.kts` + `shared/build.ohos.gradle.kts`（Kuikly `2.25.0-2.0.21-ohos`，Kotlin 2.0.21-KBA fork，与主构建 2.1.21 不同属正常）。
- **阻塞 ①（架构）**：Ktor 2.3.12 **没有 ohos 引擎**，且 `build.ohos.gradle.kts` 未声明 ktor / coroutines / serialization → commonMain 的 `GatewayMarketClient.kt`（含 SSE 流式）在 ohosArm64 下无法解析。
  - 方案 A（推荐）：把网络层抽成 `expect interface GatewayTransport`（common 只留 `get(url)` / `postStream(url, body, onLine)`），js/android/ios 用 Ktor 实现，ohosMain 经 napi 桥到 ETS `@ohos.net.http`（支持流式 `dataReceive` 事件）。
  - 方案 B：ohos 构建改用 Kuikly 官方 `KRBridgeModule` 调 ETS 发请求，返回 JSON 字符串给 Kotlin 解析。
- **阻塞 ②**：`ohosApp/entry/libs/arm64-v8a/libshared.so` 与 `cpp/thirdparty/biz_entry/libshared_api.h` 不存在（`CMakeLists.txt` 第 32 行引用）→ 需 ohos gradle 产物回填脚本。
- **阻塞 ③**：`entry/oh-package.json5` `@kuikly-open/render` 2.16.0 与 core 2.25.0 不匹配 → 升级到 2.25.0 系列。
- **阻塞 ④**：无 `ohosMain` actual（需与方案 A/B 一起补）；`ohpm install` 未执行、无 hvigorw wrapper（依赖 DevEco Studio 5.1+）。

## 4. expect / actual 覆盖表（本轮后）

| expect（commonMain） | jsMain | androidMain | iosMain | ohosMain |
|---|---|---|---|---|
| `createPlatformHttpClient()` | ✅ | ✅（新增） | ✅（新增） | ❌ |
| `systemPrefersDark()` | ✅ | ✅（新增） | ✅（新增） | ❌ |
| `watchSystemTheme()` | ✅ | ✅（no-op） | ✅（no-op） | ❌ |
| `applyHostTheme()` | ✅ | ✅（no-op） | ✅（no-op） | ❌ |

## 5. 建议的构建顺序（待用户确认后执行）

1. **Android**：`./gradlew :androidApp:installDebug`（隔离 JDK17，`env -u NODE_OPTIONS`）；首次需 Android SDK 34。
2. **iOS**：`./gradlew :shared:generateDummyFramework` → `cd iosApp && pod install` → Xcode 打开 `.xcworkspace`；模拟器需安装 iOS runtime。
3. **HarmonyOS**：先完成 §3 方案 A/B 的网络层抽象，再 `./gradlew -c settings.ohos.gradle.kts :shared:linkReleaseSharedOhosArm64` → 回填 so/头文件 → DevEco 构建。
