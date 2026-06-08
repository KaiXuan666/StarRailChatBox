package com.kaixuan.starrailchatbox.platform

import android.net.Uri
import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder

actual suspend fun readUriAsBytes(uri: String): ByteArray {
    val context = AndroidContextHolder.context ?: return ByteArray(0)
    return try {
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { inputStream ->
            inputStream.readBytes()
        } ?: ByteArray(0)
    } catch (e: Exception) {
        ByteArray(0)
    }
}
