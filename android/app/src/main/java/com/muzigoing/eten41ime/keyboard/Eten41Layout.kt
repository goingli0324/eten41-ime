package com.muzigoing.eten41ime.keyboard

/** 倚天41 螢幕鍵盤的一個鍵；鍵帽只印英數（使用者看英數鍵位打字），注音只用在測試對照。 */
data class Eten41Key(val ascii: Char, val symbol: String?) {
    /** 按 ⇧ 後送出的 ASCII（電腦上 Shift＋這個鍵）；字母鍵是 null */
    val shiftedAscii: Char? get() = Eten41Layout.shifted[ascii]?.first
    /** 中文模式按 ⇧ 後引擎打出的全形標點，用來當鍵帽 */
    val shiftedFullWidth: String? get() = Eten41Layout.shifted[ascii]?.second
    /** 英文模式按 ⇧：字母大寫，其他鍵送 Shift 後的符號 */
    val englishShiftedText: String get() = shiftedAscii?.toString() ?: ascii.uppercase()
}

/** 符號面板的一格：鍵帽顯示 label，實際送出 text（刪節號、破折號照台灣用法送兩個） */
data class PunctuationSymbol(val label: String, val text: String)

/** 與 iOS 版 Shared/Eten41Layout.swift 相同的資料 */
object Eten41Layout {
    val rows: List<List<Eten41Key>> = listOf(
        row("1˙ 2ˊ 3ˇ 4ˋ 5 6 7ㄑ 8ㄢ 9ㄣ 0ㄤ -ㄥ =ㄦ"),
        row("qㄟ wㄝ eㄧ rㄜ tㄊ yㄡ uㄩ iㄞ oㄛ pㄆ"),
        row("aㄚ sㄙ dㄉ fㄈ gㄐ hㄏ jㄖ kㄎ lㄌ ;ㄗ 'ㄘ"),
        row("zㄠ xㄨ cㄒ vㄍ bㄅ nㄋ mㄇ ,ㄓ .ㄔ /ㄕ"),
    )

    val allKeys: List<Eten41Key> get() = rows.flatten()

    /** 非字母鍵的 Shift 對照：ASCII 照美式鍵盤，全形標點是 libchewing v0.13.1 實測輸出 */
    val shifted: Map<Char, Pair<Char, String>> = mapOf(
        '1' to ('!' to "！"), '2' to ('@' to "＠"), '3' to ('#' to "＃"), '4' to ('$' to "＄"),
        '5' to ('%' to "％"), '6' to ('^' to "︿"), '7' to ('&' to "＆"), '8' to ('*' to "＊"),
        '9' to ('(' to "（"), '0' to (')' to "）"), '-' to ('_' to "—"), '=' to ('+' to "＋"),
        ';' to (':' to "："), '\'' to ('"' to "；"), ',' to ('<' to "，"), '.' to ('>' to "。"), '/' to ('?' to "？"),
    )

    val punctuationRows: List<List<PunctuationSymbol>> = listOf(
        symbols("， 。 、 ？ ！ ： ；"),
        symbols("「 」 『 』 （ ） …"),
        symbols("— ～ · 《 》 〈 〉"),
    )

    private fun row(spec: String): List<Eten41Key> = spec.split(" ").map { token ->
        val symbol = token.drop(1)
        Eten41Key(token.first(), symbol.ifEmpty { null })
    }

    private fun symbols(spec: String): List<PunctuationSymbol> = spec.split(" ").map { label ->
        PunctuationSymbol(label, if (label == "…" || label == "—") label + label else label)
    }
}
