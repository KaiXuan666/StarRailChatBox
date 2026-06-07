package com.kaixuan.starrailchatbox.data.character

import org.jetbrains.compose.resources.ExperimentalResourceApi
import starrailchatbox.shared.generated.resources.Res
import kotlin.time.Clock

data class Character(
    val id: String,
    val name: String,
    val prompt: String,
    val openingMessage: String,
    val avatarBytes: ByteArray,
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
    val createdAt: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Character) return false
        return id == other.id &&
            name == other.name &&
            prompt == other.prompt &&
            openingMessage == other.openingMessage &&
            avatarBytes.contentEquals(other.avatarBytes) &&
            temperature == other.temperature &&
            topP == other.topP &&
            createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + prompt.hashCode()
        result = 31 * result + openingMessage.hashCode()
        result = 31 * result + avatarBytes.contentHashCode()
        result = 31 * result + temperature.hashCode()
        result = 31 * result + topP.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

data class CharacterFiles(
    val id: String,
    val name: String,
    val promptBytes: ByteArray,
    val openingMessage: String,
    val avatarBytes: ByteArray,
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
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

    suspend fun updateCharacter(character: Character): Character
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

    override suspend fun updateCharacter(character: Character): Character {
        val normalizedName = character.name.trim()
        require(normalizedName.isNotEmpty()) { "Character name cannot be blank." }
        require('/' !in normalizedName && '\\' !in normalizedName && '\n' !in normalizedName) {
            "Character name cannot contain path separators or line breaks."
        }
        val files = CharacterFiles(
            id = character.id,
            name = normalizedName,
            promptBytes = character.prompt.encodeToByteArray(),
            openingMessage = character.openingMessage,
            avatarBytes = character.avatarBytes,
            temperature = character.temperature.coerceIn(0.0, 2.0),
            topP = character.topP.coerceIn(0.0, 1.0),
            createdAt = character.createdAt,
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
    temperature = temperature,
    topP = topP,
    createdAt = createdAt,
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
