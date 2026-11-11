import Combine

/// 鍵盤把字送去哪裡：鍵盤擴充是 textDocumentProxy，App 預覽是畫面上的文字。
protocol KeyboardTextOutput: AnyObject {
    func insertText(_ text: String)
    func deleteBackward()
}

/// 螢幕鍵盤的狀態與按鍵邏輯，App 預覽、鍵盤擴充、測試共用。
final class KeyboardSession: ObservableObject {
    /// 組字區已轉好的字，一格一個（畫面上每個字可以點）
    @Published private(set) var composingCharacters: [String] = []
    /// 還在打、沒轉成字的注音
    @Published private(set) var pendingBopomofo = ""
    /// 使用者點了組字區第幾個字（nil＝游標在最後，候選是最後一個詞）
    @Published private(set) var focusedIndex: Int?
    @Published private(set) var candidates: [ChewingCandidate] = []
    @Published private(set) var isEnglishMode = false
    /// ⇧ 三段循環：按一下只作用在下一個鍵，再按一下鎖定，第三下解除
    enum ShiftState { case off, once, locked }
    @Published private(set) var shiftState = ShiftState.off
    var isShifted: Bool { shiftState != .off }
    var isShiftLocked: Bool { shiftState == .locked }
    @Published private(set) var isShowingSymbols = false

    var composingText: String { composingCharacters.joined() + pendingBopomofo }

    private let engine: ChewingEngine
    private weak var output: KeyboardTextOutput?

    init(engine: ChewingEngine, output: KeyboardTextOutput?) {
        self.engine = engine
        self.output = output
    }

    func attach(output: KeyboardTextOutput) {
        self.output = output
    }

    func tap(_ key: Eten41Key) {
        let shifted = isShifted
        if shiftState == .once { shiftState = .off }
        if isEnglishMode {
            output?.insertText(shifted ? key.englishShiftedText : String(key.ascii))
            return
        }
        releaseFocus()
        // 中文模式 ⇧ 只改非字母鍵：送 Shift 後的 ASCII，引擎會轉成全形標點（如 Shift＋, →，）
        engine.pressKey(shifted ? (key.shiftedAscii ?? key.ascii) : key.ascii)
        flush()
    }

    func toggleShift() {
        switch shiftState {
        case .off: shiftState = .once
        case .once: shiftState = .locked
        case .locked: shiftState = .off
        }
    }

    func toggleSymbols() {
        isShowingSymbols.toggle()
        shiftState = .off
    }

    /// 符號面板：先把組字區上字，再插入標點，然後回到注音鍵
    func tapSymbol(_ symbol: PunctuationSymbol) {
        if engine.hasComposition {
            releaseFocus()
            engine.pressEnter()
            flush()
        }
        output?.insertText(symbol.text)
        isShowingSymbols = false
    }

    func tapSpace() {
        guard !isEnglishMode, engine.hasComposition else {
            output?.insertText(" ")
            return
        }
        releaseFocus()
        engine.pressSpace()
        flush()
    }

    func tapBackspace() {
        guard !isEnglishMode, engine.hasComposition else {
            output?.deleteBackward()
            return
        }
        releaseFocus()
        engine.pressBackspace()
        flush()
    }

    func tapReturn() {
        guard !isEnglishMode, engine.hasComposition else {
            output?.insertText("\n")
            return
        }
        releaseFocus()
        engine.pressEnter()
        flush()
    }

    /// 點組字區第 index 個字：等於微軟注音把游標移到那個字再按 ↓，候選換成從這個字開始的詞與單字。
    /// 再點同一個字取消。還在打注音時不能點（字還沒轉好）。
    func focusCharacter(at index: Int) {
        guard pendingBopomofo.isEmpty, composingCharacters.indices.contains(index) else { return }
        if focusedIndex == index {
            releaseFocus()
        } else {
            engine.moveCursor(to: index)
            focusedIndex = index
        }
        refresh()
    }

    /// 選字後游標回到最後，接著打的字接在後面
    func choose(_ candidate: ChewingCandidate) {
        engine.choose(candidate)
        releaseFocus()
        flush()
    }

    /// 中英切換；切到英文前先把組字區上字，避免打到一半的字消失。
    func toggleEnglishMode() {
        if !isEnglishMode, engine.hasComposition {
            releaseFocus()
            engine.pressEnter()
            flush()
        }
        isEnglishMode.toggle()
    }

    private func releaseFocus() {
        guard focusedIndex != nil else { return }
        engine.moveCursorToEnd()
        focusedIndex = nil
    }

    private func flush() {
        if let committed = engine.takeCommittedText() {
            output?.insertText(committed)
        }
        refresh()
    }

    private func refresh() {
        composingCharacters = engine.bufferCharacters
        pendingBopomofo = engine.pendingBopomofo
        if let focusedIndex, !composingCharacters.indices.contains(focusedIndex) {
            self.focusedIndex = nil
        }
        candidates = engine.candidates(focusedIndex == nil ? .endingAtCursor : .startingAtCursor)
    }
}
