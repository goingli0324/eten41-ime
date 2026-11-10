package com.muzigoing.eten41ime.keyboard

import com.muzigoing.eten41ime.engine.CandidateDirection
import com.muzigoing.eten41ime.engine.ChewingCandidate
import com.muzigoing.eten41ime.engine.ChewingEngine

/** 鍵盤把字送去哪裡：輸入法服務是目前的輸入框，App 試打頁是畫面上的文字。 */
interface KeyboardTextOutput {
    fun insertText(text: String)
    fun deleteBackward()
}

/** 螢幕鍵盤的狀態與按鍵邏輯（與 iOS 版 Shared/KeyboardSession.swift 相同） */
class KeyboardSession(private val engine: ChewingEngine, private var output: KeyboardTextOutput?) {
    /** 組字區已轉好的字，一格一個（畫面上每個字可以點） */
    var composingCharacters: List<String> = emptyList(); private set
    /** 還在打、沒轉成字的注音 */
    var pendingBopomofo: String = ""; private set
    /** 使用者點了組字區第幾個字（null＝游標在最後，候選是最後一個詞） */
    var focusedIndex: Int? = null; private set
    var candidates: List<ChewingCandidate> = emptyList(); private set
    var isEnglishMode = false; private set
    /** ⇧ 只作用在下一個鍵 */
    var isShifted = false; private set
    var isShowingSymbols = false; private set

    /** 狀態變了就通知畫面重畫 */
    var onChange: (() -> Unit)? = null

    val composingText: String get() = composingCharacters.joinToString("") + pendingBopomofo

    fun attach(output: KeyboardTextOutput) {
        this.output = output
    }

    fun tap(key: Eten41Key) {
        val shifted = isShifted
        isShifted = false
        if (isEnglishMode) {
            output?.insertText(if (shifted) key.englishShiftedText else key.ascii.toString())
            notifyChanged()
            return
        }
        releaseFocus()
        // 中文模式 ⇧ 只改非字母鍵：送 Shift 後的 ASCII，引擎轉成全形標點（如 Shift＋, →，）
        engine.pressKey(if (shifted) key.shiftedAscii ?: key.ascii else key.ascii)
        flush()
    }

    fun toggleShift() {
        isShifted = !isShifted
        notifyChanged()
    }

    fun toggleSymbols() {
        isShowingSymbols = !isShowingSymbols
        isShifted = false
        notifyChanged()
    }

    /** 符號面板：先把組字區上字，再插入標點，然後回到注音鍵 */
    fun tapSymbol(symbol: PunctuationSymbol) {
        if (engine.hasComposition) {
            releaseFocus()
            engine.pressEnter()
            flush()
        }
        output?.insertText(symbol.text)
        isShowingSymbols = false
        notifyChanged()
    }

    fun tapSpace() {
        if (isEnglishMode || !engine.hasComposition) {
            output?.insertText(" ")
            return
        }
        releaseFocus()
        engine.pressSpace()
        flush()
    }

    fun tapBackspace() {
        if (isEnglishMode || !engine.hasComposition) {
            output?.deleteBackward()
            return
        }
        releaseFocus()
        engine.pressBackspace()
        flush()
    }

    fun tapReturn() {
        if (isEnglishMode || !engine.hasComposition) {
            output?.insertText("\n")
            return
        }
        releaseFocus()
        engine.pressEnter()
        flush()
    }

    /** 點組字區第 index 個字＝微軟注音把游標移過去按 ↓；再點同一字取消；還在打注音時不能點 */
    fun focusCharacter(index: Int) {
        if (pendingBopomofo.isNotEmpty() || index !in composingCharacters.indices) return
        if (focusedIndex == index) {
            releaseFocus()
        } else {
            engine.moveCursor(index)
            focusedIndex = index
        }
        refresh()
    }

    /** 選字後游標回到最後，接著打的字接在後面 */
    fun choose(candidate: ChewingCandidate) {
        engine.choose(candidate)
        releaseFocus()
        flush()
    }

    /** 中英切換；切到英文前先把組字區上字 */
    fun toggleEnglishMode() {
        if (!isEnglishMode && engine.hasComposition) {
            releaseFocus()
            engine.pressEnter()
            flush()
        }
        isEnglishMode = !isEnglishMode
        notifyChanged()
    }

    /** 換了輸入框：丟掉沒送出的組字，避免帶到別的欄位 */
    fun reset() {
        engine.clear()
        focusedIndex = null
        isShifted = false
        isShowingSymbols = false
        refresh()
    }

    private fun releaseFocus() {
        if (focusedIndex == null) return
        engine.moveCursorToEnd()
        focusedIndex = null
    }

    private fun flush() {
        engine.takeCommittedText()?.let { output?.insertText(it) }
        refresh()
    }

    private fun refresh() {
        composingCharacters = engine.bufferCharacters
        pendingBopomofo = engine.pendingBopomofo
        focusedIndex?.let { if (it !in composingCharacters.indices) focusedIndex = null }
        candidates = engine.candidates(
            if (focusedIndex == null) CandidateDirection.ENDING_AT_CURSOR else CandidateDirection.STARTING_AT_CURSOR,
        )
        notifyChanged()
    }

    private fun notifyChanged() {
        onChange?.invoke()
    }
}
