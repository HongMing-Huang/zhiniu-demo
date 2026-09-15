# 知牛 ZhiNiu · 快速开始

> 版本：v1.1（2026-09-11，与工程实态对齐）。主演示端为 **Web H5**（零 Android 依赖），原生端为 Kuikly 官方壳工程。

---

## 0. 一键构建（推荐入口）

| 端 | 一键命令 | 产物 |
|:--|:--|:--|
| Android | `bash scripts/build-android.sh` | `kuikly-shell/androidApp/build/outputs/apk/debug/androidApp-debug.apk` |
| iOS（模拟器） | `bash scripts/build-ios.sh` | DerivedData `iosApp.app`（命令尾部给出 simctl 运行命令） |
| 鸿蒙 | `bash scripts/build-ohos.sh` | `kuikly-shell/ohosApp/entry/build/.../entry-default-unsigned.hap` |
| H5 | `bash scripts/build.sh` | `web-host/`（`python3 -m http.server 8082` 起服） |

> iOS / 鸿蒙脚本内部仍需 Xcode / DevEco 工具链（见下文各端小节）；脚本只负责串起
> Kotlin 编译 → 回填 → 原构建 三步。演示视频见 `docs/video/demo.mp4`（README 📺 演示视频区）。

## 0. 环境要求

| 工具 | 版本 | 用途 |
|:--|:--|:--|
| JDK | **17**（Gradle 构建必需；系统默认 JDK 25 太新会失败） | 前端构建 |
| Python | 3.10+ | 后端数据 / LLM 网关 |
| Xcode Command Line Tools / Android Studio / DevEco 5.1+ | — | 仅对应原生端需要 |

> 本仓库自带隔离工具链：`.toolchains/jdk-17.0.20.1+1`。`scripts/build.sh` 会自动使用；
> 手动跑 gradle 时请 `JAVA_HOME=$(pwd)/.toolchains/jdk-17.0.20.1+1/Contents/Home`。
> 另需 `env -u NODE_OPTIONS`（本机 NODE_OPTIONS 的 `--use-system-ca` 会被 Kotlin 内置 Node 拒绝）。

---

## 1. 后端网关（建议先跑）

```bash
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env      # 可选：填 OpenAI 兼容厂商 Key；缺 Key 自动「规则降级」
uvicorn app.main:app --port 8000
```

自检：

```bash
curl http://127.0.0.1:8000/healthz
# {"status":"ok","services":{"market":{"status":"ready","provider":"sina+eastmoney"},"agent":{...}}}
```

单测：`.venv/bin/python -m unittest discover -s tests`（当前 53/53 通过）。

不启动后端也可运行前端——行情/资讯保留内置确定性快照，研究回复标注「规则降级」。

---

## 2. 前端 H5（主演示端）

```bash
./scripts/build.sh                       # 图标+字体同步 → jsBrowserProductionWebpack → 部署 web-host/
(cd web-host && python3 -m http.server 8082 --bind 127.0.0.1)
```

打开：<http://127.0.0.1:8082/index.html?page_name=MarketList&use_spa=1>

- 路由：`page_name=MarketList / StockDetail（配 symbol=）/ AiResearch`
- 注意：`index.html` 的启动链是「nativevue2.js → 字体就绪 → h5App.js」；
  H5 自定义字体须在加载完成后再拉起渲染宿主（官方 h5-custom-font 指引），否则等宽数字会显示不全。
- 四分辨率 + 深/浅色为固定验收口径：1280×800 / 1440×900 / 1920×1080。

---

## 3. Android

```bash
cd kuikly-shell
env -u NODE_OPTIONS JAVA_HOME=<JDK17路径> ./gradlew :androidApp:installDebug
```

- 壳工程为 Kuikly 官方结构：`KuiklyRenderActivity` + 全套官方适配器
  （Image / **Font** / Router / Thread / Log / UncaughtException）。
- 网关地址默认 `http://127.0.0.1:8000`，真机联调在 H5 可用 `?gateway=` 参数覆盖；
  原生端访问电脑网关请用局域网 IP。

## 4. iOS

```bash
cd kuikly-shell/iosApp
pod install --repo-update
open iosApp.xcworkspace      # Xcode 选 iosApp target 直接 Run
```

- 共享 framework 经 CocoaPods（`shared/build.gradle.kts` cocoapods 配置）；
- 自定义字体走官方 `KRFontHandler`（`+load` 自动注册），字体文件随 shared 资源打包。

## 5. 鸿蒙

- DevEco Studio 5.1+ 打开 `kuikly-shell`（使用 `settings.ohos.gradle.kts` / `build.ohos.gradle.kts`），
  构建 `ohosApp`；字体当前为系统降级态。

---

## 6. 部署（可选）

- 后端：Render Blueprint（仓库根 `render.yaml`）或任意 PaaS；`DATABASE_URL` 指向 Neon 时启用 Postgres 持久化（研究缓存 + request_id 幂等），不设置则纯内存。
- H5：`web-host/` 纯静态，托管后用 `?gateway=https://网关域名` 指向后端。

---

## 7. 三种数据模式

| 场景 | 条件 | 效果 |
|:--|:--|:--|
| 完整真数据 | 后端在跑 + LLM Key 已配 | 真实行情/资讯 + 多 Agent LLM 辩论归纳 |
| 仅行情真 | 后端在跑、无 Key | 行情/资讯真实；研究回复标注「规则降级 · 未伪装模型」 |
| 完全离线 | 不启动后端 | 内置确定性 Mock，全部功能可演示 |

---

## 8. 常见问题

- **Gradle 编译失败**：确认 JDK 17 + `env -u NODE_OPTIONS`（见 §0）。
- **H5 白屏 / 乱码**：`index.html` 启动链三步缺一不可；改过部署链路必须重跑 `scripts/build.sh`。
- **等宽数字变成系统字体**：字体 404 时 1.5s 后兜底启动；检查 `web-host/assets/common/fonts/` 是否存在（build.sh 自动同步）。
- **行情乱码**：新浪接口为 GBK 编码，解码在后端统一处理，前端不要直连。
- **`:shared:jsNodeTest` 内部编译器错误**：`IrSimpleFunctionSymbolImpl is already bound`，为当前 Kuikly/Kotlin 工具链已知问题（clean 后仍复现），与业务代码无关。
- **Android 构建依赖下载报 TLS `protocol_version`**：本机 SOCKS 代理注入 Gradle JVM 所致，绕过代理变量构建：`env -u http_proxy -u https_proxy -u all_proxy -u NODE_OPTIONS JAVA_HOME=<JDK17> ./gradlew :androidApp:assembleDebug`。

详见 [docs/api-matrix.md](docs/api-matrix.md)（字段/端点/降级链）与 [docs/tech-stack.md](docs/tech-stack.md)（选型）。
