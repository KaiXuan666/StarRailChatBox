package com.kaixuan.starrailchatbox.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.os.Environment
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import io.github.vinceglb.filekit.div

class AndroidFileManager : KmpFileManager {
    override val fileSystem: FileSystem = FileSystem.SYSTEM

    override val appDataDir: Path by lazy {
        val context = requireNotNull(AndroidContextHolder.context) {
            "Android context must be initialized before using KmpFileManager."
        }
        context.filesDir.absolutePath.toPath()
    }

    override val cacheDir: Path by lazy {
        val context = requireNotNull(AndroidContextHolder.context) {
            "Android context must be initialized before using KmpFileManager."
        }
        context.cacheDir.absolutePath.toPath()
    }

    override suspend fun readSourceBytes(source: String): ByteArray {
        if (!source.startsWith("content://")) {
            return super<KmpFileManager>.readSourceBytes(source)
        }
        val context = requireNotNull(AndroidContextHolder.context)
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(source))?.use { input ->
                    input.readBytes()
                } ?: ByteArray(0)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Napier.e("Failed to read source URI: $source", error)
                ByteArray(0)
            }
        }
    }

    override suspend fun compressImageIfPossible(source: String): String = withContext(Dispatchers.IO) {
        val context = requireNotNull(AndroidContextHolder.context)
        try {
            val uri = if (source.startsWith("/") && !source.startsWith("//")) {
                Uri.fromFile(File(source))
            } else {
                Uri.parse(source)
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            var sampleSize = 1
            val maxDimension = 2048
            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                val halfWidth = options.outWidth / 2
                val halfHeight = options.outHeight / 2
                while (
                    halfWidth / sampleSize >= maxDimension ||
                    halfHeight / sampleSize >= maxDimension
                ) {
                    sampleSize *= 2
                }
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@withContext source
            val target = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            var quality = 90
            var currentSize: Long
            do {
                FileOutputStream(target).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                }
                currentSize = target.length()
                quality -= 10
            } while (currentSize > 1024 * 1024 && quality > 10)
            bitmap.recycle()
            Uri.fromFile(target).toString()
        } catch (error: Exception) {
            Napier.e("Failed to compress image: $source", error)
            source
        }
    }

    override suspend fun saveImageToGallery(bytes: ByteArray, name: String) {
        val context = requireNotNull(AndroidContextHolder.context)
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StarRailChatBox")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = contentResolver.insert(imageCollection, contentValues)
            if (imageUri != null) {
                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    outputStream.write(bytes)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(imageUri, contentValues, null, null)
                }
            }
        }
    }
}

actual fun getPlatformFileManager(): KmpFileManager = AndroidFileManager()

