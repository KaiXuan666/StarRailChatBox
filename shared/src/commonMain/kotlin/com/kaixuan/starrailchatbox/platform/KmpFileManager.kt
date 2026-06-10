package com.kaixuan.starrailchatbox.platform

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * 跨平台文件管理器，提供对应用私有目录的文件操作能力。
 */
interface KmpFileManager {
    /**
     * 该平台是否支持物理文件系统操作。
     */
    val isSupported: Boolean get() = true

    /**
     * 应用的私有数据根目录。
     */
    val appDataDir: Path

    /**
     * 应用的临时缓存目录。
     */
    val cacheDir: Path

    /**
     * 用于文件操作的 okio FileSystem。
     */
    val fileSystem: FileSystem

    fun exists(path: Path): Boolean = if (isSupported) fileSystem.exists(path) else false
    fun exists(relativeName: String): Boolean = exists(appDataDir / relativeName.toPath())

    fun createDirectories(path: Path) {
        if (isSupported) fileSystem.createDirectories(path)
    }
    fun createDirectories(relativeName: String) {
        createDirectories(appDataDir / relativeName.toPath())
    }

    fun move(source: Path, target: Path) {
        if (!isSupported) return
        val parent = target.parent
        if (parent != null) {
            fileSystem.createDirectories(parent)
        }
        fileSystem.atomicMove(source, target)
    }

    fun copy(source: Path, target: Path) {
        if (!isSupported) return
        val parent = target.parent
        if (parent != null) {
            fileSystem.createDirectories(parent)
        }
        fileSystem.copy(source, target)
    }

    fun writeBytes(path: Path, bytes: ByteArray) {
        if (!isSupported) return
        val parent = path.parent
        if (parent != null) {
            fileSystem.createDirectories(parent)
        }
        fileSystem.write(path) {
            write(bytes)
        }
    }
    fun writeBytes(relativeName: String, bytes: ByteArray) {
        writeBytes(appDataDir / relativeName.toPath(), bytes)
    }

    fun readBytes(path: Path): ByteArray {
        if (!isSupported) throw UnsupportedOperationException("FileSystem operations are not supported on this platform.")
        return fileSystem.read(path) {
            readByteArray()
        }
    }
    fun readBytes(relativeName: String): ByteArray {
        return readBytes(appDataDir / relativeName.toPath())
    }

    fun writeText(path: Path, text: String) {
        if (!isSupported) return
        val parent = path.parent
        if (parent != null) {
            fileSystem.createDirectories(parent)
        }
        fileSystem.write(path) {
            writeUtf8(text)
        }
    }
    fun writeText(relativeName: String, text: String) {
        writeText(appDataDir / relativeName.toPath(), text)
    }

    fun readText(path: Path): String {
        if (!isSupported) throw UnsupportedOperationException("FileSystem operations are not supported on this platform.")
        return fileSystem.read(path) {
            readUtf8()
        }
    }
    fun readText(relativeName: String): String {
        return readText(appDataDir / relativeName.toPath())
    }

    fun delete(path: Path) {
        if (isSupported) {
            fileSystem.delete(path, mustExist = false)
        }
    }
    fun delete(relativeName: String) {
        delete(appDataDir / relativeName.toPath())
    }

    fun list(path: Path): List<Path> {
        return if (isSupported && fileSystem.exists(path)) {
            fileSystem.list(path)
        } else {
            emptyList()
        }
    }
    fun list(relativeName: String): List<Path> {
        return list(appDataDir / relativeName.toPath())
    }

    /**
     * 将图片保存到系统相册/画廊。
     * @param bytes 图片的字节数组
     * @param name 保存的文件名（包含扩展名）
     */
    suspend fun saveImageToGallery(bytes: ByteArray, name: String)

    companion object {
        val Default: KmpFileManager by lazy { getPlatformFileManager() }
    }
}

/**
 * 平台特定的文件管理器工厂方法。
 */
expect fun getPlatformFileManager(): KmpFileManager
