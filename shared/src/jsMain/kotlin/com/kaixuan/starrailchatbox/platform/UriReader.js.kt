package com.kaixuan.starrailchatbox.platform

actual suspend fun readUriAsBytes(uri: String): ByteArray {
    return ByteArray(0)
}

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
actual fun writeAudioBytesToCache(bytes: ByteArray, fileName: String): String {
    return "data:audio/wav;base64," + kotlin.io.encoding.Base64.encode(bytes)
}

actual suspend fun compressImageIfPossible(uri: String): String {
    return uri
}

actual suspend fun persistAttachment(uri: String, fileName: String): String {
    return uri
}
