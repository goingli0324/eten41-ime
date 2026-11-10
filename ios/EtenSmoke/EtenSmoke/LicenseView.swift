import SwiftUI

/// 本 App 自己的程式碼是 MIT；libchewing 是 LGPL-2.1，散布時要附授權全文、版權聲明，並讓收到的人能取得原始碼。
struct LicenseView: View {
    private static let appSource = "https://github.com/goingli0324/eten41-ime"
    private static let libchewingSource = "https://codeberg.org/chewing/libchewing/src/tag/v0.13.1"
    private static let libchewingDataSource = "https://codeberg.org/chewing/libchewing-data"

    var body: some View {
        List {
            Section("41注音輸入法") {
                VStack(alignment: .leading, spacing: 6) {
                    Text("本 App 的程式碼以 MIT 授權開源").font(.callout)
                    Link("原始碼（GitHub）", destination: URL(string: Self.appSource)!)
                }
            }
            Section("本 App 使用的開源元件") {
                VStack(alignment: .leading, spacing: 6) {
                    Text("libchewing v0.13.1（新酷音輸入法核心）").font(.headline)
                    Text("授權：GNU LGPL 2.1 或更新版本").font(.callout)
                    Link("原始碼（Codeberg）", destination: URL(string: Self.libchewingSource)!)
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text("libchewing-data（詞庫）").font(.headline)
                    Text("授權：GNU LGPL 2.1 或更新版本").font(.callout)
                    Link("原始碼（Codeberg）", destination: URL(string: Self.libchewingDataSource)!)
                }
            }
            Section("關於重新連結") {
                Text("本 App 以靜態連結使用 libchewing，未修改其原始碼。本 App 的完整原始碼公開在上方 GitHub，你可以替換 libchewing 後自行重新建置。依 LGPL 2.1 第 6 條，你也可以向提供此 App 的人索取本 App 的原始碼或目的檔；此提供自你取得本 App 起三年內有效。")
                    .font(.callout)
            }
            Section("授權全文與作者") {
                NavigationLink("GNU LGPL 2.1 全文") { BundledTextView(title: "LGPL 2.1", fileName: "libchewing-COPYING.txt") }
                NavigationLink("libchewing 作者名單") { BundledTextView(title: "作者", fileName: "libchewing-AUTHORS.txt") }
            }
        }
        .navigationTitle("授權與原始碼")
    }
}

/// 顯示 build 時複製進 bundle 的 licenses/ 文字檔
private struct BundledTextView: View {
    let title: String
    let fileName: String

    var body: some View {
        ScrollView {
            Text(content)
                .font(.footnote.monospaced())
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
        }
        .navigationTitle(title)
    }

    private var content: String {
        let url = Bundle.main.bundleURL.appendingPathComponent("licenses").appendingPathComponent(fileName)
        return (try? String(contentsOf: url, encoding: .utf8)) ?? "找不到 \(fileName)，請重新建置 App。"
    }
}
