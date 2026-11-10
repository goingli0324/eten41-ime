package com.muzigoing.eten41ime.engine

import android.content.Context
import java.io.File

/**
 * libchewing 要讀實際檔案路徑，App 打包的 assets 不是檔案，所以第一次啟動（或 App 更新後）
 * 把 assets/dict/ 複製到私有資料夾。用 App 的 versionCode 判斷要不要重新複製。
 */
object DictionaryInstaller {
    private const val ASSET_DIRECTORY = "dict"
    private const val STAMP_FILE = ".installed-version"

    fun ensureInstalled(context: Context): File {
        val target = File(context.filesDir, ASSET_DIRECTORY)
        val stamp = File(target, STAMP_FILE)
        val version = currentVersion(context)
        if (stamp.exists() && stamp.readText() == version && File(target, "word.dat").exists()) {
            return target
        }
        target.mkdirs()
        val names = context.assets.list(ASSET_DIRECTORY).orEmpty()
        check(names.isNotEmpty()) { "App 裡沒有詞庫（assets/dict），請先跑 scripts/build-libchewing-android.sh" }
        for (name in names) {
            context.assets.open("$ASSET_DIRECTORY/$name").use { input ->
                File(target, name).outputStream().use { output -> input.copyTo(output) }
            }
        }
        stamp.writeText(version)
        return target
    }

    private fun currentVersion(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return "${info.longVersionCode}-${info.lastUpdateTime}"
    }
}
