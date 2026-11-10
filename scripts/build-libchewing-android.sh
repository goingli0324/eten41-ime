#!/bin/sh
# 用 Android NDK 把 libchewing 的 C API 編成靜態庫，再和 JNI 橋接（android/jni/chewing_jni.c）
# 連成 Android App 要載入的 libeten41jni.so；同時把詞庫放到 App 要打包的 assets/dict/。
# 輸出：build/android/jniLibs/arm64-v8a/libeten41jni.so、build/android/assets/dict/*.dat
# 前提：scripts/build-dictionary.sh 已產生 build/dict/
set -eu

ROOT=$(cd "$(dirname "$0")/.." && pwd)
LIBCHEWING="$ROOT/vendor/libchewing"
OUT="$ROOT/build/android"
CARGO_TARGET_DIR="$ROOT/build/cargo"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
# 與 AGP 9.5 預設 NDK 相同（developer.android.com AGP 9.5.0 release notes）
NDK="${ANDROID_NDK_HOME:-$SDK/ndk/28.2.13676358}"
API=26
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin"

[ -f "$HOME/.cargo/env" ] && . "$HOME/.cargo/env"
[ -d "$TOOLCHAIN" ] || { echo "找不到 NDK：$NDK" >&2; exit 1; }
[ -f "$ROOT/build/dict/word.dat" ] || { echo "先跑 scripts/build-dictionary.sh" >&2; exit 1; }

# Rust target → Android ABI（arm64 同時涵蓋實機與 Apple Silicon 上的模擬器）
TARGET=aarch64-linux-android
ABI=arm64-v8a
CLANG="$TOOLCHAIN/${TARGET}${API}-clang"

echo "== building chewing_capi for $TARGET"
CC_aarch64_linux_android="$CLANG" \
AR_aarch64_linux_android="$TOOLCHAIN/llvm-ar" \
CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$CLANG" \
CARGO_PROFILE_RELEASE_DEBUG=false \
  cargo build --release \
    --manifest-path "$LIBCHEWING/capi/Cargo.toml" \
    --target "$TARGET" \
    --target-dir "$CARGO_TARGET_DIR"

echo "== linking libeten41jni.so ($ABI)"
mkdir -p "$OUT/jniLibs/$ABI"
"$CLANG" -shared -fPIC -O2 \
  -I "$LIBCHEWING/capi/include" \
  "$ROOT/android/jni/chewing_jni.c" \
  "$CARGO_TARGET_DIR/$TARGET/release/libchewing_capi.a" \
  -Wl,--gc-sections -Wl,-z,max-page-size=16384 \
  -llog -ldl -lm \
  -o "$OUT/jniLibs/$ABI/libeten41jni.so"

echo "== copying dictionary into assets/dict"
mkdir -p "$OUT/assets/dict" "$OUT/assets/licenses"
cp "$ROOT"/build/dict/*.dat "$OUT/assets/dict/"
cp "$LIBCHEWING/COPYING" "$OUT/assets/licenses/libchewing-COPYING.txt"
cp "$LIBCHEWING/AUTHORS" "$OUT/assets/licenses/libchewing-AUTHORS.txt"
ls -la "$OUT/jniLibs/$ABI"
