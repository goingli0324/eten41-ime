package com.muzigoing.eten41ime.ime

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.muzigoing.eten41ime.engine.ChewingEngine
import com.muzigoing.eten41ime.keyboard.KeyboardSession
import com.muzigoing.eten41ime.keyboard.KeyboardTextOutput
import com.muzigoing.eten41ime.keyboard.KeyboardView

/** Android 系統輸入法：載入引擎，掛上倚天41鍵盤，字送進目前的輸入框。 */
class Eten41InputMethodService : InputMethodService(), KeyboardTextOutput {
    private var engine: ChewingEngine? = null
    private var session: KeyboardSession? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val created = ChewingEngine.fromAssets(this)
            engine = created
            session = KeyboardSession(created, this)
        } catch (error: Exception) {
            Log.e(TAG, "引擎載入失敗", error)
        }
    }

    override fun onDestroy() {
        engine?.close()
        engine = null
        session = null
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val current = session
            // 詞庫載不起來時至少顯示原因，使用者可以從系統切回別的輸入法
            ?: return TextView(this).apply { text = "41注音 詞庫載入失敗，請重新安裝 App" }
        return KeyboardView(
            context = this,
            session = current,
            showsGlobeKey = shouldOfferSwitchingToNextInputMethod(),
            onGlobe = { switchToNextInputMethod(false) },
        )
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (!restarting) session?.reset()
    }

    override fun insertText(text: String) {
        val connection = currentInputConnection ?: return
        if (text == "\n" && performEditorActionIfAny()) return
        connection.commitText(text, 1)
    }

    override fun deleteBackward() {
        // 用按鍵事件刪：輸入框自己處理擴充漢字（兩個 UTF-16 單位）與選取範圍
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    /** 單行輸入框的換行鍵應該觸發「送出／搜尋／下一個」 */
    private fun performEditorActionIfAny(): Boolean {
        val info = currentInputEditorInfo ?: return false
        val isMultiLine = info.inputType and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE != 0
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        val noEnterAction = info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        if (isMultiLine || noEnterAction || action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
            return false
        }
        return currentInputConnection?.performEditorAction(action) ?: false
    }

    private companion object {
        const val TAG = "Eten41IME"
    }
}
