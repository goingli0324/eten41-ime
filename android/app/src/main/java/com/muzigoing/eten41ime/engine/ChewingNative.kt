package com.muzigoing.eten41ime.engine

/**
 * libchewing C API 的 JNI 對應（實作在 android/jni/chewing_jni.c）。
 * 只做一對一轉接；字串以 UTF-8 ByteArray 回傳，由 [ChewingEngine] 解碼。
 */
internal object ChewingNative {
    init {
        System.loadLibrary("eten41jni")
    }

    @JvmStatic external fun create(dictDir: String, userPath: String): Long
    @JvmStatic external fun delete(handle: Long)
    @JvmStatic external fun kbStrToNum(name: String): Int
    @JvmStatic external fun setKBType(handle: Long, type: Int): Int
    @JvmStatic external fun setMaxChiSymbolLen(handle: Long, length: Int)
    @JvmStatic external fun setEscCleanAllBuf(handle: Long, on: Int)
    @JvmStatic external fun setPhraseChoiceRearward(handle: Long, on: Int)

    @JvmStatic external fun handleDefault(handle: Long, key: Int)
    @JvmStatic external fun handleSpace(handle: Long)
    @JvmStatic external fun handleBackspace(handle: Long)
    @JvmStatic external fun handleEnter(handle: Long)
    @JvmStatic external fun handleEsc(handle: Long)
    @JvmStatic external fun handleLeft(handle: Long)
    @JvmStatic external fun handleRight(handle: Long)

    @JvmStatic external fun bufferString(handle: Long): ByteArray?
    @JvmStatic external fun bopomofoString(handle: Long): ByteArray?
    @JvmStatic external fun bufferCheck(handle: Long): Int
    @JvmStatic external fun bopomofoCheck(handle: Long): Int
    @JvmStatic external fun cursorCurrent(handle: Long): Int
    @JvmStatic external fun takeCommit(handle: Long): ByteArray?

    @JvmStatic external fun candOpen(handle: Long): Int
    @JvmStatic external fun candClose(handle: Long): Int
    @JvmStatic external fun candListFirst(handle: Long): Int
    @JvmStatic external fun candListNext(handle: Long): Int
    @JvmStatic external fun candListHasNext(handle: Long): Int
    @JvmStatic external fun candTotalChoice(handle: Long): Int
    @JvmStatic external fun candStringAt(handle: Long, index: Int): ByteArray?
    @JvmStatic external fun candChooseByIndex(handle: Long, index: Int): Int

    @JvmStatic external fun syllableReadings(handle: Long): Array<ByteArray?>
}
