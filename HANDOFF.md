# HANDOFF · eten41-ime

主寫手：Claude Code。其他 Agent 只接邊界清楚的單，主程式不改。

## 1. 目前工作進度摘要

- **目前工作進度摘要**: 點組字區改字完成並經 iPad 實機驗收；程式碼改動待使用者決定 commit
- **當前 Git 分支**: `main` (`e5c3933`)
- **最後執行 Agent**: Claude Code
- **最後更新時間**: 2026-11-09 16:37:47

目標：倚天41 注音輸入法，iOS＋Android，先自用。引擎用 libchewing（LGPL-2.1-or-later），不自己寫選字引擎。

已定案：
- 排列是倚天41（libchewing `KB_ET`），按鍵表見 `docs/keymap-eten41.md`
- libchewing 釘 v0.13.1（Codeberg 主站），放在 `vendor/libchewing` submodule

第一片已完成（2026-11-09）：iOS 模擬器上，引擎加詞庫用 KB_ET 能打出正確的字。
- `ios/EtenSmoke`：最小 SwiftUI App、XCTest 5 項、XCUITest 1 項（用 iOS 鍵盤把 `dx- ce1` 打進試打欄，輸出「東西」），全部通過；截圖在 `docs/smoke-typing-dx-ce1.png`
- 37 個注音鍵逐鍵比對定案表、5 個聲調鍵的讀音、4 組詞（中文／注音／倚天／輸入法）

第二片已完成（2026-11-09）：iOS 鍵盤擴充 `Eten41Keyboard`，模擬器上能在別的輸入框用倚天41打字。
- 版面：實體鍵盤 4 排（使用者定案）；鍵帽只印英數（中文模式字母大寫），不印注音——倚天41使用者看英數鍵位打字（2026-11-09 使用者更正），上方組字區＋候選字列，功能列有中／英、空白（＝一聲）、換行、刪除
- 程式：`Shared/`（引擎、版面、KeyboardSession、KeyboardView）給 App 與擴充共用；`Eten41Keyboard/` 只有入口
- 測試 14 項全綠：XCTest 12（含版面對引擎、選字、退格、中英切換、記憶體）＋ XCUITest 2（含端到端：設定加入鍵盤→切換→點鍵打出「東西」）
- 記憶體：模擬器量引擎＋詞庫增量約 4.7 MB（鍵盤擴充上限社群資料約 48 MB，未在實機量）
- 截圖：`docs/keyboard-extension-typing.png`

## 2. 環境與指令

- Xcode 27.2、Rust 1.99.1（rustup，目標 aarch64-apple-ios-sim、aarch64-apple-ios）
- 從零重建：
  1. `git submodule update --init --recursive`
  2. `scripts/build-libchewing-ios.sh`：產出 `build/ios/Chewing.xcframework`
  3. `scripts/build-dictionary.sh`：產出 `build/dict/*.dat`（約 4.9 MB）
  4. `cd ios/EtenSmoke && xcodebuild test -project EtenSmoke.xcodeproj -scheme EtenSmoke -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -derivedDataPath ../../build/DerivedData`
- `project.pbxproj` 是手寫的（objectVersion 77，同步資料夾），新增 Swift 檔放進 `EtenSmoke/` 或 `EtenSmokeTests/` 就會自動編進去

## 3. 已知的坑

- libchewing 預設 Esc 只關候選窗、不清組字區，要 `chewing_set_escCleanAllBuf(ctx, 1)`
- `chewing.h` 說 `chewing_phone_to_bopomofo` 成功回 0，v0.13.1 實作其實回「位元組數＋1」，失敗回 -1
- 詞庫裡「嗎」在 ㄇㄚ（一聲）的頻率最高，所以 ㄇㄚ 和 ㄇㄚ˙ 的第一候選都是「嗎」；測聲調鍵要看讀音，不能看候選字
- 鍵盤 Info.plist 的 `IsASCIICapable` 必須是 true：false 時 iOS 在 `.asciiCapable` 欄位會載入後 0.3 秒就拆掉鍵盤、退回系統鍵盤，log 只看得到宿主端 tearing down，沒有當機紀錄
- XCUITest 找地球鍵要用 `app.buttons`（label「下一個鍵盤」），它不在 `app.keyboards` 底下
- 模擬器不吃 Mac 貼上的文字；要驗試打欄就跑 XCUITest（`typeText`），不要手動

