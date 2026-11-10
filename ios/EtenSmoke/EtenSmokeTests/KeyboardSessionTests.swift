import XCTest
@testable import EtenSmoke

private final class RecordingOutput: KeyboardTextOutput {
    var text = ""
    var deleteCount = 0
    func insertText(_ text: String) { self.text += text }
    func deleteBackward() { deleteCount += 1 }
}

final class KeyboardSessionTests: XCTestCase {
    private var output: RecordingOutput!
    private var session: KeyboardSession!

    override func setUpWithError() throws {
        output = RecordingOutput()
        session = KeyboardSession(engine: try ChewingEngine.makeForTesting(), output: output)
    }

    private func key(_ ascii: Character) -> Eten41Key {
        Eten41Layout.allKeys.first { $0.ascii == ascii }!
    }

    private func type(_ keys: String) {
        for character in keys {
            if character == " " { session.tapSpace() } else { session.tap(key(character)) }
        }
    }

    /// 螢幕鍵盤每個鍵帽上的注音，要跟引擎實際打出來的注音一致（等於對定案表再驗一次）。
    func testLayoutKeycapsMatchEngine() throws {
        let engine = try ChewingEngine.makeForTesting()
        let keys = Eten41Layout.allKeys.filter { $0.symbol != nil && !"1234".contains($0.ascii) }
        XCTAssertEqual(keys.count, 37)
        for key in keys {
            XCTAssertEqual(engine.pendingBopomofo(forKeys: String(key.ascii)), key.symbol, "鍵 \(key.ascii)")
        }
        XCTAssertEqual(Eten41Layout.rows.map(\.count), [12, 10, 11, 10])
    }

    func testTypingThenReturnCommitsToOutput() {
        type("dx- ce1")
        XCTAssertEqual(session.composingText, "東西")
        XCTAssertEqual(output.text, "")
        session.tapReturn()
        XCTAssertEqual(output.text, "東西")
        XCTAssertEqual(session.composingText, "")
        XCTAssertTrue(session.candidates.isEmpty)
    }

    func testChoosingCandidateReplacesLastCharacter() throws {
        type("/x jx4fa3")
        XCTAssertEqual(session.candidates.first?.text, "輸入法")
        let alternative = try XCTUnwrap(session.candidates.first { $0.text == "髮" })
        session.choose(alternative)
        XCTAssertEqual(session.composingText, "輸入髮")
        session.tapReturn()
        XCTAssertEqual(output.text, "輸入髮")
    }

    func testBackspaceEditsCompositionBeforeDocument() {
        type("dx")
        XCTAssertEqual(session.composingText, "ㄉㄨ")
        session.tapBackspace()
        XCTAssertEqual(session.composingText, "ㄉ")
        session.tapBackspace()
        XCTAssertEqual(session.composingText, "")
        XCTAssertEqual(output.deleteCount, 0)
        session.tapBackspace()
        XCTAssertEqual(output.deleteCount, 1, "組字區空了才刪文件裡的字")
    }

    func testSpaceAndReturnWithoutCompositionGoToDocument() {
        session.tapSpace()
        session.tapReturn()
        XCTAssertEqual(output.text, " \n")
    }

    func testEnglishModeCommitsPendingTextAndTypesAscii() {
        type("e3te8 ")
        session.toggleEnglishMode()
        XCTAssertEqual(output.text, "倚天", "切英文前要先上字")
        type("ok")
        session.tapSpace()
        XCTAssertEqual(output.text, "倚天ok ")
        session.toggleEnglishMode()
        XCTAssertFalse(session.isEnglishMode)
    }

    // MARK: - 點組字區的字改字（微軟注音的 ←→ 再 ↓）

    func testTappingMiddleCharacterListsItsCandidatesAndReplacesIt() throws {
        type("/x jx4fa3")
        XCTAssertEqual(session.composingCharacters, ["輸", "入", "法"])
        session.focusCharacter(at: 1)
        XCTAssertEqual(session.focusedIndex, 1)
        XCTAssertTrue(session.candidates.contains { $0.text == "入" }, "候選要有「入」本身")
        let alternative = try XCTUnwrap(session.candidates.first { $0.text == "辱" })
        session.choose(alternative)
        XCTAssertEqual(session.composingCharacters.count, 3)
        XCTAssertEqual(session.composingCharacters[1], "辱", "第 2 個字要換掉")
        XCTAssertNil(session.focusedIndex, "選完游標回到最後")
    }

