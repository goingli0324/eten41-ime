package com.muzigoing.eten41ime.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 與 iOS 版 ChewingEngineTests／KeyboardSessionTests 相同的案例，在 Android 模擬器上跑 */
@RunWith(AndroidJUnit4::class)
class ChewingEngineTest {
    private lateinit var engine: ChewingEngine

    @Before
    fun setUp() {
        engine = freshEngine()
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

    /** 定案按鍵表（docs/keymap-eten41.md）每一鍵單按，組字區出現對應注音 */
    @Test
    fun everyKeyMatchesDecidedKeymap() {
        val keymap = mapOf(
            'a' to "ㄚ", 'b' to "ㄅ", 'c' to "ㄒ", 'd' to "ㄉ", 'e' to "ㄧ", 'f' to "ㄈ", 'g' to "ㄐ",
            'h' to "ㄏ", 'i' to "ㄞ", 'j' to "ㄖ", 'k' to "ㄎ", 'l' to "ㄌ", 'm' to "ㄇ", 'n' to "ㄋ",
            'o' to "ㄛ", 'p' to "ㄆ", 'q' to "ㄟ", 'r' to "ㄜ", 's' to "ㄙ", 't' to "ㄊ", 'u' to "ㄩ",
            'v' to "ㄍ", 'w' to "ㄝ", 'x' to "ㄨ", 'y' to "ㄡ", 'z' to "ㄠ",
            '7' to "ㄑ", '8' to "ㄢ", '9' to "ㄣ", '0' to "ㄤ", '-' to "ㄥ", '=' to "ㄦ",
            ',' to "ㄓ", '.' to "ㄔ", '/' to "ㄕ", ';' to "ㄗ", '\'' to "ㄘ",
        )
        assertEquals(37, keymap.size)
        for ((key, symbol) in keymap) {
            assertEquals("按鍵 $key", symbol, engine.pendingBopomofo(key.toString()))
        }
    }

    @Test
    fun toneKeysStoreExpectedTone() {
        val expectations = mapOf("ma1" to "ㄇㄚ˙", "ma2" to "ㄇㄚˊ", "ma3" to "ㄇㄚˇ", "ma4" to "ㄇㄚˋ", "ma " to "ㄇㄚ")
        for ((keys, reading) in expectations) {
            assertEquals("按鍵 $keys", listOf(reading), engine.syllableReadings(keys))
        }
    }

    @Test
    fun smokeCasesCommitExpectedText() {
        val cases = mapOf(",x- x92" to "中文", ",x4e9 " to "注音", "e3te8 " to "倚天", "/x jx4fa3" to "輸入法")
        for ((keys, expected) in cases) {
            assertEquals("按鍵 $keys", expected, engine.commitText(keys))
        }
    }

    @Test
    fun shiftedPunctuationBecomesFullWidth() {
        val cases = mapOf("<" to "，", ">" to "。", "?" to "？", ":" to "：", "\"" to "；")
        for ((keys, expected) in cases) {
            assertEquals("Shift 鍵 $keys", expected, engine.commitText(keys))
        }
    }

    @Test
    fun choosingMiddleCharacterReplacesIt() {
        "/x jx4fa3".forEach { if (it == ' ') engine.pressSpace() else engine.pressKey(it) }
        assertEquals(listOf("輸", "入", "法"), engine.bufferCharacters)
        engine.moveCursor(1)
        val candidates = engine.candidates(CandidateDirection.STARTING_AT_CURSOR)
        assertTrue("候選要有「入」", candidates.any { it.text == "入" })
        val alternative = candidates.first { it.text == "辱" }
        assertTrue(engine.choose(alternative))
        assertEquals("辱", engine.bufferCharacters[1])
        engine.clear()
    }

    @Test
    fun lastWordCandidatesListLongestPhraseFirst() {
        "/x jx4fa3".forEach { if (it == ' ') engine.pressSpace() else engine.pressKey(it) }
        assertEquals("輸入法", engine.candidates().first().text)
        engine.clear()
    }

    @Test
    fun clearDoesNotLeakSyllables() {
        engine.syllableReadings("ma1")
        assertEquals(listOf("ㄇㄚˊ"), engine.syllableReadings("ma2"))
        assertNull(engine.takeCommittedText())
    }
}
