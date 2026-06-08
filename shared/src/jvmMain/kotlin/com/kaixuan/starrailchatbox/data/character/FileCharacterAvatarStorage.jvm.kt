package com.kaixuan.starrailchatbox.data.character

import java.io.File

class FileCharacterAvatarStorage(
    private val directory: File,
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
        File(sourceUri.removePrefix("file://")).copyTo(target, overwrite = true)
        return target.absolutePath
    }

    override fun delete(avatarUri: String) {
        val file = File(avatarUri.removePrefix("file://"))
        if (file.startsWith(directory)) {
            file.delete()
        }
    }
}
