package com.muzigoing.eten41ime.keyboard

import android.view.KeyEvent

/** 實體鍵盤一次按鍵要輸入法做什麼（不認得或帶 Ctrl／Alt／Meta 的鍵一律交給系統） */
sealed interface HardwareKeyAction {
    /** 倚天41的鍵；shifted＝有按 Shift */
    data class Key(val key: Eten41Key, val shifted: Boolean) : HardwareKeyAction
    data object Space : HardwareKeyAction
    data object Backspace : HardwareKeyAction
    data object Enter : HardwareKeyAction
    /** 方向鍵與 Esc：組字區有字時用來移動、選字；沒字時交給系統 */
    data object Left : HardwareKeyAction
    data object Right : HardwareKeyAction
    data object Down : HardwareKeyAction
    data object Up : HardwareKeyAction
    data object Escape : HardwareKeyAction
    data object PassThrough : HardwareKeyAction
}

/** 把 Android 按鍵碼換成倚天41的鍵。只看按鍵位置（美式排列），不管系統的實體鍵盤語言設定。 */
object HardwareKeyMapper {
    private val keysByAscii: Map<Char, Eten41Key> = Eten41Layout.allKeys.associateBy { it.ascii }

    private val asciiByKeyCode: Map<Int, Char> = buildMap {
        ('a'..'z').forEachIndexed { index, letter -> put(KeyEvent.KEYCODE_A + index, letter) }
        ('0'..'9').forEachIndexed { index, digit -> put(KeyEvent.KEYCODE_0 + index, digit) }
        put(KeyEvent.KEYCODE_MINUS, '-')
        put(KeyEvent.KEYCODE_EQUALS, '=')
        put(KeyEvent.KEYCODE_SEMICOLON, ';')
        put(KeyEvent.KEYCODE_APOSTROPHE, '\'')
        put(KeyEvent.KEYCODE_COMMA, ',')
        put(KeyEvent.KEYCODE_PERIOD, '.')
        put(KeyEvent.KEYCODE_SLASH, '/')
    }

    fun map(keyCode: Int, metaState: Int): HardwareKeyAction {
        val shortcutMeta = KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or KeyEvent.META_META_ON
        if (metaState and shortcutMeta != 0) return HardwareKeyAction.PassThrough
        return when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> HardwareKeyAction.Space
            KeyEvent.KEYCODE_DEL -> HardwareKeyAction.Backspace
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> HardwareKeyAction.Enter
            KeyEvent.KEYCODE_DPAD_LEFT -> HardwareKeyAction.Left
            KeyEvent.KEYCODE_DPAD_RIGHT -> HardwareKeyAction.Right
            KeyEvent.KEYCODE_DPAD_DOWN -> HardwareKeyAction.Down
            KeyEvent.KEYCODE_DPAD_UP -> HardwareKeyAction.Up
            KeyEvent.KEYCODE_ESCAPE -> HardwareKeyAction.Escape
            else -> {
                val key = asciiByKeyCode[keyCode]?.let(keysByAscii::get) ?: return HardwareKeyAction.PassThrough
                HardwareKeyAction.Key(key, shifted = metaState and KeyEvent.META_SHIFT_ON != 0)
            }
        }
    }
}
