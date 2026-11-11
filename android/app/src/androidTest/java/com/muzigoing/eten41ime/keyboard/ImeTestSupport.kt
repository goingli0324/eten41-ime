package com.muzigoing.eten41ime.keyboard

import androidx.test.uiautomator.UiDevice

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
