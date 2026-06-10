package com.kaixuan.starrailchatbox.data.character

import okio.FileSystem
import okio.Path

class FileCharacterVoiceSampleStorage(
    private val directory: Path,
) : CharacterVoiceSampleStorage {
    private val fileSystem = FileSystem.SYSTEM

    override fun saveBytes(characterId: String, voiceBytes: ByteArray): String {
        fileSystem.createDirectories(directory)
        val voicePath = directory / characterVoiceSampleFileName(characterId)
        fileSystem.write(voicePath) { write(voiceBytes) }
        return voicePath.toString()
    }

    override fun copyFrom(characterId: String, sourceUri: String): String {
        fileSystem.createDirectories(directory)
        val extension = sourceUri.substringAfterLast('.', "mp3").let { ext ->
            if (ext.contains('/') || ext.length > 5) "mp3" else ext
        }
        val target = directory / characterVoiceSampleFileName(characterId, extension)
        val source = sourceUri.removePrefix("file://").let(okio.Path.Companion::toPath)
        fileSystem.copy(source, target)
        return target.toString()
    }

    override fun delete(voiceUri: String) {
        val path = voiceUri.removePrefix("file://").let(okio.Path.Companion::toPath)
        if (path.toString().startsWith(directory.toString())) {
            fileSystem.delete(path, mustExist = false)
        }
    }
}
