import CChewing
import Foundation

/// 候選字：libchewing 的候選分好幾張清單（最長詞在第一張，單字在後面），
/// 選字時要回到同一張清單的同一個位置。
struct ChewingCandidate: Hashable {
    let listIndex: Int
    let choiceIndex: Int
    let text: String
    /// 這組候選是用哪個方向列的（選字時要用同一個方向）
    let direction: ChewingCandidateDirection
}

/// libchewing 的 phraseChoiceRearward：
/// - endingAtCursor：列出「結束在游標前那個字」的詞（打字中改最後一個詞用）
/// - startingAtCursor：列出「從游標所在字開始」的詞再接單字（等於微軟注音把游標移過去按 ↓）
enum ChewingCandidateDirection: Hashable {
    case endingAtCursor
    case startingAtCursor
}

/// libchewing 的薄包裝：固定用倚天41（KB_ET）。
final class ChewingEngine {
    enum EngineError: Error {
        case dictionaryMissing(String)
        case contextCreationFailed
        case keyboardLayoutRejected
    }

    private let context: OpaquePointer

    /// - Parameters:
    ///   - dictionaryDirectory: 放 word.dat / tsi.dat 等系統詞庫的資料夾
    ///   - userDictionaryPath: 使用者詞庫檔路徑（不存在時引擎會自己建）
    init(dictionaryDirectory: String, userDictionaryPath: String) throws {
        let wordDat = (dictionaryDirectory as NSString).appendingPathComponent("word.dat")
        guard FileManager.default.fileExists(atPath: wordDat) else {
            throw EngineError.dictionaryMissing(wordDat)
        }
        guard let context = chewing_new2(dictionaryDirectory, userDictionaryPath, nil, nil) else {
            throw EngineError.contextCreationFailed
        }
        self.context = context
        guard chewing_set_KBType(context, chewing_KBStr2Num("KB_ET")) == 0 else {
            chewing_delete(context)
            throw EngineError.keyboardLayoutRejected
        }
        chewing_set_maxChiSymbolLen(context, 16)
        // 預設 Esc 只關候選窗、不清組字區；清除要靠這個開關
        chewing_set_escCleanAllBuf(context, 1)
    }

