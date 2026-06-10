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

    override val cacheDir: Path by lazy {
        val tempDir = System.getProperty("java.io.tmpdir")
        val appCacheDir = File(tempDir, "starrailchatbox_cache")
        if (!appCacheDir.exists()) {
            appCacheDir.mkdirs()
        }
        appCacheDir.absolutePath.toPath()
    }

    override suspend fun saveImageToGallery(bytes: ByteArray, name: String) {
        val userHome = System.getProperty("user.home")
        val downloadsDir = File(userHome, "Downloads")
        val targetDir = if (downloadsDir.exists()) downloadsDir else File(userHome)
        val targetFile = File(targetDir, name)
        
        writeBytes(targetFile.absolutePath.toPath(), bytes)
    }
}

actual fun getPlatformFileManager(): KmpFileManager = JvmFileManager()
