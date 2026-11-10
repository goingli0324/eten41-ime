#!/bin/sh
# 把 vendor/libchewing/data（libchewing-data）的 csv 轉成 libchewing 執行期要讀的 .dat。
# 做法照 libchewing-data 的 CMakeLists：chewing-cli init --csv <src>.csv <dst>.dat，
# swkb.dat / symbols.dat 原樣複製。
# 輸出：build/dict/
set -eu

ROOT=$(cd "$(dirname "$0")/.." && pwd)
LIBCHEWING="$ROOT/vendor/libchewing"
DATA="$LIBCHEWING/data"
OUT="$ROOT/build/dict"
CARGO_TARGET_DIR="$ROOT/build/cargo"

[ -f "$HOME/.cargo/env" ] && . "$HOME/.cargo/env"

if [ ! -f "$DATA/dict/chewing/word.csv" ]; then
  echo "找不到 $DATA/dict/chewing/word.csv，先跑：git submodule update --init --recursive" >&2
  exit 1
fi

echo "== building chewing-cli (host)"
cargo build --release \
  --manifest-path "$LIBCHEWING/tools/Cargo.toml" \
  --target-dir "$CARGO_TARGET_DIR"
CLI="$CARGO_TARGET_DIR/release/chewing-cli"

mkdir -p "$OUT"
for name in word tsi alt mini; do
  echo "== $name.csv -> $name.dat"
  "$CLI" init --csv "$DATA/dict/chewing/$name.csv" "$OUT/$name.dat"
done
cp "$DATA/misc/swkb.dat" "$DATA/misc/symbols.dat" "$OUT/"
ls -la "$OUT"
