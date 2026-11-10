import XCTest
@testable import EtenSmoke

final class ChewingEngineTests: XCTestCase {
    private var engine: ChewingEngine!

    override func setUpWithError() throws {
        engine = try ChewingEngine.makeForTesting()
    }

    /// 定案按鍵表（docs/keymap-eten41.md）每一鍵單按，組字區要出現對應注音。
    func testEveryKeyMatchesDecidedKeymap() {
        let keymap: [Character: String] = [
            "a": "ㄚ", "b": "ㄅ", "c": "ㄒ", "d": "ㄉ", "e": "ㄧ", "f": "ㄈ", "g": "ㄐ",
            "h": "ㄏ", "i": "ㄞ", "j": "ㄖ", "k": "ㄎ", "l": "ㄌ", "m": "ㄇ", "n": "ㄋ",
            "o": "ㄛ", "p": "ㄆ", "q": "ㄟ", "r": "ㄜ", "s": "ㄙ", "t": "ㄊ", "u": "ㄩ",
            "v": "ㄍ", "w": "ㄝ", "x": "ㄨ", "y": "ㄡ", "z": "ㄠ",
            "7": "ㄑ", "8": "ㄢ", "9": "ㄣ", "0": "ㄤ", "-": "ㄥ", "=": "ㄦ",
            ",": "ㄓ", ".": "ㄔ", "/": "ㄕ", ";": "ㄗ", "'": "ㄘ",
        ]
        XCTAssertEqual(keymap.count, 37)
        for (key, symbol) in keymap {
            XCTAssertEqual(engine.pendingBopomofo(forKeys: String(key)), symbol, "按鍵 \(key)")
        }
    }

    /// 聲調鍵：引擎存下來的讀音要帶對的聲調符號（不靠候選字，候選字會受詞頻影響）。
    func testToneKeysStoreExpectedTone() {
        let expectations = [
            "ma1": "ㄇㄚ˙", "ma2": "ㄇㄚˊ", "ma3": "ㄇㄚˇ", "ma4": "ㄇㄚˋ", "ma ": "ㄇㄚ",
        ]
        for (keys, reading) in expectations {
            XCTAssertEqual(engine.syllableReadings(forKeys: keys), [reading], "按鍵 \(keys)")
        }
    }

    /// 每次呼叫之間組字區要清乾淨，不能把上一次的音節帶過來。
    func testCallsDoNotLeakSyllables() {
        _ = engine.syllableReadings(forKeys: "ma1")
        XCTAssertEqual(engine.syllableReadings(forKeys: "ma2"), ["ㄇㄚˊ"])
        _ = engine.commitText(forKeys: "e3te8 ")
        XCTAssertEqual(engine.syllableReadings(forKeys: "ma "), ["ㄇㄚ"])
    }

    func testSmokeCasesCommitExpectedText() {
        for smokeCase in smokeCases {
            XCTAssertEqual(engine.commitText(forKeys: smokeCase.keys), smokeCase.expected,
                           "按鍵 \(smokeCase.keys)（\(smokeCase.reading)）")
        }
    }

    func testMissingDictionaryThrows() {
        XCTAssertThrowsError(try ChewingEngine(dictionaryDirectory: "/nonexistent",
                                               userDictionaryPath: NSTemporaryDirectory() + "x.dat"))
    }
}
