#!/bin/sh

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

echo "Synced common assets to $TARGET_DIR"
