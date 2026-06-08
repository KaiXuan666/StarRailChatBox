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
