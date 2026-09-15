#!/usr/bin/env bash
# 知牛 · 打包 H5 离线分发包：web-host 运行文件 → zhiniu-h5-web.zip（评审免编译体验）
# 用法：bash scripts/pack-h5.sh
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

STAGE="$(mktemp -d)/zhiniu-h5"
mkdir -p "$STAGE"

# H5 运行所需文件（不含源码目录与演示视频副本）
for f in index.html demo.html demo.mp4 nativevue2.js nativevue2.js.map h5App.js; do
  [ -f "web-host/$f" ] && cp "web-host/$f" "$STAGE/"
done
[ -d web-host/assets ] && cp -R web-host/assets "$STAGE/assets"

ZIP="$ROOT/zhiniu-h5-web.zip"
rm -f "$ZIP"
(cd "$STAGE" && zip -qr "$ZIP" .)

echo "✅ H5 包：${ZIP}（$(wc -c < "${ZIP}" | tr -d ' ') bytes）"
echo "   使用：解压后 python3 -m http.server 8082，浏览器打开 index.html?page_name=MarketList&use_spa=1"
