package com.kaixuan.starrailchatbox.platform

/**
 * 跨平台读取 URI/本地文件路径的数据。
 *
 * 在 Android 平台支持读取 content:// 和 file:// 协议；
 * 在 JVM/Desktop 平台支持读取本地文件路径。
 */
expect suspend fun readUriAsBytes(uri: String): ByteArray

/**
 * 将生成的音频字节流写入本地平台的临时/缓存文件，并返回对应的本地 URI 或是 data: Base64 URI。
 */
expect fun writeAudioBytesToCache(bytes: ByteArray, fileName: String): String

/**
 * 压缩图片到指定大小以内（例如 1MB），并返回压缩后的新 URI。
 * 如果无需压缩或平台不支持，则返回原 URI。
 */
expect suspend fun compressImageIfPossible(uri: String): String

/**
 * 将附件持久化到应用的私有存储目录。
 */
expect suspend fun persistAttachment(uri: String, fileName: String): String

