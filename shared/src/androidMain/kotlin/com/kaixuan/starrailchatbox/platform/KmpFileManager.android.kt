package com.kaixuan.starrailchatbox.platform

import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class AndroidFileManager : KmpFileManager {
    override val fileSystem: FileSystem = FileSystem.SYSTEM

    override val appDataDir: Path by lazy {
        val context = requireNotNull(AndroidContextHolder.context) {
            "Android context must be initialized before using KmpFileManager."
        }
        context.filesDir.absolutePath.toPath()
    }
}

actual fun getPlatformFileManager(): KmpFileManager = AndroidFileManager()
