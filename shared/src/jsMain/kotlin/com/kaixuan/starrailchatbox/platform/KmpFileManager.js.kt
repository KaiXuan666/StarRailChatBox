package com.kaixuan.starrailchatbox.platform

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class JsFileManager : KmpFileManager {
    override val isSupported: Boolean = false

    override val appDataDir: Path by lazy { "/tmp".toPath() }

    override val cacheDir: Path by lazy { "/tmp/cache".toPath() }

    override val fileSystem: FileSystem
        get() = throw UnsupportedOperationException("FileSystem operations are not supported on JS Web platform.")

    override suspend fun saveImageToGallery(bytes: ByteArray, name: String) {
        // Not supported on JS Web
    }
}

actual fun getPlatformFileManager(): KmpFileManager = JsFileManager()

