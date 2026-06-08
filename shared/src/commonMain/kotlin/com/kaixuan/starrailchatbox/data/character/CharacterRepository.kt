package com.kaixuan.starrailchatbox.data.character

import org.jetbrains.compose.resources.ExperimentalResourceApi
import starrailchatbox.shared.generated.resources.Res
import kotlin.time.Clock

data class Character(
    val id: String,
    val name: String,
    val prompt: String,
    val openingMessage: String,
    val avatarUri: String,
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
    val createdAt: Long = 0L,
    val sortOrder: Int = 0,
)

data class CharacterFiles(
    val id: String,
    val name: String,
    val prompt: String,
    val openingMessage: String,
    val avatarUri: String,
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val sortOrder: Int = 0,
)

data class CharacterAvatarSource(
    val uri: String,
)

data class DefaultCharacterAsset(
    val id: String,
    val name: String,
    val prompt: String,
    val openingMessage: String,
    val avatarContent: ByteArray,
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
)

interface CharacterStorage {
    suspend fun initializeDefaults(defaults: List<DefaultCharacterAsset>)

    suspend fun loadCharacters(): List<CharacterFiles>

    suspend fun getCharacter(id: String): CharacterFiles?

    suspend fun saveCharacter(
        character: CharacterFiles,
        avatarSource: CharacterAvatarSource?,
    ): CharacterFiles

    suspend fun updateSortOrder(id: String, sortOrder: Int)

    suspend fun deleteCharacter(id: String, deletedAt: Long)
}

interface CharacterRepository {
    suspend fun loadCharacters(): List<Character>

    suspend fun getCharacter(id: String): Character?

    suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarSource: CharacterAvatarSource?,
    ): Character

    suspend fun updateCharacter(
        character: Character,
        avatarSource: CharacterAvatarSource? = null,
    ): Character

    suspend fun updateSortOrder(id: String, sortOrder: Int)

    suspend fun deleteCharacter(id: String, deletedAt: Long)
}

class DefaultCharacterRepository(
    private val storage: CharacterStorage,
    private val defaultAssets: suspend () -> List<DefaultCharacterAsset> = ::loadDefaultCharacterAssets,
) : CharacterRepository {
    override suspend fun loadCharacters(): List<Character> {
        storage.initializeDefaults(defaultAssets())
        return storage.loadCharacters().map(CharacterFiles::toCharacter)
    }

    override suspend fun getCharacter(id: String): Character? {
        return storage.getCharacter(id)?.toCharacter()
    }

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarSource: CharacterAvatarSource?,
    ): Character {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Character name cannot be blank." }
        require('/' !in normalizedName && '\\' !in normalizedName && '\n' !in normalizedName) {
            "Character name cannot contain path separators or line breaks."
        }
        val files = CharacterFiles(
            id = normalizedName,
            name = normalizedName,
            prompt = prompt,
            openingMessage = "",
            avatarUri = avatarSource?.uri.orEmpty(),
        )
        return storage.saveCharacter(files, avatarSource).toCharacter()
    }

    override suspend fun updateCharacter(
        character: Character,
        avatarSource: CharacterAvatarSource?,
    ): Character {
        val normalizedName = character.name.trim()
        require(normalizedName.isNotEmpty()) { "Character name cannot be blank." }
        require('/' !in normalizedName && '\\' !in normalizedName && '\n' !in normalizedName) {
            "Character name cannot contain path separators or line breaks."
        }
        val files = CharacterFiles(
            id = character.id,
            name = normalizedName,
            prompt = character.prompt,
            openingMessage = character.openingMessage,
            avatarUri = character.avatarUri,
            temperature = character.temperature.coerceIn(0.0, 2.0),
            topP = character.topP.coerceIn(0.0, 1.0),
            createdAt = character.createdAt,
            sortOrder = character.sortOrder,
        )
        return storage.saveCharacter(files, avatarSource).toCharacter()
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        storage.updateSortOrder(id, sortOrder)
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        storage.deleteCharacter(id, deletedAt)
    }
}

private fun CharacterFiles.toCharacter() = Character(
    id = id,
    name = name,
    prompt = prompt,
    openingMessage = openingMessage,
    avatarUri = avatarUri,
    temperature = temperature,
    topP = topP,
    createdAt = createdAt,
    sortOrder = sortOrder,
)

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadDefaultCharacterAssets(): List<DefaultCharacterAsset> {
    val openingMessage = Res.readBytes("files/characters/opening_message.txt")
        .decodeToString()
        .trim()
    return DefaultCharacterNames.map { name ->
        DefaultCharacterAsset(
            id = "builtin:$name",
            name = name,
            prompt = Res.readBytes("files/characters/$name.md").decodeToString(),
            openingMessage = openingMessage,
            avatarContent = Res.readBytes("files/characters/$name.webp"),
        )
    }
}

private val DefaultCharacterNames = listOf(
    "流萤",
    "三月七",
    "黄泉",
    "瑕蝶",
)
