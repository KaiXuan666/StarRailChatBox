package com.kaixuan.starrailchatbox.data.character

import java.io.File

actual fun createCharacterStorage(directoryPath: String?): CharacterStorage {
    val directory = directoryPath?.let(::File) ?: File(
        System.getProperty("user.home"),
        ".starrailchatbox/characters",
    )
    return FileCharacterStorage(directory)
}

private class FileCharacterStorage(
    private val directory: File,
) : CharacterStorage {
    private val initializedMarker = directory.resolve(".initialized")

    override suspend fun initializeDefaults(defaults: List<CharacterFiles>) {
        directory.mkdirs()
        if (initializedMarker.isFile) return
        defaults.forEach(::writeCharacterIfMissing)
        initializedMarker.writeText("1", Charsets.UTF_8)
    }

    override suspend fun loadCharacters(): List<CharacterFiles> {
        directory.mkdirs()
        return directory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
            .mapNotNull { promptFile ->
                val avatarFile = directory.resolve("${promptFile.nameWithoutExtension}.webp")
                if (!avatarFile.isFile) return@mapNotNull null
                CharacterFiles(
                    name = promptFile.nameWithoutExtension,
                    promptBytes = promptFile.readBytes(),
                    avatarBytes = avatarFile.readBytes(),
                )
            }
            .sortedBy(CharacterFiles::name)
    }

    override suspend fun saveCharacter(character: CharacterFiles) {
        directory.mkdirs()
        directory.resolve("${character.name}.md").writeBytes(character.promptBytes)
        directory.resolve("${character.name}.webp").writeBytes(character.avatarBytes)
    }

    private fun writeCharacterIfMissing(character: CharacterFiles) {
        val promptFile = directory.resolve("${character.name}.md")
        val avatarFile = directory.resolve("${character.name}.webp")
        if (!promptFile.exists()) promptFile.writeBytes(character.promptBytes)
        if (!avatarFile.exists()) avatarFile.writeBytes(character.avatarBytes)
    }
}
