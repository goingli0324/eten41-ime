import SwiftUI

/// App 主畫面：怎麼開啟鍵盤、試打、授權與原始碼。上架審查看的是這個畫面，不放測試用內容。
struct ContentView: View {
    @State private var practiceText = ""

    var body: some View {
        NavigationStack {
            List {
                Section("開啟41注音鍵盤") {
                    Text("用倚天41鍵盤排列打注音。設定 → 一般 → 鍵盤 → 鍵盤 → 新增鍵盤 → 41注音。之後在任何 App 按地球鍵切換。")
                        .font(.callout)
                    Text("41注音不需要「允許完整取用」，不連網、不收集你打的字。")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    NavigationLink("在這裡先試用鍵盤") { KeyboardPreviewView() }
                        .accessibilityIdentifier("openKeyboardPreview")
                }
                Section("試打（切到41注音後在這裡打字）") {
                    // 單行：多行 TextField 在 UI 測試裡是 textView，端到端測試找的是 textField
                    TextField("點這裡打字", text: $practiceText)
                        .accessibilityIdentifier("typedKeysField")
                }
                Section {
                    NavigationLink("授權與原始碼") { LicenseView() }
                        .accessibilityIdentifier("openLicense")
                }
            }
            .navigationTitle("41注音輸入法")
        }
    }
}
