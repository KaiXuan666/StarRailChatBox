package com.kaixuan.starrailchatbox.platform

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.File

class JvmFileManager : KmpFileManager {
    override val fileSystem: FileSystem = FileSystem.SYSTEM

    override val appDataDir: Path by lazy {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".starrailchatbox")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        appDir.absolutePath.toPath()
    }
}

actual fun getPlatformFileManager(): KmpFileManager = JvmFileManager()