## 4. 風險與待決

- GuilelessBopomofo 是 GPL-3.0，只參考不抄。
- 鍵盤擴充的記憶體上限 Apple 沒公開，實機要再量一次。
- 開源上架：本專案 MIT；libchewing LGPL 靜態連結的義務由授權頁＋公開原始碼滿足。

## 下一棒

2026-11-09 已裝到使用者的 iPad 實機（已開開發者模式、已登記到開發者 Team）。專案改成 iPhone＋iPad（TARGETED_DEVICE_FAMILY "1,2"）。
- 實機安裝：`xcodebuild build ... -destination 'id=<UDID>' -allowProvisioningUpdates -allowProvisioningDeviceRegistration` 後 `xcrun devicectl device install app --device <UDID> build/DerivedData/Build/Products/Debug-iphoneos/EtenSmoke.app`
- 坑：新裝置剛登記時描述檔會重產，建置可能引用舊檔而失敗；刪掉 Debug-iphoneos 的 Products 與 Intermediates 再建即可

標點已完成（2026-11-09）：⇧ 單次（中文模式非字母鍵送 Shift 後 ASCII，引擎轉全形，如 ⇧, →，；英文模式大寫與半形符號）＋「符」面板 21 個常用全形標點（… 與 — 送兩個；選完自動回注音鍵，有組字先上字）。測試 21 項全綠（XCTest 19＋XCUITest 2，端到端打出「東西，、」）；已重裝到 iPad。截圖：`docs/keyboard-shift.png`、`docs/keyboard-symbols.png`。

2026-11-09 使用者在 iPad Pro 13 吋實機驗收通過（備忘錄裡切倚天41打字、⇧ 標點、符號面板皆正常）；iPad 實機單元測試 19 項全綠，引擎＋詞庫記憶體增量 4.6 MB。

點組字區改字已完成（2026-11-09，對照使用者電腦上的微軟注音倚天排列）：點組字區任一字＝游標移過去按 ↓，候選列換成「從這個字開始的詞＋單字」（phraseChoiceRearward=0）；平常打字時候選是「結束在最後的詞」（=1）；選完游標回到最後。再點同一字取消；還在打注音時不能點。截圖 `docs/keyboard-focus-candidates.png`。測試 25 項全綠（XCTest 23＋XCUITest 2，端到端把「東西」的東改成冬）。
- 2026-11-09 使用者在 iPad 實機驗收「點字改字」通過
- 已知：libchewing 選字後會重新斷詞，沒固定的相鄰字可能跟著變（例：入→辱 時 輸→書），這是引擎行為，未改
- 坑：SwiftUI 在容器上掛 accessibilityIdentifier 會蓋掉子元素自己的 identifier

授權頁（2026-11-09）：App 首頁「授權與原始碼」列 libchewing／libchewing-data 的 LGPL 2.1、原始碼連結、第 6 條重新連結說明；build 時把 vendor/libchewing 的 COPYING、AUTHORS 複製進 App 與鍵盤的 licenses/。
iPhone 實機單元測試 24 項全綠，記憶體增量 4.7 MB（2026-11-09）。裝到別人的裝置＝散布，LGPL 義務適用（授權頁即為此）。

