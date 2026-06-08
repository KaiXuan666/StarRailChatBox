package com.kaixuan.starrailchatbox.platform

import android.net.Uri
import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder

actual suspend fun readUriAsBytes(uri: String): ByteArray {
    val context = AndroidContextHolder.context ?: return ByteArray(0)
    return try {
        val parsedUri = if (uri.startsWith("/") && !uri.startsWith("//")) {
            Uri.fromFile(java.io.File(uri))
        } else {
            Uri.parse(uri)
        }
        context.contentResolver.openInputStream(parsedUri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: ByteArray(0)
    } catch (e: Exception) {
        ByteArray(0)
    }
}
