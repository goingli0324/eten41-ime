#!/bin/sh
# 把 vendor/libchewing 的 C API 交叉編譯成 iOS 靜態函式庫。
# 輸出：build/ios/<rust-target>/libchewing_capi.a，以及 build/ios/include/
# 用法：scripts/build-libchewing-ios.sh [rust-target ...]
# 預設 target：aarch64-apple-ios-sim aarch64-apple-ios
set -eu

ROOT=$(cd "$(dirname "$0")/.." && pwd)
LIBCHEWING="$ROOT/vendor/libchewing"
OUT="$ROOT/build/ios"
CARGO_TARGET_DIR="$ROOT/build/cargo"

[ -f "$HOME/.cargo/env" ] && . "$HOME/.cargo/env"

if [ $# -eq 0 ]; then
  set -- aarch64-apple-ios-sim aarch64-apple-ios
fi

for target in "$@"; do
  echo "== building chewing_capi for $target"
  # debuginfo 會讓 .a 膨脹到 50MB 以上，iOS 用不到，關掉
  CARGO_PROFILE_RELEASE_DEBUG=false \
    cargo build --release \
      --manifest-path "$LIBCHEWING/capi/Cargo.toml" \
      --target "$target" \
      --target-dir "$CARGO_TARGET_DIR"
  mkdir -p "$OUT/$target"
  cp "$CARGO_TARGET_DIR/$target/release/libchewing_capi.a" "$OUT/$target/"
done

rm -rf "$OUT/include"
mkdir -p "$OUT/include"
cp "$LIBCHEWING"/capi/include/*.h "$OUT/include/"
# 讓 Swift 能 import CChewing
cat > "$OUT/include/module.modulemap" <<'EOF'
module CChewing {
  header "chewing.h"
  export *
}
EOF

# 模擬器與實機兩個 slice 都在時，包成 xcframework 給 Xcode 自動挑
if [ -f "$OUT/aarch64-apple-ios-sim/libchewing_capi.a" ] && [ -f "$OUT/aarch64-apple-ios/libchewing_capi.a" ]; then
  rm -rf "$OUT/Chewing.xcframework"
  xcodebuild -create-xcframework \
    -library "$OUT/aarch64-apple-ios-sim/libchewing_capi.a" -headers "$OUT/include" \
    -library "$OUT/aarch64-apple-ios/libchewing_capi.a" -headers "$OUT/include" \
    -output "$OUT/Chewing.xcframework"
fi
echo "== done: $OUT"
