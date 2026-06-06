@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kaixuan.starrailchatbox.data.character

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun createCharacterStorage(directoryPath: String?): CharacterStorage {
    val resolvedPath = directoryPath ?: run {
        val documentsDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        "${requireNotNull(documentsDirectory?.path)}/characters"
    }
    return FileCharacterStorage(resolvedPath.toPath())
}

private class FileCharacterStorage(
    private val directory: Path,
) : CharacterStorage {
    private val fileSystem = FileSystem.SYSTEM
    private val initializedMarker = directory / ".initialized"

    override suspend fun initializeDefaults(defaults: List<CharacterFiles>) {
        fileSystem.createDirectories(directory)
        if (fileSystem.exists(initializedMarker)) return
        defaults.forEach(::writeCharacterIfMissing)
        fileSystem.write(initializedMarker) { writeUtf8("1") }
    }

    override suspend fun loadCharacters(): List<CharacterFiles> {
        fileSystem.createDirectories(directory)
        return fileSystem.list(directory)
            .filter { it.name.endsWith(".md", ignoreCase = true) }
            .mapNotNull { promptPath ->
                val name = promptPath.name.dropLast(3)
                val avatarPath = directory / "$name.webp"
                if (!fileSystem.exists(avatarPath)) return@mapNotNull null
                CharacterFiles(
                    name = name,
                    promptBytes = fileSystem.read(promptPath) { readByteArray() },
                    avatarBytes = fileSystem.read(avatarPath) { readByteArray() },
                )
            }
            .sortedBy(CharacterFiles::name)
    }

    override suspend fun saveCharacter(character: CharacterFiles) {
        fileSystem.createDirectories(directory)
        fileSystem.write(directory / "${character.name}.md") {
            write(character.promptBytes)
        }
        fileSystem.write(directory / "${character.name}.webp") {
            write(character.avatarBytes)
        }
    }

    private fun writeCharacterIfMissing(character: CharacterFiles) {
        val promptPath = directory / "${character.name}.md"
        val avatarPath = directory / "${character.name}.webp"
        if (!fileSystem.exists(promptPath)) {
            fileSystem.write(promptPath) { write(character.promptBytes) }
        }
        if (!fileSystem.exists(avatarPath)) {
            fileSystem.write(avatarPath) { write(character.avatarBytes) }
        }
    }
}
