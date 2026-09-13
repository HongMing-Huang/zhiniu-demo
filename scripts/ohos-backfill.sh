#!/usr/bin/env bash
# 知牛 · ohos 构建产物回填：shared ohosArm64 .so/.h → ohosApp 工程
# 用法：先 ./gradlew -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64，再 bash scripts/ohos-backfill.sh
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

SRC_BIN="$ROOT/kuikly-shell/shared/build/bin/ohosArm64/sharedDebugShared"
SO_DST="$ROOT/kuikly-shell/ohosApp/entry/libs/arm64-v8a"
HDR_DST="$ROOT/kuikly-shell/ohosApp/entry/src/main/cpp/thirdparty/biz_entry"

if [ ! -f "$SRC_BIN/libshared.so" ]; then
  echo "❌ 缺 $SRC_BIN/libshared.so（先跑 ohos gradle link 任务）"
  exit 1
fi

mkdir -p "$SO_DST" "$HDR_DST"
cp "$SRC_BIN/libshared.so" "$SO_DST/libshared.so"
cp "$SRC_BIN/libshared_api.h" "$HDR_DST/libshared_api.h"
echo "✅ 回填完成：libshared.so → entry/libs/arm64-v8a；libshared_api.h → cpp/thirdparty/biz_entry"
echo "   下一步（需 DevEco Studio 5.1+）：打开 ohosApp → ohpm install → hvigor 构建"
