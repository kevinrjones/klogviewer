#!/usr/bin/env bash

# Required tools:
# - macOS: `sips`, `iconutil`
# - Cross-platform conversion: ImageMagick (`magick`)
#
# This script generates desktop icon assets from:
#   app/src/main/resources/icons/klogviewer-1024.png

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ICON_DIR="$ROOT_DIR/app/src/main/resources/icons"
MASTER_PNG="$ICON_DIR/klogviewer-1024.png"
LINUX_PNG="$ICON_DIR/klogviewer.png"
WINDOWS_ICO="$ICON_DIR/klogviewer.ico"
MAC_ICNS="$ICON_DIR/klogviewer.icns"

fail() {
  echo "Error: $1" >&2
  exit 1
}

require_tool() {
  command -v "$1" >/dev/null 2>&1 || fail "Required tool '$1' is not installed or not on PATH"
}

[[ -f "$MASTER_PNG" ]] || fail "Master icon not found: $MASTER_PNG"

require_tool sips
require_tool iconutil
require_tool magick

mkdir -p "$ICON_DIR"

echo "Generating Linux PNG..."
cp "$MASTER_PNG" "$LINUX_PNG"

echo "Generating Windows ICO (multi-size)..."
magick "$MASTER_PNG" \
  \( -clone 0 -resize 16x16 \) \
  \( -clone 0 -resize 24x24 \) \
  \( -clone 0 -resize 32x32 \) \
  \( -clone 0 -resize 48x48 \) \
  \( -clone 0 -resize 64x64 \) \
  \( -clone 0 -resize 128x128 \) \
  \( -clone 0 -resize 256x256 \) \
  -delete 0 "$WINDOWS_ICO"

echo "Generating macOS ICNS..."
ICONSET_DIR="$ROOT_DIR/.tmp-klogviewer.iconset"
rm -rf "$ICONSET_DIR"
mkdir -p "$ICONSET_DIR"

sips -z 16 16 "$MASTER_PNG" --out "$ICONSET_DIR/icon_16x16.png" >/dev/null
sips -z 32 32 "$MASTER_PNG" --out "$ICONSET_DIR/icon_16x16@2x.png" >/dev/null
sips -z 32 32 "$MASTER_PNG" --out "$ICONSET_DIR/icon_32x32.png" >/dev/null
sips -z 64 64 "$MASTER_PNG" --out "$ICONSET_DIR/icon_32x32@2x.png" >/dev/null
sips -z 128 128 "$MASTER_PNG" --out "$ICONSET_DIR/icon_128x128.png" >/dev/null
sips -z 256 256 "$MASTER_PNG" --out "$ICONSET_DIR/icon_128x128@2x.png" >/dev/null
sips -z 256 256 "$MASTER_PNG" --out "$ICONSET_DIR/icon_256x256.png" >/dev/null
sips -z 512 512 "$MASTER_PNG" --out "$ICONSET_DIR/icon_256x256@2x.png" >/dev/null
sips -z 512 512 "$MASTER_PNG" --out "$ICONSET_DIR/icon_512x512.png" >/dev/null
sips -z 1024 1024 "$MASTER_PNG" --out "$ICONSET_DIR/icon_512x512@2x.png" >/dev/null

iconutil -c icns "$ICONSET_DIR" -o "$MAC_ICNS"
rm -rf "$ICONSET_DIR"

echo "Generated:"
echo "- $LINUX_PNG"
echo "- $WINDOWS_ICO"
echo "- $MAC_ICNS"
