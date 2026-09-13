#!/bin/sh
# 知牛 · common 资产同步：shared/src/commonMain/assets/common → ohos rawfile
# 并生成 manifest.txt（相对路径清单），Index.ets 优先按清单拷贝，避免手维护列表漏文件。

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SOURCE_DIR="$SCRIPT_DIR/../shared/src/commonMain/assets/common"
TARGET_DIR="$SCRIPT_DIR/entry/src/main/resources/rawfile/common"

if [ ! -d "$SOURCE_DIR" ]; then
  echo "Missing common assets: $SOURCE_DIR" >&2
  exit 1
fi

mkdir -p "$TARGET_DIR"
cp -R "$SOURCE_DIR"/. "$TARGET_DIR"/

# 清单：所有文件（除旧 manifest）排序去重，便于 ETS 侧逐行拷贝
cd "$TARGET_DIR"
find . -type f ! -name manifest.txt | sed 's|^\./||' | sort > manifest.txt

echo "Synced common assets to $TARGET_DIR ($(wc -l < manifest.txt | tr -d ' ') files)"
