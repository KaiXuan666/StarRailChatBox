package com.kaixuan.starrailchatbox.platform

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder
import io.github.aakira.napier.Napier
import java.io.File

actual fun openUri(uri: String, mimeType: String?) {
    val context = AndroidContextHolder.context ?: return
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        if (uri.startsWith("file://") || uri.startsWith("/")) {
            // 使用 Uri.parse 并获取 path，它会自动处理解码
            val parsedUri = Uri.parse(uri)
            val filePath = if (uri.startsWith("file://")) {
                parsedUri.path
            } else {
                // 如果是直接传入的路径，可能需要手动解码或者处理
                Uri.decode(uri)
            }
            
            if (filePath == null) {
                Napier.e { "Parsed file path is null for URI: $uri" }
                return
            }
            
            val file = File(filePath)
            if (!file.exists()) {
                Napier.e { "File does not exist at decoded path: $filePath (Original: $uri)" }
                return
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            intent.setDataAndType(contentUri, mimeType ?: context.contentResolver.getType(contentUri))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.data = Uri.parse(uri)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Napier.e(e) { "Failed to open URI: $uri" }
    }
}
