# 41注音輸入法

用**倚天41鍵盤排列**打注音的 iOS／iPadOS 與 Android 輸入法。鍵帽印英數鍵位，跟在電腦上用倚天41一樣看著英數打字。

- 選字引擎：[libchewing](https://codeberg.org/chewing/libchewing)（新酷音核心，`KB_ET` 排列）
- 不連網、不收集打字內容；iOS 不需要「允許完整取用」
- 打字中可點組字區任一字改字；⇧＋符號鍵打全形標點，「符」面板有常用標點

「倚天」是他人的註冊商標，本專案與其權利人無關，只用來說明支援的鍵盤排列。

## 從原始碼建置

需要：Xcode 27 以上、Rust（rustup）、Android Studio（NDK 28.2.13676358）。

```sh
git submodule update --init --recursive
scripts/build-dictionary.sh          # 詞庫 → build/dict/
scripts/build-libchewing-ios.sh      # iOS → build/ios/Chewing.xcframework
scripts/build-libchewing-android.sh  # Android → build/android/
```

**iOS**：開 `ios/EtenSmoke/EtenSmoke.xcodeproj`，在 App 與鍵盤兩個 target 把 Signing 的 Team 換成你自己的、Bundle ID 換成你自己的前綴，再建置。測試（`name=` 換成你裝了的 iPhone 模擬器；同名模擬器有好幾個 iOS 版本時，改用 `id=<simctl list 看到的 ID>`）：

```sh
cd ios/EtenSmoke && xcodebuild test -project EtenSmoke.xcodeproj -scheme EtenSmoke \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -derivedDataPath ../../build/DerivedData
```

**Android**：

```sh
cd android && ./gradlew assembleDebug
cd android && ./gradlew connectedDebugAndroidTest   # 需要開著模擬器或接實機
```

按鍵對照與來源比對見 [`docs/keymap-eten41.md`](docs/keymap-eten41.md)。

## 授權

- 本專案自己的程式碼：[MIT](LICENSE)
- libchewing、libchewing-data：GNU LGPL 2.1 或更新版本。App 以靜態連結使用 libchewing、未修改其原始碼；本 repo 即是替換 libchewing 後重新建置所需的完整原始碼。App 內「授權與原始碼」頁附授權全文與作者名單。
