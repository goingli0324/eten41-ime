// libchewing C API 的 JNI 薄橋接：一個 Kotlin 呼叫對一個 C 函式，邏輯全部留在 Kotlin。
// 對應 Kotlin：com.muzigoing.eten41ime.engine.ChewingNative（object + @JvmStatic external）
// 字串一律回傳 UTF-8 byte[]，由 Kotlin 解碼：JNI 的 NewStringUTF 是 modified UTF-8，
// 遇到 BMP 以外的字（擴充漢字、emoji）會出錯。
#include <jni.h>
#include <stdint.h>
#include <string.h>
#include "chewing.h"

#define FN(name) Java_com_muzigoing_eten41ime_engine_ChewingNative_##name
#define CTX(handle) ((ChewingContext *)(intptr_t)(handle))

static jbyteArray utf8_bytes(JNIEnv *env, const char *text) {
    if (text == NULL) return NULL;
    jsize length = (jsize)strlen(text);
    jbyteArray bytes = (*env)->NewByteArray(env, length);
    if (bytes != NULL && length > 0) {
        (*env)->SetByteArrayRegion(env, bytes, 0, length, (const jbyte *)text);
    }
    return bytes;
}

JNIEXPORT jlong JNICALL FN(create)(JNIEnv *env, jclass cls, jstring dictDir, jstring userPath) {
    const char *dict = (*env)->GetStringUTFChars(env, dictDir, NULL);
    const char *user = (*env)->GetStringUTFChars(env, userPath, NULL);
    ChewingContext *ctx = chewing_new2(dict, user, NULL, NULL);
    (*env)->ReleaseStringUTFChars(env, dictDir, dict);
    (*env)->ReleaseStringUTFChars(env, userPath, user);
    return (jlong)(intptr_t)ctx;
}

JNIEXPORT void JNICALL FN(delete)(JNIEnv *env, jclass cls, jlong h) { chewing_delete(CTX(h)); }

JNIEXPORT jint JNICALL FN(kbStrToNum)(JNIEnv *env, jclass cls, jstring name) {
    const char *value = (*env)->GetStringUTFChars(env, name, NULL);
    int result = chewing_KBStr2Num(value);
    (*env)->ReleaseStringUTFChars(env, name, value);
    return result;
}

JNIEXPORT jint JNICALL FN(setKBType)(JNIEnv *env, jclass cls, jlong h, jint type) { return chewing_set_KBType(CTX(h), type); }
JNIEXPORT void JNICALL FN(setMaxChiSymbolLen)(JNIEnv *env, jclass cls, jlong h, jint n) { chewing_set_maxChiSymbolLen(CTX(h), n); }
JNIEXPORT void JNICALL FN(setEscCleanAllBuf)(JNIEnv *env, jclass cls, jlong h, jint on) { chewing_set_escCleanAllBuf(CTX(h), on); }
JNIEXPORT void JNICALL FN(setPhraseChoiceRearward)(JNIEnv *env, jclass cls, jlong h, jint on) { chewing_set_phraseChoiceRearward(CTX(h), on); }

JNIEXPORT void JNICALL FN(handleDefault)(JNIEnv *env, jclass cls, jlong h, jint key) { chewing_handle_Default(CTX(h), key); }
JNIEXPORT void JNICALL FN(handleSpace)(JNIEnv *env, jclass cls, jlong h) { chewing_handle_Space(CTX(h)); }
JNIEXPORT void JNICALL FN(handleBackspace)(JNIEnv *env, jclass cls, jlong h) { chewing_handle_Backspace(CTX(h)); }
JNIEXPORT void JNICALL FN(handleEnter)(JNIEnv *env, jclass cls, jlong h) { chewing_handle_Enter(CTX(h)); }
JNIEXPORT void JNICALL FN(handleEsc)(JNIEnv *env, jclass cls, jlong h) { chewing_handle_Esc(CTX(h)); }
JNIEXPORT void JNICALL FN(handleLeft)(JNIEnv *env, jclass cls, jlong h) { chewing_handle_Left(CTX(h)); }
JNIEXPORT void JNICALL FN(handleRight)(JNIEnv *env, jclass cls, jlong h) { chewing_handle_Right(CTX(h)); }

