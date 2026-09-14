#!/usr/bin/env bash
# 知牛 · 鸿蒙一键构建：shared(ohosArm64 so) → 回填 ohosApp → hvigor HAP
# 前置：DevEco Studio（含 SDK/hvigor/hdc）；产物：ohosApp/entry/build/.../entry-default-unsigned.hap
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

DEVECO="${DEVECO_ROOT:-/Applications/DevEco-Studio.app/Contents}"
if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
  TOOLCHAIN_JDK="$(ls -d "$ROOT"/.toolchains/jdk-17*/Contents/Home 2>/dev/null | head -1 || true)"
  if [ -n "$TOOLCHAIN_JDK" ]; then export JAVA_HOME="$TOOLCHAIN_JDK"; fi
fi
export PATH="$DEVECO/tools/hvigor/bin:$DEVECO/tools/node:$PATH"
export DEVECO_SDK_HOME="$DEVECO/sdk"

echo "==> [1/3] :shared:linkDebugSharedOhosArm64（Kotlin → libshared.so）"
(cd kuikly-shell && ./gradlew -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64)

echo "==> [2/3] 回填 so/api.h 到 ohosApp"
bash scripts/ohos-backfill.sh

echo "==> [3/3] hvigor assembleHap"
(cd kuikly-shell/ohosApp && hvigorw assembleHap --mode module -p product=default -p buildMode=debug --no-daemon)

HAP="kuikly-shell/ohosApp/entry/build/default/outputs/default/entry-default-unsigned.hap"
echo "✅ HAP: $ROOT/$HAP"
echo "   安装：hdc install -r $HAP && hdc shell aa start -a EntryAbility -b com.zhiniu.demo"
echo "   网关：模拟器内 10.0.2.2:8000（与 Android 同为 QEMU NAT，实测连通）"
