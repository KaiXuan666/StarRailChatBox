package com.kaixuan.starrailchatbox.platform

import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
