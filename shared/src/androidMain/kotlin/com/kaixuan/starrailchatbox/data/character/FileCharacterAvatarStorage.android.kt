package com.kaixuan.starrailchatbox.data.character

import android.content.Context
import android.net.Uri
import java.io.File

class FileCharacterAvatarStorage(
    private val directory: File,
    private val context: Context? = null,
) : CharacterAvatarStorage {
    override fun saveBytes(characterId: String, avatarBytes: ByteArray): String {
        directory.mkdirs()
        return directory.resolve(characterAvatarFileName(characterId))
            .also { file -> file.writeBytes(avatarBytes) }
            .absolutePath
    }

    override fun copyFrom(characterId: String, sourceUri: String): String {
        directory.mkdirs()
        val target = directory.resolve(characterAvatarFileName(characterId))
        if (sourceUri.startsWith("content://")) {
            val resolver = requireNotNull(context) {
                "Android context is required to copy content URI avatars."
            }.contentResolver
            resolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                requireNotNull(input) { "Unable to open avatar source: $sourceUri" }
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            File(sourceUri.removePrefix("file://")).copyTo(target, overwrite = true)
        }
        return target.absolutePath
    }

    override fun delete(avatarUri: String) {
        val file = File(avatarUri.removePrefix("file://"))
        if (file.startsWith(directory)) {
            file.delete()
        }
    }
}