iOS 27 候選列被糊掉（2026-11-09，iOS 27.0.1 實機回報）：
- 原因：iOS 26 起 ScrollView 碰到容器邊緣會加 scroll edge effect（模糊漸層）；鍵盤擴充的候選列在最上緣，iOS 27 實機與 iOS 27 模擬器整排糊掉。App 內預覽頁不會，只有鍵盤擴充會
- 修法：候選列 ScrollView 加 `scrollEdgeEffectHidden(true, for: .all)`（iOS 26+，用 #available 包）
- 迴路：iOS 27 模擬器跑 `KeyboardExtensionUITests/testCandidateBarInKeyboardExtension` 拍候選列，修前糊、修後清楚（`docs/ios27-candidates-before.png`／`after.png`）
- 教訓：實機已是 iOS 27，之前只在 iOS 26.5 模擬器測；之後 UI 驗證兩個版本都要跑
- 坑：iOS 27「新增鍵盤」改成長清單＋搜尋框，第三方鍵盤在最底下，測試改用搜尋「41」；直接 defaults 寫 AppleKeyboards 無效
- 測試 28 項：iOS 26.5 全綠（XCTest 24＋XCUITest 4），iOS 27 UI 4 項全綠
- 已重裝到 iPad；回報問題的 iPhone 還是舊版，下次接上 Mac 時重裝

決定（2026-11-10）：之後以**開源**方式上架（App Store＋Google Play），GitHub 個人帳號 goingli0324。公開前要用乾淨內容另開新 repo（舊歷史含裝置序號），授權頁連結改指 GitHub。
- 2026-11-10 定案：repo `goingli0324/eten41-ime`；commit 用 GitHub noreply 信箱；本專案程式碼 MIT（libchewing 照舊 LGPL）；HANDOFF 清掉裝置資料後公開——**之後不要在 HANDOFF 寫裝置序號、別人的裝置或帳號資料**
- 2026-11-10 改名：「倚天」是倚天酷碁在第 009 類（軟體）的有效註冊商標（智慧局註冊號 00797573，專用期限至民國124年底），App 改叫「41注音輸入法」，主畫面／鍵盤清單短名「41注音」；說明文字只寫「倚天41鍵盤排列」描述功能

Android 第一片完成（2026-11-10）：引擎在 Android 模擬器上跑通。
- 結構：`android/`（AGP 9.5.1、Gradle 9.6.0 wrapper、內建 Kotlin、compileSdk 36、minSdk 26、只編 arm64-v8a）
- 引擎：`scripts/build-libchewing-android.sh` 用 NDK 28.2.13676358（AGP 9.5 預設）把 libchewing 編成靜態庫，和 `android/jni/chewing_jni.c` 連成 `libeten41jni.so`（16KB page 對齊），詞庫與授權檔放 `build/android/assets`；Kotlin 包裝 `ChewingEngine.kt` 與 iOS 版同介面；第一次啟動 `DictionaryInstaller` 把詞庫從 assets 複製到私有資料夾
- 測試：`./gradlew connectedDebugAndroidTest`，模擬器 ziyuan（Pixel 7、Android 15、arm64）7 項全綠（37 鍵、聲調、四組詞、⇧ 標點、改中間字、最後一詞候選、清組字區）
- 環境：JAVA_HOME 用 Android Studio 內建 JBR（`/Applications/Android Studio.app/Contents/jbr/Contents/Home`）
- 坑：這個開發環境解析不到 `repo.maven.apache.org`（mavenCentral() 預設），改用 `maven("https://repo1.maven.org/maven2/")`；第一次編譯因此卡 20 分鐘才失敗
- JNI 字串一律回 UTF-8 byte[] 在 Kotlin 解碼（NewStringUTF 是 modified UTF-8，BMP 以外的字會壞）

