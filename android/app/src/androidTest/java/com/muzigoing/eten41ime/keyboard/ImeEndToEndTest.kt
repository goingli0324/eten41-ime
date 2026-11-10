package com.muzigoing.eten41ime.keyboard

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 端到端：把系統輸入法切到倚天41，在 App 的試打欄用螢幕鍵盤打字。
 * 同 iOS 版 KeyboardExtensionUITests：打「東西」→ 點組字區「東」改成「冬」→ ⇧＋, → 符號面板「、」
 */
@RunWith(AndroidJUnit4::class)
class ImeEndToEndTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @Before
    fun enableEten41() {
        device.executeShellCommand("ime enable $IME_ID")
        device.executeShellCommand("ime set $IME_ID")
    }

    @After
    fun restoreDefaultIme() {
        device.executeShellCommand("ime reset")
    }

    @Test
    fun typeInTextFieldWithEten41Keyboard() {
        val context = instrumentation.targetContext
        context.startActivity(
            context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        val field = device.wait(Until.findObject(By.desc("試打欄")), TIMEOUT)
        assertNotNull("找不到試打欄", field)
        field.click()

        tapAll("D", "X", "-", "空白", "C", "E", "1")
        tap("組字區 東")
        tap("冬")
        tap("換行")
        assertFieldText("冬西")

        tap("Shift")
        tap("，")
        tap("換行")
        tap("標點符號")
        tap("、")
        assertFieldText("冬西，、")
    }

    /**
     * 商店截圖用：平常跳過；要拍時加 `-Pandroid.testInstrumentationRunnerArguments.storeScreenshots=1`，
     * 圖存在 /data/local/tmp/eten41-store/（測試跑完 App 會被移除，不能存 App 自己的資料夾），再用 adb pull 取出
     */
    @Test
    fun storeScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("storeScreenshots") == "1")
        val context = instrumentation.targetContext
        device.executeShellCommand("mkdir -p $STORE_DIR")
        context.startActivity(
            context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        val field = device.wait(Until.findObject(By.desc("試打欄")), TIMEOUT)
        assertNotNull("找不到試打欄", field)
        device.waitForIdle()
        shot("store-4-home.png")

        field.click()
        tapAll(",", "X", "-", "空白", "X", "9", "2", "換行")
        tapAll(",", "X", "4", "E", "9", "空白", "/", "X", "空白", "J", "X", "4", "F", "A", "3")
        assertNotNull("組字區沒有「注音輸入法」", device.wait(Until.findObject(By.desc("組字區 法")), TIMEOUT))
        shot("store-1-typing.png")

        tapAll("換行", "D", "X", "-", "空白", "C", "E", "1", "組字區 東")
        assertNotNull(device.wait(Until.findObject(By.desc("冬")), TIMEOUT))
        shot("store-2-fix.png")

        tapAll("冬", "換行", "Shift", "，", "換行", "標點符號")
        assertNotNull(device.wait(Until.findObject(By.desc("、")), TIMEOUT))
        shot("store-3-symbols.png")
    }

    private fun shot(name: String) {
        device.waitForIdle()
        device.executeShellCommand("screencap -p $STORE_DIR/$name")
    }

    /** 鍵盤重畫時無障礙樹會短暫抓不到欄位：等到內容符合，逾時才報實際內容 */
    private fun assertFieldText(expected: String) {
        if (device.wait(Until.hasObject(By.desc("試打欄").text(expected)), TIMEOUT)) return
        val actual = device.wait(Until.findObject(By.desc("試打欄")), TIMEOUT)?.text
        if (actual != expected) {
            // 失敗當下的畫面與 UI 結構，留在 /data/local/tmp 給 adb pull 查原因
            device.executeShellCommand("screencap -p /data/local/tmp/eten41-e2e-fail.png")
            device.executeShellCommand("uiautomator dump /data/local/tmp/eten41-e2e-fail.xml")
        }
        assertEquals(expected, actual)
    }

    private fun tapAll(vararg descriptions: String) = descriptions.forEach(::tap)

    /** 鍵盤每次狀態改變都會重建按鍵，找到和點擊之間可能過期：過期就重找再點 */
    private fun tap(description: String) {
        repeat(3) {
            val target = device.wait(Until.findObject(By.desc(description)), TIMEOUT)
            assertNotNull("找不到「$description」", target)
            try {
                target.click()
                device.waitForIdle()
                return
            } catch (_: StaleObjectException) {
                device.waitForIdle()
            }
        }
        throw AssertionError("「$description」一直過期，點不到")
    }

    private companion object {
        const val IME_ID = "app.muzigoing.eten41/com.muzigoing.eten41ime.ime.Eten41InputMethodService"
        const val TIMEOUT = 5_000L
        const val STORE_DIR = "/data/local/tmp/eten41-store"
    }
}
