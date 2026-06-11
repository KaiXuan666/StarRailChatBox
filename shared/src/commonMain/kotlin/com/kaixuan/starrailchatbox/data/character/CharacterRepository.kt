package com.kaixuan.starrailchatbox.data.character

import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.ExperimentalResourceApi
import starrailchatbox.shared.generated.resources.Res
import kotlin.time.Clock

@Serializable
data class Character(
    val id: String,
    val name: String,
    val description: String = "",
    val prompt: String,
    val openingMessage: String,
    val avatarUri: String,
    val voiceSampleUri: String? = null,
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
    val createdAt: Long = 0L,
    val sortOrder: Int = 0,
    val lastMessageAt: Long? = null,
)

@Serializable
data class CharacterFiles(
    val id: String,
    val name: String,
    val description: String = "",
    val prompt: String,
    val openingMessage: String,
    val avatarUri: String,
    val voiceSampleUri: String? = null,
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val sortOrder: Int = 0,
    val lastMessageAt: Long? = null,
)

data class CharacterAvatarSource(
    val uri: String,
    val name: String? = null,
    val extension: String? = null,
)

data class DefaultCharacterAsset(
    val id: String,
    val name: String,
    val description: String = "",
    val prompt: String,
    val openingMessage: String,
    val avatarContent: ByteArray,
    val voiceSampleContent: ByteArray? = null,
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

    suspend fun getDefaultCharacter(id: String): Character?
}

class DefaultCharacterRepository(
    private val storage: CharacterStorage,
    private val fileManager: com.kaixuan.starrailchatbox.platform.KmpFileManager = com.kaixuan.starrailchatbox.platform.KmpFileManager.Default,
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
            description = "",
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
            description = character.description,
            prompt = character.prompt,
            openingMessage = character.openingMessage,
            avatarUri = character.avatarUri,
            voiceSampleUri = character.voiceSampleUri,
            temperature = character.temperature.coerceIn(0.0, 2.0),
            topP = character.topP.coerceIn(0.0, 1.0),
            createdAt = character.createdAt,
            sortOrder = character.sortOrder,
            lastMessageAt = character.lastMessageAt,
        )
        return storage.saveCharacter(files, avatarSource).toCharacter()
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        storage.updateSortOrder(id, sortOrder)
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        storage.deleteCharacter(id, deletedAt)
    }

    override suspend fun getDefaultCharacter(id: String): Character? {
        val asset = defaultAssets().find { it.id == id } ?: return null
        
        val avatarUri = run {
            val fileName = characterAvatarFileName(asset.id)
            val relativePath = "character_avatars/$fileName"
            fileManager.writeBytes(relativePath, asset.avatarContent)
            (fileManager.appDataDir / relativePath.toPath()).toString()
        }

        val voiceSampleUri = asset.voiceSampleContent?.let { bytes ->
            val fileName = characterVoiceSampleFileName(asset.id)
            val relativePath = "character_voice_samples/$fileName"
            fileManager.writeBytes(relativePath, bytes)
            (fileManager.appDataDir / relativePath.toPath()).toString()
        }

        return Character(
            id = asset.id,
            name = asset.name,
            description = asset.description,
            prompt = asset.prompt,
            openingMessage = asset.openingMessage,
            avatarUri = avatarUri,
            voiceSampleUri = voiceSampleUri,
            temperature = asset.temperature,
            topP = asset.topP,
        )
    }
}

private fun CharacterFiles.toCharacter() = Character(
    id = id,
    name = name,
    description = description,
    prompt = prompt,
    openingMessage = openingMessage,
    avatarUri = avatarUri,
    voiceSampleUri = voiceSampleUri,
    temperature = temperature,
    topP = topP,
    createdAt = createdAt,
    sortOrder = sortOrder,
    lastMessageAt = lastMessageAt,
)

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadDefaultCharacterAssets(): List<DefaultCharacterAsset> {
    val openingMessage = Res.readBytes("files/characters/opening_message.txt")
        .decodeToString()
        .trim()
    return DefaultCharacterNames.map { name ->
        val voiceSampleContent = try {
            Res.readBytes("files/characters/$name.mp3")
        } catch (_: Exception) {
            null
        }
        DefaultCharacterAsset(
            id = "builtin:$name",
            name = name,
            description = try {
                Res.readBytes("files/characters/$name.txt").decodeToString().trim()
            } catch (_: Exception) {
                ""
            },
            prompt = Res.readBytes("files/characters/$name.md").decodeToString(),
            openingMessage = openingMessage,
            avatarContent = Res.readBytes("files/characters/$name.png"),
            voiceSampleContent = voiceSampleContent,
        )
    }
}

private val DefaultCharacterNames = listOf(
    "流萤",
    "三月七",
    "黄泉",
    "瑕蝶",
)

fun characterAvatarFileName(characterId: String): String {
    return characterId.encodeToByteArray()
        .joinToString(separator = "") { byte -> byte.toUByte().toString(16).padStart(2, '0') }
        .plus(".png")
}

fun characterVoiceSampleFileName(characterId: String, extension: String = "mp3"): String {
    return characterId.encodeToByteArray()
        .joinToString(separator = "") { byte -> byte.toUByte().toString(16).padStart(2, '0') }
        .plus(".$extension")
}