package com.muzigoing.eten41ime.engine

import android.content.Context
import java.io.Closeable
import java.io.File

/** 候選字：libchewing 分好幾張清單（最長詞在前、單字在後），選字時要回到同一張清單的同一格。 */
data class ChewingCandidate(
    val listIndex: Int,
    val choiceIndex: Int,
    val text: String,
    val direction: CandidateDirection,
)

/**
 * phraseChoiceRearward：
 * - ENDING_AT_CURSOR：列「結束在游標前那個字」的詞（打字中改最後一個詞）
 * - STARTING_AT_CURSOR：列「從游標所在字開始」的詞再接單字（＝微軟注音把游標移過去按 ↓）
 */
enum class CandidateDirection { ENDING_AT_CURSOR, STARTING_AT_CURSOR }

/** libchewing 的包裝，固定倚天41（KB_ET）。行為與 iOS 版 Shared/ChewingEngine.swift 一致。 */
class ChewingEngine(dictionaryDirectory: File, userDictionary: File) : Closeable {
    private var handle: Long

    init {
        val wordDat = File(dictionaryDirectory, "word.dat")
        require(wordDat.exists()) { "找不到詞庫：${wordDat.path}" }
        handle = ChewingNative.create(dictionaryDirectory.path, userDictionary.path)
        check(handle != 0L) { "libchewing 初始化失敗" }
        check(ChewingNative.setKBType(handle, ChewingNative.kbStrToNum("KB_ET")) == 0) { "KB_ET 設定失敗" }
        ChewingNative.setMaxChiSymbolLen(handle, 16)
        // 預設 Esc 只關候選窗、不清組字區
        ChewingNative.setEscCleanAllBuf(handle, 1)
    }

    override fun close() {
        if (handle != 0L) {
            ChewingNative.delete(handle)
            handle = 0L
        }
    }

    // ---- 互動輸入（鍵盤用） ----

    val bufferCharacters: List<String>
        get() = decode(ChewingNative.bufferString(handle)).codePointStrings()

    val pendingBopomofo: String get() = decode(ChewingNative.bopomofoString(handle))

    val composingText: String get() = bufferCharacters.joinToString("") + pendingBopomofo

    val hasComposition: Boolean
        get() = ChewingNative.bufferCheck(handle) != 0 || ChewingNative.bopomofoCheck(handle) != 0

    val cursorPosition: Int get() = ChewingNative.cursorCurrent(handle)

    fun pressKey(ascii: Char) = ChewingNative.handleDefault(handle, ascii.code)
    fun pressSpace() = ChewingNative.handleSpace(handle)
    fun pressBackspace() = ChewingNative.handleBackspace(handle)
    fun pressEnter() = ChewingNative.handleEnter(handle)

    /** 上一個動作造成的上屏文字；libchewing 下一個按鍵就清掉，每個動作後要馬上讀 */
    fun takeCommittedText(): String? = ChewingNative.takeCommit(handle)?.let(::decode)?.ifEmpty { null }

    fun moveCursor(position: Int) {
        val target = position.coerceIn(0, bufferCharacters.size)
        var guard = bufferCharacters.size + 2
        while (cursorPosition > target && guard-- > 0) ChewingNative.handleLeft(handle)
        while (cursorPosition < target && guard-- > 0) ChewingNative.handleRight(handle)
    }

    fun moveCursorToEnd() = moveCursor(bufferCharacters.size)

    fun candidates(direction: CandidateDirection = CandidateDirection.ENDING_AT_CURSOR, limit: Int = 80): List<ChewingCandidate> {
        if (ChewingNative.bopomofoCheck(handle) != 0 || ChewingNative.bufferCheck(handle) == 0) return emptyList()
        apply(direction)
        if (ChewingNative.candOpen(handle) != 0) return emptyList()
        try {
            ChewingNative.candListFirst(handle)
            val result = mutableListOf<ChewingCandidate>()
            var listIndex = 0
            do {
                val total = ChewingNative.candTotalChoice(handle)
                for (choiceIndex in 0 until total) {
                    if (result.size >= limit) break
                    val text = ChewingNative.candStringAt(handle, choiceIndex)?.let(::decode) ?: continue
                    result += ChewingCandidate(listIndex, choiceIndex, text, direction)
                }
                listIndex++
            } while (result.size < limit &&
                ChewingNative.candListHasNext(handle) != 0 &&
                ChewingNative.candListNext(handle) == 0)
            return result
        } finally {
            ChewingNative.candClose(handle)
        }
    }

    fun choose(candidate: ChewingCandidate): Boolean {
        apply(candidate.direction)
        if (ChewingNative.candOpen(handle) != 0) return false
        try {
            ChewingNative.candListFirst(handle)
            repeat(candidate.listIndex) {
                if (ChewingNative.candListNext(handle) != 0) return false
            }
            return ChewingNative.candChooseByIndex(handle, candidate.choiceIndex) == 0
        } finally {
            ChewingNative.candClose(handle)
        }
    }

    // ---- 一次送一串按鍵（測試用） ----

    fun commitText(keys: String): String {
        press(keys)
        pressEnter()
        val committed = takeCommittedText().orEmpty()
        clear()
        return committed
    }

    fun pendingBopomofo(keys: String): String {
        press(keys)
        val bopomofo = pendingBopomofo
        clear()
        return bopomofo
    }

    fun syllableReadings(keys: String): List<String> {
        press(keys)
        val readings = ChewingNative.syllableReadings(handle).map { decode(it) }
        clear()
        return readings
    }

    fun clear() {
        ChewingNative.handleEsc(handle)
        ChewingNative.handleEsc(handle)
    }

    private fun press(keys: String) {
        for (character in keys) if (character == ' ') pressSpace() else pressKey(character)
    }

    private fun apply(direction: CandidateDirection) {
        ChewingNative.setPhraseChoiceRearward(handle, if (direction == CandidateDirection.ENDING_AT_CURSOR) 1 else 0)
    }

    companion object {
        /**
         * 用 App 打包的詞庫（第一次會從 assets 複製到私有資料夾）建立引擎。
         * 使用者詞庫會記住選過的字（例如選過「冬」之後 ㄉㄨㄥ 先出冬）；測試要傳各自的暫存檔，避免互相影響。
         */
        fun fromAssets(
            context: Context,
            userDictionary: File = File(context.filesDir, "chewing-user.dat"),
        ): ChewingEngine {
            val dictionaryDirectory = DictionaryInstaller.ensureInstalled(context)
            return ChewingEngine(dictionaryDirectory, userDictionary)
        }

        private fun decode(bytes: ByteArray?): String = bytes?.toString(Charsets.UTF_8).orEmpty()

        /** 依 code point 切字：擴充漢字是兩個 UTF-16 單位，不能用 String 的 char 切 */
        private fun String.codePointStrings(): List<String> =
            codePoints().toArray().map { String(Character.toChars(it)) }
    }
}
