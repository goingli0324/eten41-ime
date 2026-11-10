package com.muzigoing.eten41ime

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** App 主畫面：怎麼啟用鍵盤、試打欄、授權與原始碼（本 App MIT；libchewing 的 LGPL 散布義務）。 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        content.addView(text("41注音輸入法", 26f))
        content.addView(section("開啟41注音鍵盤"))
        content.addView(text("用倚天41鍵盤排列打注音。\n1. 按下方「開啟輸入法設定」，把「41注音」打開。\n2. 回到這裡按「切換輸入法」，選「41注音」。", 16f))
        content.addView(button("開啟輸入法設定") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
        content.addView(button("切換輸入法") {
            getSystemService(InputMethodManager::class.java).showInputMethodPicker()
        })

        content.addView(section("試打"))
        content.addView(EditText(this).apply {
            hint = "點這裡用41注音打字"
            contentDescription = "試打欄"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
        })

        content.addView(section("授權與原始碼"))
        content.addView(text(LICENSE_SUMMARY, 14f))
        content.addView(button("GNU LGPL 2.1 全文") { showAsset("licenses/libchewing-COPYING.txt", "LGPL 2.1") })
        content.addView(button("libchewing 作者名單") { showAsset("licenses/libchewing-AUTHORS.txt", "作者") })

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(content)
        })
    }

    private fun showAsset(path: String, title: String) {
        val body = runCatching { assets.open(path).bufferedReader().use { it.readText() } }
            .getOrElse { "找不到 $path，請重新建置 App。" }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(body)
            .setPositiveButton("關閉", null)
            .show()
    }

    private fun section(title: String) = text(title, 18f).apply {
        setPadding(0, dp(24), 0, dp(8))
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun text(value: String, sizeSp: Float) = TextView(this).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextIsSelectable(true)
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val LICENSE_SUMMARY =
            "本 App 的程式碼以 MIT 授權開源，原始碼：https://github.com/goingli0324/eten41-ime\n\n" +
                "本 App 使用 libchewing v0.13.1（新酷音輸入法核心）與 libchewing-data（詞庫），" +
                "授權皆為 GNU LGPL 2.1 或更新版本。原始碼：https://codeberg.org/chewing/libchewing\n\n" +
                "本 App 以靜態連結使用 libchewing，未修改其原始碼。本 App 的完整原始碼公開在上方 GitHub，" +
                "你可以替換 libchewing 後自行重新建置。依 LGPL 2.1 第 6 條，你也可以向提供此 App 的人索取本 App 的原始碼或目的檔，以便替換 libchewing 後重新連結。" +
                "此提供自你取得本 App 起三年內有效。"
    }
}
