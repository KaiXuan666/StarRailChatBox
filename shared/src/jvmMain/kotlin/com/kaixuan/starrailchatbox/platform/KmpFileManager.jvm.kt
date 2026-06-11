package com.kaixuan.starrailchatbox.platform

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.File
import java.io.FileOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

import io.github.vinceglb.filekit.div

class JvmFileManager : KmpFileManager {
    override val fileSystem: FileSystem = FileSystem.SYSTEM

    override val appDataDir: Path by lazy {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".starrailchatbox")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        appDir.absolutePath.toPath()
    }

    override val cacheDir: Path by lazy {
        val tempDir = System.getProperty("java.io.tmpdir")
        val appCacheDir = File(tempDir, "starrailchatbox_cache")
        if (!appCacheDir.exists()) {
            appCacheDir.mkdirs()
        }
        appCacheDir.absolutePath.toPath()
    }

    override suspend fun saveImageToGallery(bytes: ByteArray, name: String) {
        val userHome = System.getProperty("user.home")
        val downloadsDir = File(userHome, "Downloads")
        val targetDir = if (downloadsDir.exists()) downloadsDir else File(userHome)
        val targetFile = File(targetDir, name)
        
        writeBytes(targetFile.absolutePath.toPath(), bytes)
    }

    override suspend fun compressImageIfPossible(source: String): String {
        return try {
            val sourceFile = File(source.removePrefix("file://"))
            if (!sourceFile.exists() || sourceFile.length() < 1024 * 1024) return source
            val image = ImageIO.read(sourceFile) ?: return source
            val target = File.createTempFile("compressed_", ".jpg")
            var quality = 0.9f
            do {
                val writer = ImageIO.getImageWritersByFormatName("jpg").next()
                val writeParam = writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = quality
                }
                FileOutputStream(target).use { output ->
                    writer.output = ImageIO.createImageOutputStream(output)
                    writer.write(null, IIOImage(image, null, null), writeParam)
                    writer.dispose()
                }
                quality -= 0.1f
            } while (target.length() > 1024 * 1024 && quality > 0.1f)
            target.absolutePath
        } catch (error: Exception) {
            source
        }
    }
}

actual fun getPlatformFileManager(): KmpFileManager = JvmFileManager()

