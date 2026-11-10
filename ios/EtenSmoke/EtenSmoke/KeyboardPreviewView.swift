import SwiftUI

/// 在 App 裡直接用同一個鍵盤畫面打字，不用先去設定開鍵盤。
struct KeyboardPreviewView: View {
    @StateObject private var output = PreviewTextOutput()
    @State private var session: KeyboardSession?
    @State private var loadError: String?

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                Text(output.text.isEmpty ? "（打出來的字會出現在這裡）" : output.text)
                    .foregroundStyle(output.text.isEmpty ? .secondary : .primary)
                    .font(.title2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .accessibilityIdentifier("previewText")
            }
            if let session {
                KeyboardView(session: session)
            } else if let loadError {
                Text(loadError).foregroundStyle(.red).padding()
            }
        }
        .navigationTitle("鍵盤試用")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            guard session == nil else { return }
            do {
                session = KeyboardSession(engine: try ChewingEngine.makeFromBundle(), output: output)
            } catch {
                loadError = "\(error)"
            }
        }
    }
}

final class PreviewTextOutput: ObservableObject, KeyboardTextOutput {
    @Published private(set) var text = ""

    func insertText(_ text: String) {
        self.text += text
    }

    func deleteBackward() {
        guard !text.isEmpty else { return }
        text.removeLast()
    }
}
