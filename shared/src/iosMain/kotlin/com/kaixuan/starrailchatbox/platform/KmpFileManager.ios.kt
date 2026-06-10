package com.kaixuan.starrailchatbox.platform

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

class IosFileManager : KmpFileManager {
    override val fileSystem: FileSystem = FileSystem.SYSTEM

    @OptIn(ExperimentalForeignApi::class)
    override val appDataDir: Path by lazy {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        val directoryPath = requireNotNull(directory?.path) {
            "Failed to retrieve iOS NSDocumentDirectory path"
        }
        directoryPath.toPath()
    }

    override suspend fun saveImageToGallery(bytes: ByteArray, name: String) {
        // TODO: Implement iOS photo gallery saving
    }
}

actual fun getPlatformFileManager(): KmpFileManager = IosFileManager()
