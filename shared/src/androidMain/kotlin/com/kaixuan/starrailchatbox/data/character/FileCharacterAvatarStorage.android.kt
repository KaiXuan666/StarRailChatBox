package com.kaixuan.starrailchatbox.data.character

import java.io.File

class FileCharacterAvatarStorage(
    private val directory: File,
) : CharacterAvatarStorage {
    override fun save(characterId: String, avatarBytes: ByteArray): String {
        directory.mkdirs()
        return directory.resolve(characterAvatarFileName(characterId))
            .also { file -> file.writeBytes(avatarBytes) }
            .absolutePath
    }

    override fun read(avatarUri: String): ByteArray = File(avatarUri).readBytes()
}