    /// 用目前 bundle（App 或鍵盤擴充）裡的 dict/ 建立引擎；使用者詞庫預設放在自己的 Application Support。
    /// 使用者詞庫會記住選過的字（選過「冬」之後 ㄉㄨㄥ 先出冬），測試要傳各自的暫存檔避免互相影響。
    static func makeFromBundle(userDictionaryPath: String? = nil) throws -> ChewingEngine {
        let dictionaryDirectory = Bundle.main.bundleURL.appendingPathComponent("dict").path
        let resolvedUserPath: String
        if let userDictionaryPath {
            resolvedUserPath = userDictionaryPath
        } else {
            let supportDirectory = try FileManager.default.url(
                for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            resolvedUserPath = supportDirectory.appendingPathComponent("chewing-user.dat").path
        }
        return try ChewingEngine(dictionaryDirectory: dictionaryDirectory,
                                 userDictionaryPath: resolvedUserPath)
    }

    /// 測試用：每次一份全新的空白使用者詞庫
    static func makeForTesting() throws -> ChewingEngine {
        let path = FileManager.default.temporaryDirectory
            .appendingPathComponent("chewing-user-test-\(UUID().uuidString).dat").path
        return try makeFromBundle(userDictionaryPath: path)
    }

    deinit {
        chewing_delete(context)
    }

    // MARK: - 互動輸入（鍵盤用）

    /// 組字區：已轉好的字＋還在打的注音
    var composingText: String {
        String(cString: chewing_buffer_String_static(context))
            + String(cString: chewing_bopomofo_String_static(context))
    }

    var hasComposition: Bool {
        chewing_buffer_Check(context) != 0 || chewing_bopomofo_Check(context) != 0
    }

    /// 組字區裡已轉好的字，一格一個（游標位置 i 就是第 i 個字前面）
    var bufferCharacters: [String] {
        String(cString: chewing_buffer_String_static(context)).map(String.init)
    }

    /// 還在打、沒轉成字的注音
    var pendingBopomofo: String {
        String(cString: chewing_bopomofo_String_static(context))
    }

    var cursorPosition: Int { Int(chewing_cursor_Current(context)) }

    /// 把游標移到第 position 個字前面（等於按 ←→）
    func moveCursor(to position: Int) {
        let target = min(max(position, 0), bufferCharacters.count)
        var guardCount = bufferCharacters.count + 2
        while cursorPosition > target, guardCount > 0 { chewing_handle_Left(context); guardCount -= 1 }
        while cursorPosition < target, guardCount > 0 { chewing_handle_Right(context); guardCount -= 1 }
    }

    func moveCursorToEnd() {
        moveCursor(to: bufferCharacters.count)
    }

    func pressKey(_ ascii: Character) {
        guard let value = ascii.asciiValue else { return }
        chewing_handle_Default(context, Int32(value))
    }

    func pressSpace() { chewing_handle_Space(context) }
    func pressBackspace() { chewing_handle_Backspace(context) }
    func pressEnter() { chewing_handle_Enter(context) }

    /// 上一個動作造成的上屏文字；libchewing 在下一個按鍵時會清掉，所以每個動作後要馬上讀。
    func takeCommittedText() -> String? {
        guard chewing_commit_Check(context) != 0 else { return nil }
        let text = String(cString: chewing_commit_String_static(context))
        return text.isEmpty ? nil : text
    }

    /// 游標位置的候選字（方向見 ChewingCandidateDirection）；還在打注音或組字區是空的時候沒有候選。
    func candidates(_ direction: ChewingCandidateDirection = .endingAtCursor,
                    limit: Int = 80) -> [ChewingCandidate] {
        guard chewing_bopomofo_Check(context) == 0, chewing_buffer_Check(context) != 0 else { return [] }
        apply(direction)
        guard chewing_cand_open(context) == 0 else { return [] }
        defer { chewing_cand_close(context) }
        chewing_cand_list_first(context)
        var result: [ChewingCandidate] = []
        var listIndex = 0
        repeat {
            let total = Int(chewing_cand_TotalChoice(context))
            for choiceIndex in 0..<total where result.count < limit {
                if let text = chewing_cand_string_by_index_static(context, Int32(choiceIndex)) {
                    result.append(ChewingCandidate(listIndex: listIndex, choiceIndex: choiceIndex,
                                                   text: String(cString: text), direction: direction))
                }
            }
            listIndex += 1
        } while result.count < limit
            && chewing_cand_list_has_next(context) != 0
            && chewing_cand_list_next(context) == 0
        return result
    }

    @discardableResult
    func choose(_ candidate: ChewingCandidate) -> Bool {
        apply(candidate.direction)
        guard chewing_cand_open(context) == 0 else { return false }
        defer { chewing_cand_close(context) }
        chewing_cand_list_first(context)
        for _ in 0..<candidate.listIndex {
            guard chewing_cand_list_next(context) == 0 else { return false }
        }
        return chewing_cand_choose_by_index(context, Int32(candidate.choiceIndex)) == 0
    }

    private func apply(_ direction: ChewingCandidateDirection) {
        chewing_set_phraseChoiceRearward(context, direction == .endingAtCursor ? 1 : 0)
    }

    // MARK: - 一次送一串按鍵（測試與驗證畫面用）

    /// 按鍵串送進引擎，空白鍵當一聲，最後按 Enter 上字，回傳上屏的字。
    func commitText(forKeys keys: String) -> String {
        press(keys)
        pressEnter()
        let committed = takeCommittedText() ?? ""
        clear()
        return committed
    }

    /// 按鍵串送進引擎但不上字，回傳目前組字區的注音（例如 ",x-" → "ㄓㄨㄥ"）。
    func pendingBopomofo(forKeys keys: String) -> String {
        press(keys)
        let bopomofo = String(cString: chewing_bopomofo_String_static(context))
        clear()
        return bopomofo
    }

    /// 按鍵串送進引擎但不上字，回傳組字區每個音節的讀音（含聲調符號，一聲不帶符號）。
    func syllableReadings(forKeys keys: String) -> [String] {
        press(keys)
        defer { clear() }
        let count = Int(chewing_get_phoneSeqLen(context))
        guard count > 0, let sequence = chewing_get_phoneSeq(context) else { return [] }
        defer { chewing_free(sequence) }
        return (0..<count).map { index in
            var buffer = [CChar](repeating: 0, count: 32)
            // chewing.h 註解說成功回 0，但 v0.13.1 實作（capi/src/io.rs）成功回「位元組數＋1」、失敗回 -1
            guard chewing_phone_to_bopomofo(sequence[index], &buffer, UInt16(buffer.count)) > 0 else {
                return ""
            }
            return String(cString: buffer)
        }
    }

    /// 清掉組字區（不上字）
    func clear() {
        chewing_handle_Esc(context)
        chewing_handle_Esc(context)
    }

    private func press(_ keys: String) {
        for character in keys {
            if character == " " {
                pressSpace()
            } else {
                pressKey(character)
            }
        }
    }
}
