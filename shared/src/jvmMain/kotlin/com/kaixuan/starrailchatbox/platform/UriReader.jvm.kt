package com.kaixuan.starrailchatbox.platform

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

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

actual suspend fun compressImageIfPossible(uri: String): String {
    return try {
        val path = if (uri.startsWith("file://")) uri.removePrefix("file://") else uri
        val sourceFile = File(path)
        if (!sourceFile.exists()) return uri

        val image = ImageIO.read(sourceFile) ?: return uri
        
        val targetSize = 1024 * 1024 // 1MB
        if (sourceFile.length() < targetSize) return uri

        val tempFile = File.createTempFile("compressed_", ".jpg")
        var quality = 0.9f
        
        do {
            val writer = ImageIO.getImageWritersByFormatName("jpg").next()
            val writeParam = writer.defaultWriteParam
            writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
            writeParam.compressionQuality = quality
            
            FileOutputStream(tempFile).use { out ->
                writer.output = ImageIO.createImageOutputStream(out)
                writer.write(null, IIOImage(image, null, null), writeParam)
                writer.dispose()
            }
            quality -= 0.1f
        } while (tempFile.length() > targetSize && quality > 0.1f)

        tempFile.absolutePath
    } catch (e: Exception) {
        uri
    }
}

actual suspend fun persistAttachment(uri: String, fileName: String): String {
    return try {
        val path = if (uri.startsWith("file://")) uri.removePrefix("file://") else uri
        val sourceFile = File(path)
        if (!sourceFile.exists()) return uri

        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".starrailchatbox/attachments")
        if (!appDir.exists()) appDir.mkdirs()

        val destFile = File(appDir, fileName)
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        destFile.absolutePath
    } catch (e: Exception) {
        uri
    }
}
