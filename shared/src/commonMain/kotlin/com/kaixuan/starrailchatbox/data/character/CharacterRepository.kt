package com.kaixuan.starrailchatbox.data.character

import org.jetbrains.compose.resources.ExperimentalResourceApi
import starrailchatbox.shared.generated.resources.Res

data class Character(
    val id: String,
    val name: String,
    val prompt: String,
    val openingMessage: String,
    val avatarBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Character) return false
        return id == other.id &&
            name == other.name &&
            prompt == other.prompt &&
            openingMessage == other.openingMessage &&
            avatarBytes.contentEquals(other.avatarBytes)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + prompt.hashCode()
        result = 31 * result + openingMessage.hashCode()
        result = 31 * result + avatarBytes.contentHashCode()
        return result
    }
}

data class CharacterFiles(
    val id: String,
    val name: String,
    val promptBytes: ByteArray,
    val openingMessage: String,
    val avatarBytes: ByteArray,
)

interface CharacterStorage {
    suspend fun initializeDefaults(defaults: List<CharacterFiles>)

    suspend fun loadCharacters(): List<CharacterFiles>

    suspend fun saveCharacter(character: CharacterFiles)
}

interface CharacterRepository {
    suspend fun loadCharacters(): List<Character>

    suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarBytes: ByteArray,
    ): Character
}

class DefaultCharacterRepository(
    private val storage: CharacterStorage,
    private val defaultAssets: suspend () -> List<CharacterFiles> = ::loadDefaultCharacterAssets,
) : CharacterRepository {
    override suspend fun loadCharacters(): List<Character> {
        storage.initializeDefaults(defaultAssets())
        return storage.loadCharacters().map(CharacterFiles::toCharacter)
    }

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarBytes: ByteArray,
    ): Character {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Character name cannot be blank." }
        require('/' !in normalizedName && '\\' !in normalizedName && '\n' !in normalizedName) {
            "Character name cannot contain path separators or line breaks."
        }
        val files = CharacterFiles(
            id = normalizedName,
            name = normalizedName,
            promptBytes = prompt.encodeToByteArray(),
            openingMessage = "",
            avatarBytes = avatarBytes,
        )
        storage.saveCharacter(files)
        return files.toCharacter()
    }
}

private fun CharacterFiles.toCharacter() = Character(
    id = id,
    name = name,
    prompt = promptBytes.decodeToString(),
    openingMessage = openingMessage,
    avatarBytes = avatarBytes,
)

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadDefaultCharacterAssets(): List<CharacterFiles> {
    val openingMessage = Res.readBytes("files/characters/opening_message.txt")
        .decodeToString()
        .trim()
    return DefaultCharacterNames.map { name ->
        CharacterFiles(
            id = "builtin:$name",
            name = name,
            promptBytes = Res.readBytes("files/characters/$name.md"),
            openingMessage = openingMessage,
            avatarBytes = Res.readBytes("files/characters/$name.webp"),
        )
    }
}

private val DefaultCharacterNames = listOf(
    "流萤",
    "三月七",
    "黄泉",
    "瑕蝶",
)
