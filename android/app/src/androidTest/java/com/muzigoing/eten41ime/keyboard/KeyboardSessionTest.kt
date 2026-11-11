package com.muzigoing.eten41ime.keyboard

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.muzigoing.eten41ime.engine.ChewingEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 與 iOS 版 KeyboardSessionTests 相同的案例 */
@RunWith(AndroidJUnit4::class)
class KeyboardSessionTest {
    private class RecordingOutput : KeyboardTextOutput {
        var text = ""
        var deleteCount = 0
        override fun insertText(text: String) { this.text += text }
        override fun deleteBackward() { deleteCount++ }
    }

    private lateinit var engine: ChewingEngine
    private lateinit var output: RecordingOutput
    private lateinit var session: KeyboardSession

    @Before
    fun setUp() {
        engine = freshEngine()
        output = RecordingOutput()
        session = KeyboardSession(engine, output)
    }

    /** 每個測試用一份全新的使用者詞庫：引擎會記住選過的字，共用會讓測試互相影響 */
    private fun freshEngine(): ChewingEngine {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val userDictionary = java.io.File.createTempFile("chewing-user-test", ".dat", context.cacheDir)
        userDictionary.delete()
        return ChewingEngine.fromAssets(context, userDictionary)
    }

    @After
    fun tearDown() = engine.close()

    private fun key(ascii: Char) = Eten41Layout.allKeys.first { it.ascii == ascii }

    private fun type(keys: String) = keys.forEach { if (it == ' ') session.tapSpace() else session.tap(key(it)) }

    @Test
    fun secondShiftTapLocksUntilThirdTap() {
        session.toggleShift()
        session.toggleShift()
        assertTrue(session.isShiftLocked)
        session.tap(key(','))
        session.tap(key('.'))
        assertEquals("鎖定時每個符號鍵都是全形標點", "，。", session.composingText)
        assertTrue(session.isShifted)
        session.toggleShift()
        assertFalse(session.isShifted)
        assertFalse(session.isShiftLocked)
        session.tap(key(','))
        assertEquals("，。ㄓ", session.composingText)
    }

    @Test
    fun shiftLockTypesUppercaseInEnglishMode() {
        session.toggleEnglishMode()
        session.toggleShift()
        session.toggleShift()
        session.tap(key('o'))
        session.tap(key('k'))
        assertEquals("OK", output.text)
    }

    @Test
    fun hardwareKeyDoesNotDisturbShiftLock() {
        session.toggleShift()
        session.toggleShift()
        session.tapHardwareKey(key('d'), shifted = false)
        assertTrue("實體鍵不該解除螢幕鍵盤的鎖定", session.isShiftLocked)
        assertEquals("ㄉ", session.composingText)
    }

    @Test
    fun hardwareShiftLetterTypesUppercaseAfterCommitting() {
        type("dx- ")
        session.tapHardwareKey(key('o'), shifted = true)
        assertEquals("東O", output.text)
        assertEquals("", session.composingText)
    }

    @Test
    fun hardwareKeyMapperFollowsUsKeyPositions() {
        val d = HardwareKeyMapper.map(android.view.KeyEvent.KEYCODE_D, 0)
        assertEquals(HardwareKeyAction.Key(key('d'), shifted = false), d)
        val comma = HardwareKeyMapper.map(android.view.KeyEvent.KEYCODE_COMMA, android.view.KeyEvent.META_SHIFT_ON)
        assertEquals(HardwareKeyAction.Key(key(','), shifted = true), comma)
        // Ctrl＋C 這類快捷鍵、方向鍵交給系統
        assertEquals(HardwareKeyAction.PassThrough, HardwareKeyMapper.map(android.view.KeyEvent.KEYCODE_C, android.view.KeyEvent.META_CTRL_ON))
        assertEquals(HardwareKeyAction.PassThrough, HardwareKeyMapper.map(android.view.KeyEvent.KEYCODE_DPAD_LEFT, 0))
    }

    @Test
    fun candidatesOnlyViewHasNoKeys() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var descriptions = emptyList<String>()
        instrumentation.runOnMainSync {
            val view = KeyboardView(instrumentation.targetContext, session, candidatesOnly = true)
            type("dx- ")
            descriptions = allDescriptions(view)
        }
        assertTrue("候選列要有組字區", "組字區 東" in descriptions)
        assertTrue("不該有螢幕鍵：$descriptions", descriptions.none { it == "D" || it == "換行" || it == "刪除" })
    }

    private fun allDescriptions(view: android.view.View): List<String> = buildList {
        view.contentDescription?.let { add(it.toString()) }
        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) addAll(allDescriptions(view.getChildAt(i)))
    }

    @Test
    fun layoutKeycapsMatchEngine() {
        val keys = Eten41Layout.allKeys.filter { it.symbol != null && it.ascii !in "1234" }
        assertEquals(37, keys.size)
        for (k in keys) assertEquals("鍵 ${k.ascii}", k.symbol, engine.pendingBopomofo(k.ascii.toString()))
        assertEquals(listOf(12, 10, 11, 10), Eten41Layout.rows.map { it.size })
    }

    @Test
    fun shiftedKeycapsMatchEngine() {
        val keys = Eten41Layout.allKeys.filter { it.shiftedAscii != null }
        assertEquals(17, keys.size)
        for (k in keys) assertEquals("Shift＋${k.ascii}", k.shiftedFullWidth, engine.commitText(k.shiftedAscii.toString()))
    }

    @Test
    fun typingThenReturnCommitsToOutput() {
        type("dx- ce1")
        assertEquals("東西", session.composingText)
        assertEquals("", output.text)
        session.tapReturn()
        assertEquals("東西", output.text)
        assertTrue(session.candidates.isEmpty())
    }

    @Test
    fun tappingMiddleCharacterReplacesIt() {
        type("/x jx4fa3")
        session.focusCharacter(1)
        assertEquals(1, session.focusedIndex)
        session.choose(session.candidates.first { it.text == "辱" })
        assertEquals("辱", session.composingCharacters[1])
        assertNull("選完游標回到最後", session.focusedIndex)
    }

    @Test
    fun typingAfterFocusAppendsAtEnd() {
        type("/x jx4fa3")
        session.focusCharacter(0)
        type("dx- ")
        assertEquals("輸入法東", session.composingText)
    }

    @Test
    fun backspaceEditsCompositionBeforeDocument() {
        type("dx")
        session.tapBackspace()
        assertEquals("ㄉ", session.composingText)
        session.tapBackspace()
        assertEquals(0, output.deleteCount)
        session.tapBackspace()
        assertEquals(1, output.deleteCount)
    }

    @Test
    fun shiftPunctuationIsOneShot() {
        session.toggleShift()
        session.tap(key(','))
        session.tap(key(','))
        assertEquals("，ㄓ", session.composingText)
        assertFalse(session.isShifted)
    }

    @Test
    fun symbolPanelCommitsCompositionThenInserts() {
        type("dx- ce1")
        session.toggleSymbols()
        session.tapSymbol(Eten41Layout.punctuationRows.flatten().first { it.label == "、" })
        assertEquals("東西、", output.text)
        assertFalse(session.isShowingSymbols)
        session.tapSymbol(Eten41Layout.punctuationRows.flatten().first { it.label == "…" })
        assertEquals("東西、……", output.text)
    }

    @Test
    fun englishModeCommitsPendingTextAndShiftGivesUppercase() {
        type("e3te8 ")
        session.toggleEnglishMode()
        assertEquals("倚天", output.text)
        session.toggleShift()
        type("ok")
        assertEquals("倚天Ok", output.text)
    }
}
