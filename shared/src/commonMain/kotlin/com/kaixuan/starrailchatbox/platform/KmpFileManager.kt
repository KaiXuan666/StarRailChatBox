package com.kaixuan.starrailchatbox.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.HashingSource
import okio.buffer
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

    /** 流式复制路径或平台 URI，适用于数 GB 模型文件。 */
    suspend fun copySourceTo(source: String, target: Path, append: Boolean = false): Long {
        if (!isSupported) throw UnsupportedOperationException("File operations are not supported.")
        val sourcePath = source.removePrefix("file://").toPath()
        return withContext(Dispatchers.Default) {
            target.parent?.let(fileSystem::createDirectories)
            val input = fileSystem.source(sourcePath).buffer()
            try {
                val rawSink = if (append) {
                    fileSystem.appendingSink(target, mustExist = false)
                } else {
                    fileSystem.sink(target, mustCreate = false)
                }
                val output = rawSink.buffer()
                try {
                    var total = 0L
                    while (true) {
                        val read = input.read(output.buffer, 64 * 1024L)
                        if (read == -1L) break
                        total += read
                        output.emitCompleteSegments()
                    }
                    total
                } finally {
                    output.close()
                }
            } finally {
                input.close()
            }
        }
    }

    suspend fun sha256(path: Path): String = withContext(Dispatchers.Default) {
        val hashingSource = HashingSource.sha256(fileSystem.source(path))
        val source = hashingSource.buffer()
        try {
            val discard = Buffer()
            while (source.read(discard, 64 * 1024L) != -1L) {
                discard.clear()
            }
        } finally {
            source.close()
        }
        hashingSource.hash.hex()
    }

    /** 返回目标应用目录所在卷的剩余空间；未知时返回 null。 */
    suspend fun availableSpaceBytes(path: Path = appDataDir): Long? = null

    /**
     * 获取文件路径、平台 URI 或 data URI 的字节大小。无法通过元数据获取时返回 null。
     */
    suspend fun sourceSizeBytes(source: String): Long? {
        if (source.isBlank()) return 0L
        if (source.startsWith("data:")) {
            val encoded = source.substringAfter("base64,", missingDelimiterValue = "")
                .filterNot { it == '\r' || it == '\n' || it == ' ' || it == '\t' }
            if (encoded.isEmpty()) return 0L
            val padding = encoded.takeLast(2).count { it == '=' }
            return (encoded.length.toLong() / 4L) * 3L - padding
        }
        if (!isSupported) return null
        val path = source.removePrefix("file://").toPath()
        return withContext(Dispatchers.Default) {
            try {
                fileSystem.metadata(path).takeIf { !it.isDirectory }?.size
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * 最多读取 maxBytes + 1 字节，用于在元数据不可用时安全判断大小。
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun readSourceBytesUpTo(source: String, maxBytes: Long): ByteArray {
        if (source.isBlank()) return ByteArray(0)
        if (source.startsWith("data:")) {
            return readSourceBytes(source)
        }
        if (!isSupported) return ByteArray(0)
        val path = source.removePrefix("file://").toPath()
        return withContext(Dispatchers.Default) {
            try {
                if (!exists(path) || fileSystem.metadata(path).isDirectory) {
                    return@withContext ByteArray(0)
                }
                val rawSource = fileSystem.source(path)
                try {
                    val buffer = Buffer()
                    var remaining = maxBytes + 1
                    while (remaining > 0L) {
                        val read = rawSource.read(buffer, minOf(8192L, remaining))
                        if (read == -1L) break
                        remaining -= read
                    }
                    buffer.readByteArray()
                } finally {
                    rawSource.close()
                }
            } catch (_: Exception) {
                ByteArray(0)
            }
        }
    }

    /**
     * 将文件路径、平台 URI 或 data URI 以 Base64 文本分块写出，避免为大文件一次性分配字节数组。
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun writeSourceBase64(
        source: String,
        writeChunk: suspend (String) -> Unit,
    ) {
        if (source.isBlank()) return
        if (source.startsWith("data:")) {
            writeBase64TextChunks(
                encoded = source.substringAfter("base64,", missingDelimiterValue = ""),
                writeChunk = writeChunk,
            )
            return
        }
        if (!isSupported) return
        val path = source.removePrefix("file://").toPath()
        return withContext(Dispatchers.Default) {
            val rawSource = try {
                if (!exists(path) || fileSystem.metadata(path).isDirectory) {
                    return@withContext
                }
                fileSystem.source(path)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                return@withContext
            }

            val buffer = Buffer()
            val encoder = StreamingBase64ChunkWriter()
            try {
                while (true) {
                    val read = rawSource.read(buffer, Base64SourceChunkSize.toLong())
                    if (read == -1L) break
                    val bytes = buffer.readByteArray()
                    encoder.write(bytes, bytes.size, writeChunk)
                }
                encoder.finish(writeChunk)
            } finally {
                rawSource.close()
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

private const val Base64SourceChunkSize = 24 * 1024
private const val Base64TextChunkSize = 16 * 1024

internal suspend fun writeBase64TextChunks(
    encoded: String,
    writeChunk: suspend (String) -> Unit,
) {
    val chunk = StringBuilder(Base64TextChunkSize)
    for (char in encoded) {
        if (char == '\r' || char == '\n' || char == ' ' || char == '\t') continue
        chunk.append(char)
        if (chunk.length >= Base64TextChunkSize) {
            writeChunk(chunk.toString())
            chunk.clear()
        }
    }
    if (chunk.isNotEmpty()) {
        writeChunk(chunk.toString())
    }
}

@OptIn(ExperimentalEncodingApi::class)
internal class StreamingBase64ChunkWriter {
    private val pending = ByteArray(2)
    private var pendingSize = 0

    suspend fun write(
        bytes: ByteArray,
        length: Int,
        writeChunk: suspend (String) -> Unit,
    ) {
        if (length <= 0) return

        var offset = 0
        if (pendingSize > 0) {
            val needed = 3 - pendingSize
            if (length < needed) {
                bytes.copyInto(pending, destinationOffset = pendingSize, startIndex = 0, endIndex = length)
                pendingSize += length
                return
            }

            val firstGroup = ByteArray(3)
            pending.copyInto(firstGroup, endIndex = pendingSize)
            bytes.copyInto(firstGroup, destinationOffset = pendingSize, startIndex = 0, endIndex = needed)
            writeChunk(Base64.Default.encode(firstGroup))
            offset = needed
            pendingSize = 0
        }

        val remaining = length - offset
        val encodableLength = remaining - (remaining % 3)
        if (encodableLength > 0) {
            val encodableBytes = bytes.copyOfRange(offset, offset + encodableLength)
            writeChunk(Base64.Default.encode(encodableBytes))
            offset += encodableLength
        }

        while (offset < length) {
            pending[pendingSize] = bytes[offset]
            pendingSize += 1
            offset += 1
        }
    }

    suspend fun finish(writeChunk: suspend (String) -> Unit) {
        if (pendingSize > 0) {
            writeChunk(Base64.Default.encode(pending.copyOf(pendingSize)))
            pendingSize = 0
        }
    }
}
