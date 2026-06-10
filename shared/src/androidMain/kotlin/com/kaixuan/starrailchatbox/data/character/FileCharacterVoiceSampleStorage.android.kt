package com.kaixuan.starrailchatbox.data.character

import android.content.Context
import android.net.Uri
import java.io.File

class FileCharacterVoiceSampleStorage(
    private val directory: File,
    private val context: Context? = null,
) : CharacterVoiceSampleStorage {
    override fun saveBytes(characterId: String, voiceBytes: ByteArray): String {
        directory.mkdirs()
        return directory.resolve(characterVoiceSampleFileName(characterId))
            .also { file -> file.writeBytes(voiceBytes) }
            .absolutePath
    }

    override fun copyFrom(characterId: String, sourceUri: String): String {
        directory.mkdirs()
        
        val extension = sourceUri.substringAfterLast('.', "mp3").let { ext ->
            if (ext.contains('/') || ext.length > 5) "mp3" else ext
        }
        val target = directory.resolve(characterVoiceSampleFileName(characterId, extension))
        
        if (sourceUri.startsWith("content://")) {
            val resolver = requireNotNull(context) {
                "Android context is required to copy content URI voice samples."
            }.contentResolver
            resolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                requireNotNull(input) { "Unable to open voice source: $sourceUri" }
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            File(sourceUri.removePrefix("file://")).copyTo(target, overwrite = true)
        }
        return target.absolutePath
    }

    override fun delete(voiceUri: String) {
        val file = File(voiceUri.removePrefix("file://"))
        if (file.startsWith(directory)) {
            file.delete()
        }
    }
}
