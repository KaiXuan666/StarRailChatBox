package com.kaixuan.starrailchatbox.data.character

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FileCharacterAvatarStorage(
    private val directory: Path,
) : CharacterAvatarStorage {
    private val fileSystem = FileSystem.SYSTEM

    override fun saveBytes(characterId: String, avatarBytes: ByteArray): String {
        fileSystem.createDirectories(directory)
        val avatarPath = directory / characterAvatarFileName(characterId)
        fileSystem.write(avatarPath) { write(avatarBytes) }
        return avatarPath.toString()
    }

    override fun copyFrom(characterId: String, sourceUri: String): String {
        fileSystem.createDirectories(directory)
        val avatarPath = directory / characterAvatarFileName(characterId)
        fileSystem.delete(avatarPath, mustExist = false)
        fileSystem.copy(sourceUri.removePrefix("file://").toPath(), avatarPath)
        return avatarPath.toString()
    }

    override fun delete(avatarUri: String) {
        val path = avatarUri.removePrefix("file://").toPath()
        if (path.toString().startsWith(directory.toString())) {
            fileSystem.delete(path, mustExist = false)
        }
    }
}
