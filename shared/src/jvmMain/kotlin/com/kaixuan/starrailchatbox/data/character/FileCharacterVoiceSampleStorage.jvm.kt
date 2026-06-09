package com.kaixuan.starrailchatbox.data.character

import java.io.File

class FileCharacterVoiceSampleStorage(
    private val directory: File,
) : CharacterVoiceSampleStorage {
    override fun saveBytes(characterId: String, voiceBytes: ByteArray): String {
        directory.mkdirs()
        return directory.resolve(characterVoiceSampleFileName(characterId))
            .also { file -> file.writeBytes(voiceBytes) }
            .absolutePath
    }

    override fun delete(voiceUri: String) {
        val file = File(voiceUri.removePrefix("file://"))
        if (file.startsWith(directory)) {
            file.delete()
        }
    }
}