Android 第二片完成（2026-11-10）：倚天41 輸入法可在模擬器上使用。
- `keyboard/`：Eten41Layout、KeyboardSession（與 iOS 同邏輯）、KeyboardView（傳統 View，不用 Compose）；`ime/Eten41InputMethodService`；`MainActivity`（啟用說明、試打欄、授權與原始碼）；`res/xml/method.xml`（zh-TW 子類型）
- 測試 17 項全綠：引擎 7、鍵盤邏輯 9、端到端 1（`ime enable/set` 切到倚天41，在試打欄打「東西」→點「東」改「冬」→⇧＋,→符號面板「、」＝「冬西，、」）
- 坑 1：Android 15 起輸入法視窗延伸到螢幕最底，系統把「收鍵盤／切換輸入法」畫在上面，最下排（換行、⇧）被蓋住、點到的是系統切換輸入法 → KeyboardView 加導覽列高度的底部 padding（端到端測試由紅轉綠確認）
- 坑 2：引擎的使用者詞庫會記住選過的字，測試共用一份會互相影響（端到端選「冬」後其他測試 ㄉㄨㄥ 先出冬）→ 測試一律用各自的暫存使用者詞庫；iOS 也同步改成 `ChewingEngine.makeForTesting()`
- 坑 3：鍵盤每次狀態改變都重建按鍵，UiAutomator 找到的物件會過期 → 點擊過期就重找；讀欄位改成等到內容符合

上架準備（2026-11-10 起）：
- 原定商店名稱「倚天41注音輸入法」，11-10 因商標改為「41注音輸入法」（見上）
- 定案 ID（上架後不能改）：iOS App `app.muzigoing.eten41`、鍵盤 `app.muzigoing.eten41.keyboard`、測試 `.tests`／`.uitests`；Android applicationId `app.muzigoing.eten41`（Kotlin namespace 仍是 com.muzigoing.eten41ime，使用者看不到）。格式比照紫垣 `app.muzigoing.ziyuan`
- 紫垣上架文件可參考：`~/Developer/Claude/ziyuan/.claude/worktrees/app-store-release-ae71c1/docs/`（app-store-checklist、store-submission-prep、closed-testing-kit、store-listing）
- iOS 主畫面拿掉測試用內容（固定案例、英數按鍵試打），只留啟用說明、試用鍵盤、試打欄（一般輸入框）、授權；TypingFieldUITests 一併移除
- 坑：鍵盤多（6～7 個）時連按地球鍵只在最近兩個鍵盤之間來回，UI 測試改成長按地球鍵開清單點「倚天41」
- 坑：模擬器殘留舊 ID（…smoke）的鍵盤會讓「已加入」判斷誤判；已從模擬器移除舊 App
- 測試：iOS 26.5（iPhone 17 Pro 模擬器）27 項全綠；Android 模擬器 17 項全綠
- 使用者的實機上還是舊 ID（…smoke）版本；換新 ID 安裝會變成另一個 App，舊的要不要刪先問使用者

開源 repo 已公開（2026-11-10）：https://github.com/goingli0324/eten41-ime
- 公開的歷史從乾淨的單一 commit 開始（作者用 noreply 信箱，repo 層 git config 已設好）
- 本機分支 `private-history` 是舊歷史備份，含裝置序號，**永遠不要 push**
- LICENSE 只放 MIT 全文，第三方授權移到 THIRD_PARTY_NOTICES.md（GitHub 才認得出 MIT）

隱私權政策（2026-11-10）：https://goingli0324.github.io/eten41-ime/privacy.html（GitHub Pages，main 的 /docs；原始檔 docs/privacy.md）。支援網址可填 repo 或 issues 頁。
- 內容依程式實際行為寫；之後若加網路、完整取用、分析工具，要同步改這頁

Android 上傳金鑰（2026-11-11）：`~/keystores/eten41-upload.jks`（alias eten41-upload，CN=MUZI-GOING，RSA 2048，有效至 2056），密碼在 `~/keystores/eten41-keystore.properties`（600 權限，repo 外）。`android/app/build.gradle.kts` 預設讀這個檔，或用環境變數 ETEN41_KEYSTORE_PROPERTIES 指定；檔案不在時 release 不簽章。
- 打包：`cd android && ./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`（jarsigner 驗過 jar verified）
- 上 Play Console 時用 Play App Signing（Google 保管正式簽章金鑰，我們只保管上傳金鑰；上傳金鑰遺失可申請重設）
- **金鑰檔與密碼要另外備份**（使用者自己的密碼管理器），不要放進任何 repo

