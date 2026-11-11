package com.muzigoing.eten41ime.keyboard

import android.content.Intent
import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 實體（藍牙）鍵盤：按鍵事件送進輸入法，照倚天41轉成注音。
 * 用 UiDevice.pressKeyCode 模擬實體鍵，跟螢幕鍵盤無關。
 */
@RunWith(AndroidJUnit4::class)
class HardwareKeyboardTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @Before
    fun enableEten41() {
        device.executeShellCommand("ime enable $IME_ID")
        device.executeShellCommand("ime set $IME_ID")
        waitUntilEten41IsDefault(device, IME_ID)
        val context = instrumentation.targetContext
        context.startActivity(
            context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        val field = device.wait(Until.findObject(By.desc("試打欄")), TIMEOUT)
        assertNotNull("找不到試打欄", field)
        field.click()
        ensureEten41KeyboardShown(device, IME_ID)
        device.waitForIdle()
    }

    @After
    fun restoreDefaultIme() {
        device.executeShellCommand("ime reset")
    }

    @Test
    fun typesChineseWithHardwareKeys() {
        // d x - 空白 c e 1 ＝ ㄉㄨㄥ ㄒㄧ˙ → 東西；Enter 送出組字區
        press(KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_ENTER)
        assertFieldText("東西")
    }

    @Test
    fun shiftPunctuationAndBackspace() {
        // Shift＋, → 全形逗號；組字區沒字時 Backspace 交給系統刪字
        press(KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_SPACE)
        device.pressKeyCode(KeyEvent.KEYCODE_COMMA, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)
        press(KeyEvent.KEYCODE_ENTER)
        assertFieldText("東，")
        press(KeyEvent.KEYCODE_DEL)
        assertFieldText("東")
    }

    @Test
    fun shiftTapTogglesEnglish() {
        // 單按一下 Shift 切英文，按鍵直接交給系統；再按一下切回中文
        press(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_SHIFT_LEFT)
        press(KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER)
        assertFieldText("ok東")
    }

    private fun press(vararg keyCodes: Int) {
        keyCodes.forEach {
            device.pressKeyCode(it)
            device.waitForIdle()
        }
    }

    private fun assertFieldText(expected: String) {
        if (device.wait(Until.hasObject(By.desc("試打欄").text(expected)), TIMEOUT)) return
        val actual = device.wait(Until.findObject(By.desc("試打欄")), TIMEOUT)?.text
        assertEquals(expected, actual)
    }

    private companion object {
        const val IME_ID = "app.muzigoing.eten41/com.muzigoing.eten41ime.ime.Eten41InputMethodService"
        const val TIMEOUT = 5_000L
    }
}
