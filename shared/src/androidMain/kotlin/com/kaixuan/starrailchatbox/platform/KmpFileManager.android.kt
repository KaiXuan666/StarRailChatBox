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
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.os.Environment
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.os.StatFs

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

    override suspend fun copySourceTo(source: String, target: Path, append: Boolean): Long {
        if (!source.startsWith("content://")) {
            return super<KmpFileManager>.copySourceTo(source, target, append)
        }
        val context = requireNotNull(AndroidContextHolder.context)
        return withContext(Dispatchers.IO) {
            target.parent?.let(fileSystem::createDirectories)
            context.contentResolver.openInputStream(Uri.parse(source))?.use { input ->
                FileOutputStream(target.toString(), append).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        total += read
                    }
                    total
                }
            } ?: throw IllegalArgumentException("Unable to open selected file.")
        }
    }

    override suspend fun availableSpaceBytes(path: Path): Long = withContext(Dispatchers.IO) {
        val existingPath = generateSequence(File(path.toString())) { it.parentFile }
            .firstOrNull(File::exists)
            ?.absolutePath
            ?: requireNotNull(AndroidContextHolder.context).filesDir.absolutePath
        StatFs(existingPath).availableBytes
    }

    override suspend fun sourceSizeBytes(source: String): Long? {
        if (!source.startsWith("content://")) {
            return super<KmpFileManager>.sourceSizeBytes(source)
        }
        val context = requireNotNull(AndroidContextHolder.context)
        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(source)
            val queriedSize = runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.SIZE),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                        cursor.getLong(index)
                    } else {
                        null
                    }
                }
            }.getOrNull()
            if (queriedSize != null && queriedSize >= 0L) {
                return@withContext queriedSize
            }
            runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    descriptor.length.takeIf { it >= 0L }
                }
            }.getOrNull()
        }
    }

    override suspend fun readSourceBytesUpTo(source: String, maxBytes: Long): ByteArray {
        if (!source.startsWith("content://")) {
            return super<KmpFileManager>.readSourceBytesUpTo(source, maxBytes)
        }
        val context = requireNotNull(AndroidContextHolder.context)
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(source))?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var remaining = maxBytes + 1
                    while (remaining > 0L) {
                        val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        remaining -= read
                    }
                    output.toByteArray()
                } ?: ByteArray(0)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Napier.e("Failed to read bounded source URI: $source", error)
                ByteArray(0)
            }
        }
    }

    override suspend fun writeSourceBase64(
        source: String,
        writeChunk: suspend (String) -> Unit,
    ) {
        if (!source.startsWith("content://")) {
            return super<KmpFileManager>.writeSourceBase64(source, writeChunk)
        }
        val context = requireNotNull(AndroidContextHolder.context)
        withContext(Dispatchers.IO) {
            val input = try {
                context.contentResolver.openInputStream(Uri.parse(source))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Napier.e("Failed to open source URI for Base64 streaming: $source", error)
                null
            } ?: return@withContext

            try {
                input.use {
                    val buffer = ByteArray(24 * 1024)
                    val encoder = StreamingBase64ChunkWriter()
                    while (true) {
                        val read = it.read(buffer)
                        if (read == -1) break
                        encoder.write(buffer, read, writeChunk)
                    }
                    encoder.finish(writeChunk)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Napier.e("Failed to stream source URI as Base64: $source", error)
                throw error
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

