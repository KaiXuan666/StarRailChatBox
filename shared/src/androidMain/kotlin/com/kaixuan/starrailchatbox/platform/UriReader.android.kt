package com.kaixuan.starrailchatbox.platform

import android.net.Uri
import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder
import java.io.File

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

actual fun writeAudioBytesToCache(bytes: ByteArray, fileName: String): String {
    val context = AndroidContextHolder.context ?: return ""
    return try {

        val outputDir = File(context.filesDir, "chat_attachments")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val file = java.io.File(outputDir, fileName)
        file.writeBytes(bytes)
        file.absolutePath
    } catch (e: Exception) {
        ""
    }
}

