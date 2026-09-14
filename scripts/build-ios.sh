#!/usr/bin/env bash
# 知牛 · iOS 一键构建：shared(iOS) CocoaPods 框架 + iosApp → iOS Simulator App
# 前置：macOS + Xcode + CocoaPods（首次需 cd kuikly-shell/iosApp && pod install）
# 产物：DerivedData .../Debug-iphonesimulator/iosApp.app
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
  TOOLCHAIN_JDK="$(ls -d "$ROOT"/.toolchains/jdk-17*/Contents/Home 2>/dev/null | head -1 || true)"
  if [ -n "$TOOLCHAIN_JDK" ]; then export JAVA_HOME="$TOOLCHAIN_JDK"; fi
fi

echo "==> [1/2] :shared:compileKotlinIosSimulatorArm64（同步业务框架）"
(cd kuikly-shell && ./gradlew :shared:compileKotlinIosSimulatorArm64)

echo "==> [2/2] xcodebuild（iOS Simulator Debug）"
(cd kuikly-shell/iosApp && xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -configuration Debug -destination 'generic/platform=iOS Simulator' -quiet build)

APP="$(find ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphonesimulator -maxdepth 1 -name 'iosApp.app' 2>/dev/null | head -1)"
echo "✅ App: $APP"
echo "   运行：xcrun simctl boot 'iPhone 17 Pro' && xcrun simctl install booted '$APP' && xcrun simctl launch booted com.zhiniu"
echo "   注意：iOS 网关默认 127.0.0.1:8000（模拟器与宿主共用 loopback）；请先起后端"
