#!/usr/bin/env bash
# 知牛 · TDesign Icons 下载 + PNG 化（线性轮廓，黑/白双色）
# 来源：Tencent/tdesign-icons (develop 分支) svg/ 目录，文件名均已核对存在。
set -e
BR="develop"
BASE="https://raw.githubusercontent.com/Tencent/tdesign-icons/$BR/svg"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/kuikly-shell/shared/src/commonMain/assets/common/icons"
WEB="$ROOT/web-host/assets/common/icons"
mkdir -p "$SRC" "$WEB"

# 业务名|官方文件名
ICONS="
search|ai-search
back|arrow-left
chevron_down|chevron-down
chevron_up|chevron-up
chevron_right|chevron-right
star|collection
star_filled|collection-filled
filter|filter
theme|brightness
close|close
send|arrow-up
ai|ai-1
more|ellipsis
chart|chart-line
calendar|calendar
check|check
filter-sort|filter-sort
error-triangle|error-triangle
chat-message|chat-message
data-display|data-display
user-circle|user-circle
"

echo "==> 下载 + 预处理（线性轮廓）"
rm -rf /tmp/tdesign_svg && mkdir -p /tmp/tdesign_svg
while IFS='|' read -r name file; do
  [ -z "$name" ] && continue
  curl -s "$BASE/$file.svg" -o "/tmp/tdesign_svg/$name.svg"
  # 线性化：填充白 → 无填充；描边黑 → 主题黑（渲染后 tint 染色）
  sed -i '' 's/fill="white"/fill="none"/g; s/stroke="black"/stroke="#171A1F"/g; s/fill="#000000"/fill="none"/g' "/tmp/tdesign_svg/$name.svg"
  echo "  $name <= $file.svg"
done <<< "$ICONS"
echo "完成"
