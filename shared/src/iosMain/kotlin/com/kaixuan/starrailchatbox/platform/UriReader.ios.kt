package com.kaixuan.starrailchatbox.platform

import kotlinx.cinterop.usePinned
import kotlinx.cinterop.addressOf

actual suspend fun readUriAsBytes(uri: String): ByteArray {
    return ByteArray(0)
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual fun writeAudioBytesToCache(bytes: ByteArray, fileName: String): String {
    return try {
        val tempDir = platform.Foundation.NSTemporaryDirectory()
        val path = tempDir + fileName
        if (bytes.isNotEmpty()) {
            val nsData = bytes.usePinned { pinned ->
                platform.Foundation.NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            nsData.writeToFile(path, true)
            path
        } else {
            ""
        }
    } catch (e: Exception) {
        @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
        "data:audio/wav;base64," + kotlin.io.encoding.Base64.encode(bytes)
    }
}

