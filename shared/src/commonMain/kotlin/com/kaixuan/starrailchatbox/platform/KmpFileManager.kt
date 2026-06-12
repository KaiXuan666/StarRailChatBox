package com.kaixuan.starrailchatbox.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

    /**
     * 读取文件路径、平台 URI 或 data URI。
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun readSourceBytes(source: String): ByteArray {
        if (source.isBlank()) return ByteArray(0)
        if (source.startsWith("data:")) {
            val encoded = source.substringAfter("base64,", missingDelimiterValue = "")
            return if (encoded.isEmpty()) ByteArray(0) else Base64.decode(encoded)
        }
        val path = source.removePrefix("file://").toPath()
        return withContext(Dispatchers.Default) {
            try {
                if (exists(path) && !fileSystem.metadata(path).isDirectory) {
                    readBytes(path)
                } else {
                    ByteArray(0)
                }
            } catch (e: Exception) {
                ByteArray(0)
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun persistAudioAttachment(bytes: ByteArray, fileName: String): String {
        if (!isSupported) {
            return "data:audio/wav;base64,${Base64.encode(bytes)}"
        }
        val target = appDataDir / "chat_attachments".toPath() / fileName.toPath()
        return withContext(Dispatchers.Default) {
            writeBytes(target, bytes)
            target.toString()
        }
    }

    suspend fun compressImageIfPossible(source: String): String = source

    suspend fun persistAttachment(source: String, fileName: String): String {
        if (!isSupported) return source
        val target = appDataDir / "chat_attachments".toPath() / fileName.toPath()
        if (source.removePrefix("file://") == target.toString()) {
            return target.toString()
        }
        val bytes = readSourceBytes(source)
        return withContext(Dispatchers.Default) {
            writeBytes(target, bytes)
            target.toString()
        }
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
