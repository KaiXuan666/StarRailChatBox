package com.kaixuan.starrailchatbox.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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

actual suspend fun compressImageIfPossible(uri: String): String = withContext(Dispatchers.IO) {
    val context = AndroidContextHolder.context ?: return@withContext uri
    try {
        val parsedUri = if (uri.startsWith("/") && !uri.startsWith("//")) {
            Uri.fromFile(java.io.File(uri))
        } else {
            Uri.parse(uri)
        }

        // 1. 获取图片原始尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(parsedUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        // 2. 计算缩放比例 (限制最大分辨率以减小体积，例如 2048)
        var inSampleSize = 1
        val maxDimension = 2048
        if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
            val halfWidth = options.outWidth / 2
            val halfHeight = options.outHeight / 2
            while (halfWidth / inSampleSize >= maxDimension || halfHeight / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
        }

        // 3. 解码图片
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = context.contentResolver.openInputStream(parsedUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@withContext uri

        // 4. 循环压缩直到小于 1MB
        val targetSize = 1024 * 1024 // 1MB
        var quality = 90
        val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        
        var currentSize: Long
        do {
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            currentSize = tempFile.length()
            quality -= 10
        } while (currentSize > targetSize && quality > 10)

        bitmap.recycle()
        Uri.fromFile(tempFile).toString()
    } catch (e: Exception) {
        uri
    }
}

actual suspend fun persistAttachment(uri: String, fileName: String): String = withContext(Dispatchers.IO) {
    Napier.d { "persistAttachment=$uri $fileName" }
    val context = AndroidContextHolder.context ?: return@withContext uri
    try {
        val parsedUri = if (uri.startsWith("/") && !uri.startsWith("//")) {
            Uri.fromFile(java.io.File(uri))
        } else {
            Uri.parse(uri)
        }
        val outputDir = File(context.filesDir, "chat_attachments")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val destFile = File(outputDir, fileName)
        // ===== 核心修复：检查源文件和目标文件是否是同一个 =====
        if (parsedUri.scheme == "file") {
            val sourceFile = java.io.File(parsedUri.path ?: "")
            // 如果绝对路径一致，说明已经是最终想要的目标文件了，直接返回
            if (sourceFile.canonicalPath == destFile.canonicalPath) {
                Napier.d { "源文件与目标文件一致，跳过复制" }
                return@withContext Uri.fromFile(destFile).toString()
            }
        }
        context.contentResolver.openInputStream(parsedUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(destFile).toString()
    } catch (e: Exception) {
        uri
    }
}
