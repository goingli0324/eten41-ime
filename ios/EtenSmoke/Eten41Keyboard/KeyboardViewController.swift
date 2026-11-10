import SwiftUI
import UIKit

/// 鍵盤擴充入口：載入引擎，把 SwiftUI 鍵盤掛上去，字送到 textDocumentProxy。
final class KeyboardViewController: UIInputViewController, KeyboardTextOutput {
    private var session: KeyboardSession?
    private var hostingController: UIHostingController<AnyView>?

    override func viewDidLoad() {
        super.viewDidLoad()
        let rootView: AnyView
        do {
            let session = KeyboardSession(engine: try ChewingEngine.makeFromBundle(), output: nil)
            session.attach(output: self)
            self.session = session
            rootView = AnyView(KeyboardView(session: session,
                                            showsGlobeKey: needsInputModeSwitchKey,
                                            onGlobe: { [weak self] in self?.advanceToNextInputMode() }))
        } catch {
            // 詞庫載不起來時至少讓使用者能切回其他鍵盤
            rootView = AnyView(EngineLoadFailedView(message: "\(error)",
                                                    onGlobe: { [weak self] in self?.advanceToNextInputMode() }))
        }
        let hosting = UIHostingController(rootView: rootView)
        hosting.view.translatesAutoresizingMaskIntoConstraints = false
        hosting.view.backgroundColor = .clear
        addChild(hosting)
        view.addSubview(hosting.view)
        NSLayoutConstraint.activate([
            hosting.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            hosting.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            hosting.view.topAnchor.constraint(equalTo: view.topAnchor),
            hosting.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            hosting.view.heightAnchor.constraint(equalToConstant: KeyboardView.totalHeight),
        ])
        hosting.didMove(toParent: self)
        hostingController = hosting
    }

    func insertText(_ text: String) {
        textDocumentProxy.insertText(text)
    }

    func deleteBackward() {
        textDocumentProxy.deleteBackward()
    }
}

private struct EngineLoadFailedView: View {
    let message: String
    let onGlobe: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text("41注音 詞庫載入失敗").font(.headline)
            Text(message).font(.caption).foregroundStyle(.secondary)
            Button("切換到其他鍵盤", action: onGlobe)
        }
        .frame(maxWidth: .infinity)
        .frame(height: KeyboardView.totalHeight)
    }
}
