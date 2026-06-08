package com.kaixuan.starrailchatbox.platform

import java.io.File

actual suspend fun readUriAsBytes(uri: String): ByteArray {
    return try {
        val path = if (uri.startsWith("file://")) {
            uri.removePrefix("file://")
        } else {
            uri
        }
        File(path).readBytes()
    } catch (e: Exception) {
        ByteArray(0)
    }
}

actual fun writeAudioBytesToCache(bytes: ByteArray, fileName: String): String {
    return try {
        val tempDir = System.getProperty("java.io.tmpdir")
        val file = File(tempDir, fileName)
        file.writeBytes(bytes)
        file.absolutePath
    } catch (e: Exception) {
        ""
    }
}

