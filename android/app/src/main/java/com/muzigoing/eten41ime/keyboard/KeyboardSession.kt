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
    /** 實體鍵盤按 ↓ 進入選字後，反白的是第幾個候選（null＝不在選字中） */
    var highlightedCandidateIndex: Int? = null; private set
    val isPicking: Boolean get() = highlightedCandidateIndex != null
    var isEnglishMode = false; private set
    /** ⇧ 三段循環：按一下只作用在下一個鍵，再按一下鎖定，第三下解除 */
    enum class ShiftState { OFF, ONCE, LOCKED }
    var shiftState = ShiftState.OFF; private set
    val isShifted: Boolean get() = shiftState != ShiftState.OFF
    val isShiftLocked: Boolean get() = shiftState == ShiftState.LOCKED
    var isShowingSymbols = false; private set

    /** 狀態變了就通知畫面重畫 */
    var onChange: (() -> Unit)? = null

    val composingText: String get() = composingCharacters.joinToString("") + pendingBopomofo

    fun attach(output: KeyboardTextOutput) {
        this.output = output
    }

    fun tap(key: Eten41Key) {
        val shifted = isShifted
        if (shiftState == ShiftState.ONCE) shiftState = ShiftState.OFF
        type(key, shifted)
    }

    private fun type(key: Eten41Key, shifted: Boolean) {
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

    /** 組字區有沒有字（實體鍵盤：沒字時空白／退格／Enter 交給系統） */
    val hasComposition: Boolean get() = engine.hasComposition

    /**
     * 實體鍵盤的鍵（只在中文模式呼叫；英文模式由輸入法服務直接交給系統）。
     * Shift＋字母打大寫英文（同微軟注音），Shift＋其他鍵打全形標點。
     */
    fun tapHardwareKey(key: Eten41Key, shifted: Boolean) {
        if (shifted && key.shiftedAscii == null) {
            commitComposition()
            output?.insertText(key.ascii.uppercase())
            notifyChanged()
            return
        }
        // 實體鍵盤自己有 Shift，不動螢幕鍵盤的 ⇧ 狀態
        type(key, shifted)
    }

    fun toggleShift() {
        shiftState = when (shiftState) {
            ShiftState.OFF -> ShiftState.ONCE
            ShiftState.ONCE -> ShiftState.LOCKED
            ShiftState.LOCKED -> ShiftState.OFF
        }
        notifyChanged()
    }

    fun toggleSymbols() {
        isShowingSymbols = !isShowingSymbols
        shiftState = ShiftState.OFF
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

    // ---- 實體鍵盤的方向鍵（比照微軟注音）----

    /** ← →：在組字區的字之間移動；從最後往左是最後一個字，移過最後一個字就回到「游標在最後」 */
    fun moveFocus(delta: Int) {
        if (pendingBopomofo.isNotEmpty() || composingCharacters.isEmpty()) return
        val last = composingCharacters.lastIndex
        val target = (focusedIndex ?: (last + 1)) + delta
        when {
            target > last -> releaseFocus()
            else -> target.coerceAtLeast(0).let {
                engine.moveCursor(it)
                focusedIndex = it
            }
        }
        refresh()
    }

    /** ↓：進入選字，反白第一個候選 */
    fun startPicking() {
        if (candidates.isEmpty()) return
        highlightedCandidateIndex = 0
        notifyChanged()
    }

    /** 選字中的 ← →：移動反白的候選 */
    fun moveHighlight(delta: Int) {
        val current = highlightedCandidateIndex ?: return
        highlightedCandidateIndex = (current + delta).coerceIn(candidates.indices)
        notifyChanged()
    }

    /** 選字中的 Enter／空白：選定反白的候選 */
    fun confirmHighlighted() {
        val candidate = highlightedCandidateIndex?.let(candidates::getOrNull) ?: return
        choose(candidate)
    }

    /** 選字中的數字鍵 1～9：直接選第幾個；沒有那麼多候選就不動 */
    fun pickByNumber(number: Int) {
        if (!isPicking) return
        candidates.getOrNull(number - 1)?.let(::choose)
    }

    /** Esc／↑：離開選字，組字區不變 */
    fun cancelPicking() {
        if (!isPicking) return
        highlightedCandidateIndex = null
        notifyChanged()
    }

    /** 選字後游標回到最後，接著打的字接在後面 */
    fun choose(candidate: ChewingCandidate) {
        engine.choose(candidate)
        releaseFocus()
        flush()
    }

    /** 中英切換；切到英文前先把組字區上字 */
    fun toggleEnglishMode() {
        if (!isEnglishMode) commitComposition()
        isEnglishMode = !isEnglishMode
        notifyChanged()
    }

    /** 換了輸入框：丟掉沒送出的組字，避免帶到別的欄位 */
    fun reset() {
        engine.clear()
        focusedIndex = null
        shiftState = ShiftState.OFF
        isShowingSymbols = false
        refresh()
    }

    private fun commitComposition() {
        if (!engine.hasComposition) return
        releaseFocus()
        engine.pressEnter()
        flush()
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
        // 候選換了一批，原本反白的位置就沒意義了
        highlightedCandidateIndex = null
        candidates = engine.candidates(
            if (focusedIndex == null) CandidateDirection.ENDING_AT_CURSOR else CandidateDirection.STARTING_AT_CURSOR,
        )
        notifyChanged()
    }

    private fun notifyChanged() {
        onChange?.invoke()
    }
}
