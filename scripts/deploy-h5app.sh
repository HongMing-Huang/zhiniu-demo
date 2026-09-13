#!/usr/bin/env bash
# 知牛 · 上游 h5App 构建产物部署：upstream-kuiklyui/h5App → web-host / web-8083
# 前置：cd upstream-kuiklyui && ./gradlew :h5App:jsBrowserDevelopmentWebpack
# 说明：产物已含两处知牛源码补丁（Main.kt 打开 autoUpdateRootViewSizeOnResize、
#       KRRouterModule openPage 用 location.assign 整页跳转），无需再对产物打补丁。
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

SRC="$ROOT/upstream-kuiklyui/h5App/build/kotlin-webpack/js/developmentExecutable/h5App.js"
if [ ! -f "$SRC" ]; then
  echo "❌ 缺 $SRC（先构建 :h5App:jsBrowserDevelopmentWebpack）"
  exit 1
fi

cp "$SRC" "$ROOT/web-host/h5App.js"
cp "$SRC" "$ROOT/web-8083/h5App.js"
echo "✅ h5App.js 已部署到 web-host/ 与 web-8083/（$(wc -c < "$SRC" | tr -d ' ') bytes）"
