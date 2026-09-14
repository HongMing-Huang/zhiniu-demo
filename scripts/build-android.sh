#!/usr/bin/env bash
# 知牛 · Android 一键构建：shared(Android) + androidApp → debug APK
# 产物：kuikly-shell/androidApp/build/outputs/apk/debug/androidApp-debug.apk
# 依赖：JDK 17（JAVA_HOME 指向 .toolchains/jdk-17*）、Android SDK（ANDROID_HOME）
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
  TOOLCHAIN_JDK="$(ls -d "$ROOT"/.toolchains/jdk-17*/Contents/Home 2>/dev/null | head -1 || true)"
  if [ -n "$TOOLCHAIN_JDK" ]; then export JAVA_HOME="$TOOLCHAIN_JDK"; fi
fi

echo "==> [1/1] :androidApp:assembleDebug"
(cd kuikly-shell && ./gradlew :androidApp:assembleDebug)

APK="kuikly-shell/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
echo "✅ APK: $ROOT/$APK"
echo "   安装：adb install -r ${APK}（模拟器内网关默认 10.0.2.2:8000）"