iOS TestFlight（2026-11-11）：App Store Connect App ID 6833066181；1.0 (1) 已上傳、處理完成（準備提交）
- 封存＋上傳：`xcodebuild archive ... -configuration Release -destination 'generic/platform=iOS' -allowProvisioningUpdates`，再 `xcodebuild -exportArchive`（ExportOptions：method app-store-connect、destination upload、teamID、automatic）
- 坑：之前只用萬用描述檔，套件 ID 沒正式註冊，ASC 建 App 時下拉選單是空的 → 先 exportArchive（destination export）讓 Xcode 自動註冊 App 與鍵盤 ID
- 坑：支援 iPad 必須宣告 UISupportedInterfaceOrientations，否則上傳被退（Invalid bundle）
- 已加：兩個 PrivacyInfo.xcprivacy（File timestamp C617.1，libchewing 的 stat 系列）、ITSAppUsesNonExemptEncryption=NO、版本 1.0
- 內部測試群組「自己測試」（自動分發開啟）；2026-11-11 使用者從 TestFlight 安裝 1.0 (1) 實機試用正常
- 外部測試群組「主管測試」（1 人，名單只在 ASC）：1.0 (1) 2026-11-11 送 Beta 審查，狀態「等待審查」；TestFlight 測試資訊已填（審查聯絡人同紫垣）
- 下次上傳要把 CURRENT_PROJECT_VERSION 加 1
- 正式送審前：ASC 要求先填歐盟「貿易商狀態」（DSA）

Android 內部測試（2026-11-11）：Play Console 已建 App（套件 app.muzigoing.eten41），內部測試 1 (1.0) 已發布，測試名單 eten41-testers（2 人，名單內容只在 Play Console，不寫進 repo）；加入連結 https://play.google.com/apps/internaltest/4700878220930203526
- 版本 versionName 改 1.0；Play 只警告沒附去模糊化檔（沒開 R8，可忽略）
- 2026-11-11 使用者在 Android 實機從 Play 內部測試安裝 1.0 試用正常
- 坑：Play 內部測試不會寄邀請信，要把加入連結傳給測試人員；在 Play 商店 App 直接開連結、或裝置登入的帳號不在名單，會顯示「找不到項目」→ 用瀏覽器開、登入名單帳號按「接受邀請」
- 下次上傳 versionCode 要加 1

上架路線剩下：圖示／截圖／商店文字、Android 正式簽署金鑰、送審（iOS TestFlight→App Store；Android 內部測試→正式）。

還沒做：長按、大寫鎖定、Android。
下一片待使用者決定（建議：裝到使用者的 iPhone 實際用）。

---

## 5. 交接紀錄 (Checkpoint Log)
- [2026-11-09 13:57:05] Claude Code · `main` (`none`)：第一片完成：iOS 模擬器上 libchewing v0.13.1＋KB_ET 打字，XCTest 5/5 通過；尚未 commit，第二片待決（建議 Keyboard Extension）
- [2026-11-09 15:27:17] Claude Code · `main` (`none`)：第二片完成：iOS 倚天41鍵盤擴充（實體4排＋選字），模擬器端到端打字通過，測試14項全綠；尚未 commit、未上實機
- [2026-11-09 15:55:09] Claude Code · `main` (`none`)：標點完成：⇧ 單次＋符號面板，測試21項全綠，已重裝到 iPad Pro 13；尚未 commit
- [2026-11-09 16:03:23] Claude Code · `main` (`9c0004c`)：iPad Pro 13 實機驗收通過：倚天41鍵盤在備忘錄可正常打字與標點；下一步待使用者決定（Android／長按等）
- [2026-11-09 16:37:47] Claude Code · `main` (`e5c3933`)：點組字區改字完成並經 iPad 實機驗收；程式碼改動待使用者決定 commit