    func testTypingAfterFocusAppendsAtEnd() {
        type("/x jx4fa3")
        session.focusCharacter(at: 0)
        type("dx- ")
        XCTAssertEqual(session.composingText, "輸入法東", "點過字之後繼續打，要接在最後")
        XCTAssertNil(session.focusedIndex)
    }

    func testTappingSameCharacterAgainCancelsFocus() {
        type("/x jx4fa3")
        session.focusCharacter(at: 1)
        session.focusCharacter(at: 1)
        XCTAssertNil(session.focusedIndex)
        XCTAssertEqual(session.candidates.first?.text, "輸入法", "回到最後一個詞的候選")
    }

    func testCannotFocusWhileBopomofoPending() {
        type("/x jx4fa3d")
        XCTAssertEqual(session.pendingBopomofo, "ㄉ")
        session.focusCharacter(at: 0)
        XCTAssertNil(session.focusedIndex)
    }

    // MARK: - 標點

    /// ⇧ 鍵帽上印的全形標點，要跟引擎實際打出來的一致
    func testShiftedKeycapsMatchEngine() throws {
        let engine = try ChewingEngine.makeForTesting()
        let keys = Eten41Layout.allKeys.filter { $0.shiftedAscii != nil }
        XCTAssertEqual(keys.count, 17)
        for key in keys {
            XCTAssertEqual(engine.commitText(forKeys: String(key.shiftedAscii!)), key.shiftedFullWidth,
                           "Shift＋\(key.ascii)")
        }
    }

    func testShiftPunctuationJoinsComposition() {
        type("dx- ")
        session.toggleShift()
        session.tap(key(","))
        XCTAssertEqual(session.composingText, "東，")
        XCTAssertFalse(session.isShifted)
        session.tapReturn()
        XCTAssertEqual(output.text, "東，")
    }

    func testShiftIsOneShot() {
        session.toggleShift()
        session.tap(key(","))
        session.tap(key(","))
        XCTAssertEqual(session.composingText, "，ㄓ", "第二次 , 要回到 ㄓ")
    }

    func testShiftDoesNotChangeLettersInChineseMode() {
        session.toggleShift()
        session.tap(key("d"))
        XCTAssertEqual(session.composingText, "ㄉ")
    }

    func testSymbolPanelCommitsCompositionThenInserts() throws {
        type("dx- ce1")
        session.toggleSymbols()
        XCTAssertTrue(session.isShowingSymbols)
        let comma = try XCTUnwrap(Eten41Layout.punctuationRows.joined().first { $0.label == "、" })
        session.tapSymbol(comma)
        XCTAssertEqual(output.text, "東西、")
        XCTAssertFalse(session.isShowingSymbols, "選完標點回到注音鍵")
    }

    func testEllipsisAndDashInsertTaiwanDoubleForms() throws {
        let symbols = Eten41Layout.punctuationRows.joined()
        session.tapSymbol(try XCTUnwrap(symbols.first { $0.label == "…" }))
        session.tapSymbol(try XCTUnwrap(symbols.first { $0.label == "—" }))
        XCTAssertEqual(output.text, "……——")
    }

    func testEnglishShiftGivesUppercaseAndSymbols() {
        session.toggleEnglishMode()
        session.toggleShift()
        session.tap(key("o"))
        session.tap(key("k"))
        session.toggleShift()
        session.tap(key("/"))
        XCTAssertEqual(output.text, "Ok?")
    }

    /// 鍵盤擴充有記憶體上限（社群資料約 48MB，Apple 沒公開）；在模擬器量引擎＋詞庫載入的增量當參考。
    func testEngineMemoryFootprintIsSmall() throws {
        let before = Self.physicalFootprintBytes()
        let engine = try ChewingEngine.makeForTesting()
        _ = engine.commitText(forKeys: "/x jx4fa3")
        let after = Self.physicalFootprintBytes()
        let deltaMB = Double(after - before) / 1_048_576
        print("[memory] engine footprint delta: \(String(format: "%.1f", deltaMB)) MB, total \(after / 1_048_576) MB")
        XCTAssertLessThan(deltaMB, 20, "引擎載入增量 \(deltaMB) MB")
        withExtendedLifetime(engine) {}
    }

    private static func physicalFootprintBytes() -> UInt64 {
        var info = task_vm_info_data_t()
        var count = mach_msg_type_number_t(MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<natural_t>.size)
        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), $0, &count)
            }
        }
        return result == KERN_SUCCESS ? info.phys_footprint : 0
    }
}