JNIEXPORT jbyteArray JNICALL FN(bufferString)(JNIEnv *env, jclass cls, jlong h) { return utf8_bytes(env, chewing_buffer_String_static(CTX(h))); }
JNIEXPORT jbyteArray JNICALL FN(bopomofoString)(JNIEnv *env, jclass cls, jlong h) { return utf8_bytes(env, chewing_bopomofo_String_static(CTX(h))); }
JNIEXPORT jint JNICALL FN(bufferCheck)(JNIEnv *env, jclass cls, jlong h) { return chewing_buffer_Check(CTX(h)); }
JNIEXPORT jint JNICALL FN(bopomofoCheck)(JNIEnv *env, jclass cls, jlong h) { return chewing_bopomofo_Check(CTX(h)); }
JNIEXPORT jint JNICALL FN(cursorCurrent)(JNIEnv *env, jclass cls, jlong h) { return chewing_cursor_Current(CTX(h)); }

/// 上一個動作的上屏文字；沒有就回 null
JNIEXPORT jbyteArray JNICALL FN(takeCommit)(JNIEnv *env, jclass cls, jlong h) {
    if (chewing_commit_Check(CTX(h)) == 0) return NULL;
    return utf8_bytes(env, chewing_commit_String_static(CTX(h)));
}

JNIEXPORT jint JNICALL FN(candOpen)(JNIEnv *env, jclass cls, jlong h) { return chewing_cand_open(CTX(h)); }
JNIEXPORT jint JNICALL FN(candClose)(JNIEnv *env, jclass cls, jlong h) { return chewing_cand_close(CTX(h)); }
JNIEXPORT jint JNICALL FN(candListFirst)(JNIEnv *env, jclass cls, jlong h) { return chewing_cand_list_first(CTX(h)); }
JNIEXPORT jint JNICALL FN(candListNext)(JNIEnv *env, jclass cls, jlong h) { return chewing_cand_list_next(CTX(h)); }
JNIEXPORT jint JNICALL FN(candListHasNext)(JNIEnv *env, jclass cls, jlong h) { return chewing_cand_list_has_next(CTX(h)); }
JNIEXPORT jint JNICALL FN(candTotalChoice)(JNIEnv *env, jclass cls, jlong h) { return chewing_cand_TotalChoice(CTX(h)); }
JNIEXPORT jbyteArray JNICALL FN(candStringAt)(JNIEnv *env, jclass cls, jlong h, jint index) {
    return utf8_bytes(env, chewing_cand_string_by_index_static(CTX(h), index));
}
JNIEXPORT jint JNICALL FN(candChooseByIndex)(JNIEnv *env, jclass cls, jlong h, jint index) {
    return chewing_cand_choose_by_index(CTX(h), index);
}

/// 組字區每個音節的讀音（測試聲調鍵用）；chewing_phone_to_bopomofo 在 v0.13.1 成功回「位元組數＋1」
JNIEXPORT jobjectArray JNICALL FN(syllableReadings)(JNIEnv *env, jclass cls, jlong h) {
    int count = chewing_get_phoneSeqLen(CTX(h));
    jclass byteArrayClass = (*env)->FindClass(env, "[B");
    jobjectArray result = (*env)->NewObjectArray(env, count > 0 ? count : 0, byteArrayClass, NULL);
    if (count <= 0) return result;
    unsigned short *sequence = chewing_get_phoneSeq(CTX(h));
    for (int i = 0; i < count; i++) {
        char buffer[32] = {0};
        if (chewing_phone_to_bopomofo(sequence[i], buffer, sizeof buffer) > 0) {
            jbyteArray item = utf8_bytes(env, buffer);
            (*env)->SetObjectArrayElement(env, result, i, item);
            (*env)->DeleteLocalRef(env, item);
        }
    }
    chewing_free(sequence);
    return result;
}
