/// 倚天41 螢幕鍵盤的排列，照實體鍵盤 4 排（docs/keymap-eten41.md 定案表）。
struct Eten41Key: Hashable {
    /// 送進引擎的 ASCII 鍵
    let ascii: Character
    /// 鍵帽上的注音或聲調；倚天41 沒用到的鍵（5、6）是 nil
    let symbol: String?

    /// 按 ⇧ 後送出的 ASCII（等於電腦上 Shift＋這個鍵）；字母鍵是 nil
    var shiftedAscii: Character? { Eten41Layout.shifted[ascii]?.ascii }
    /// 中文模式按 ⇧ 後引擎打出來的全形標點，用來當鍵帽
    var shiftedFullWidth: String? { Eten41Layout.shifted[ascii]?.fullWidth }
    /// 英文模式按 ⇧：字母變大寫，其他鍵送 Shift 後的符號
    var englishShiftedText: String { shiftedAscii.map { String($0) } ?? String(ascii).uppercased() }
}

/// 符號面板的一格：鍵帽顯示 `label`，實際送出 `text`（刪節號、破折號照台灣用法送兩個）
struct PunctuationSymbol: Hashable {
    let label: String
    let text: String
}

enum Eten41Layout {
    static let rows: [[Eten41Key]] = [
        row("1˙ 2ˊ 3ˇ 4ˋ 5 6 7ㄑ 8ㄢ 9ㄣ 0ㄤ -ㄥ =ㄦ"),
        row("qㄟ wㄝ eㄧ rㄜ tㄊ yㄡ uㄩ iㄞ oㄛ pㄆ"),
        row("aㄚ sㄙ dㄉ fㄈ gㄐ hㄏ jㄖ kㄎ lㄌ ;ㄗ 'ㄘ"),
        row("zㄠ xㄨ cㄒ vㄍ bㄅ nㄋ mㄇ ,ㄓ .ㄔ /ㄕ"),
    ]

    static var allKeys: [Eten41Key] { rows.flatMap { $0 } }

    /// 非字母鍵的 Shift 對照：ASCII 照美式鍵盤，全形標點是 libchewing v0.13.1 實測輸出
    static let shifted: [Character: (ascii: Character, fullWidth: String)] = [
        "1": ("!", "！"), "2": ("@", "＠"), "3": ("#", "＃"), "4": ("$", "＄"),
        "5": ("%", "％"), "6": ("^", "︿"), "7": ("&", "＆"), "8": ("*", "＊"),
        "9": ("(", "（"), "0": (")", "）"), "-": ("_", "—"), "=": ("+", "＋"),
        ";": (":", "："), "'": ("\"", "；"), ",": ("<", "，"), ".": (">", "。"), "/": ("?", "？"),
    ]

    /// 符號面板（常用全形標點）
    static let punctuationRows: [[PunctuationSymbol]] = [
        symbols("， 。 、 ？ ！ ： ；"),
        symbols("「 」 『 』 （ ） …"),
        symbols("— ～ · 《 》 〈 〉"),
    ]

    /// "qㄟ" 這種字串：第一個字元是 ASCII 鍵，其餘是鍵帽符號
    private static func row(_ spec: String) -> [Eten41Key] {
        spec.split(separator: " ").map { token in
            let symbol = String(token.dropFirst())
            return Eten41Key(ascii: token.first!, symbol: symbol.isEmpty ? nil : symbol)
        }
    }

    private static func symbols(_ spec: String) -> [PunctuationSymbol] {
        spec.split(separator: " ").map { token in
            let label = String(token)
            let text = (label == "…" || label == "—") ? label + label : label
            return PunctuationSymbol(label: label, text: text)
        }
    }
}
