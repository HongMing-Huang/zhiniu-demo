#!/usr/bin/env bash
# 知牛 · 构建并部署 H5 bundle + 本地图标资源
# 产物：kuikly-shell/shared/build/kotlin-webpack/js/productionExecutable/nativevue2.js → web-host（同源主入口）+ web-8083
#       kuikly-shell/shared/src/commonMain/assets/common/icons/ → web-host/assets/common/icons/
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

export JAVA_HOME="${JAVA_HOME:-/Users/c14h14n3/Desktop/demo/zhiniu/.toolchains/jdk-17.0.20.1+1/Contents/Home}"

echo "==> [1/3] 静态资源（图标 + 字体，同步到 web-host）"
SRC_ICONS="$ROOT/kuikly-shell/shared/src/commonMain/assets/common/icons"
DST_ICONS="$ROOT/web-host/assets/common/icons"
if [ -d "$SRC_ICONS" ]; then
  mkdir -p "$DST_ICONS"
  rsync -a --delete "$SRC_ICONS/" "$DST_ICONS/"
  echo "  图标数量: $(ls $DST_ICONS | wc -l | tr -d ' ')"
else
  echo "  ⚠ 缺失 $SRC_ICONS（先跑 scripts/icons_build.sh + scripts/icons_render.js）"
fi
SRC_BRAND="$ROOT/kuikly-shell/shared/src/commonMain/assets/common/brand"
DST_BRAND="$ROOT/web-host/assets/common/brand"
if [ -d "$SRC_BRAND" ]; then
  mkdir -p "$DST_BRAND"
  rsync -a --delete "$SRC_BRAND/" "$DST_BRAND/"
  echo "  品牌资源: $(ls $DST_BRAND | wc -l | tr -d ' ') 个"
fi
SRC_FONTS="$ROOT/kuikly-shell/shared/src/commonMain/assets/common/fonts"
DST_FONTS="$ROOT/web-host/assets/common/fonts"
if [ -d "$SRC_FONTS" ]; then
  mkdir -p "$DST_FONTS"
  rsync -a --delete "$SRC_FONTS/" "$DST_FONTS/"
  echo "  字体数量: $(ls $DST_FONTS | wc -l | tr -d ' ')"
else
  echo "  ⚠ 缺失 $SRC_FONTS（等宽数字字体，见 docs/typography.md）"
fi

echo "==> [2/3] jsBrowserProductionWebpack"
(cd "$ROOT/kuikly-shell" && env -u NODE_OPTIONS ./gradlew :shared:jsBrowserProductionWebpack 2>&1 | tail -3)

echo "==> [3/3] 部署 nativevue2.js → web-host/（同源，主入口）+ web-8083/（兼容旧链）"
cp "$ROOT/kuikly-shell/shared/build/kotlin-webpack/js/productionExecutable/nativevue2.js" "$ROOT/web-host/nativevue2.js"
cp "$ROOT/kuikly-shell/shared/build/kotlin-webpack/js/productionExecutable/nativevue2.js" "$ROOT/web-8083/nativevue2.js"
cp "$ROOT/kuikly-shell/shared/build/kotlin-webpack/js/productionExecutable/nativevue2.js.map" "$ROOT/web-8083/nativevue2.js.map" 2>/dev/null || true
# 缓存穿透：每次部署更新 index.html 的 bundle 版本号（?v=时间戳），浏览器强制拉新
BUMP="$(date +%s)"
sed -i '' -E "s|nativevue2\.js\?v=[0-9]+|nativevue2.js?v=$BUMP|" "$ROOT/web-host/index.html"
ls -la "$ROOT/web-host/nativevue2.js"
echo "✅ 构建部署完成（bundle 版本 v=${BUMP}；单端入口 http://127.0.0.1:8082/index.html?page_name=MarketList&use_spa=1）"
