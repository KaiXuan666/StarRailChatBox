package com.kaixuan.starrailchatbox.data.character

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FileCharacterAvatarStorage(
    private val directory: Path,
) : CharacterAvatarStorage {
    private val fileSystem = FileSystem.SYSTEM

    override fun save(characterId: String, avatarBytes: ByteArray): String {
        fileSystem.createDirectories(directory)
        val avatarPath = directory / characterAvatarFileName(characterId)
        fileSystem.write(avatarPath) { write(avatarBytes) }
        return avatarPath.toString()
    }

    override fun read(avatarUri: String): ByteArray {
        return fileSystem.read(avatarUri.toPath()) { readByteArray() }
    }
}
