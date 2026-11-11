package com.muzigoing.eten41ime.keyboard

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * `ime set` 回來時系統不一定已經切過去；上一個測試剛 `ime reset` 時特別容易還停在 Gboard，
 * 接著點欄位就會叫出 Gboard（2026-11-11 全套測試偶發「找不到 D」，截圖是 Gboard）
 */
fun waitUntilEten41IsDefault(device: UiDevice, imeId: String, timeoutMs: Long = 5_000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (device.executeShellCommand("settings get secure default_input_method").trim() == imeId) return
        Thread.sleep(100)
    }
    throw AssertionError("系統預設輸入法沒有切到 $imeId")
}

/**
 * 點了輸入框之後確認出來的真的是41注音的鍵盤。
 * 上一個測試 @After 的 `ime reset` 可能比這個測試的 `ime set` 晚生效（設定值看起來已切換，出來的卻是 Gboard，
 * 2026-11-11 失敗截圖確認）：不是就重設一次輸入法、收起鍵盤再點一次。
 */
fun ensureEten41KeyboardShown(device: UiDevice, imeId: String, fieldDescription: String = "試打欄") {
    repeat(3) {
        if (device.wait(Until.hasObject(By.desc("空白")), 3_000)) return
        device.executeShellCommand("ime set $imeId")
        waitUntilEten41IsDefault(device, imeId)
        device.pressBack()
        device.wait(Until.findObject(By.desc(fieldDescription)), 5_000)?.click()
    }
    throw AssertionError("點了輸入框，出來的不是41注音鍵盤")
}
