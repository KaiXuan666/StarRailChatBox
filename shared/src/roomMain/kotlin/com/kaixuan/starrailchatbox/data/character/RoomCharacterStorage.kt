package com.kaixuan.starrailchatbox.data.character

import com.kaixuan.starrailchatbox.data.database.dao.AgentRoleDao
import com.kaixuan.starrailchatbox.data.database.entity.AgentRoleEntity
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.github.aakira.napier.Napier
import okio.Path
import okio.Path.Companion.toPath

class RoomCharacterStorage(
    private val dao: AgentRoleDao,
    private val fileManager: KmpFileManager = KmpFileManager.Default,
    private val currentTimeMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : CharacterStorage {
    override suspend fun initializeDefaults(defaults: List<DefaultCharacterAsset>) {
        val now = currentTimeMillis()
        val missingRoles = defaults.mapIndexedNotNull { index, character ->
            if (dao.containsId(character.id)) {
                null
            } else {
                val avatarFileName = characterAvatarFileName(character.id)
                val avatarRelativePath = "character_avatars/$avatarFileName"
                fileManager.writeBytes(avatarRelativePath, character.avatarContent)
                val avatarUri = (fileManager.appDataDir / avatarRelativePath.toPath()).toString()

                val voiceSampleUri = character.voiceSampleContent?.let { bytes ->
                    val voiceFileName = characterVoiceSampleFileName(character.id)
                    val voiceRelativePath = "character_voice_samples/$voiceFileName"
                    fileManager.writeBytes(voiceRelativePath, bytes)
                    (fileManager.appDataDir / voiceRelativePath.toPath()).toString()
                }
                AgentRoleEntity(
                    id = character.id,
                    name = character.name,
                    avatarUri = avatarUri,
                    description = character.description,
                    systemPrompt = character.prompt,
                    openingMessage = character.openingMessage,
                    temperature = character.temperature,
                    topP = character.topP,
                    sortOrder = index,
                    isBuiltin = true,
                    createdAt = now,
                    updatedAt = now,
                    voiceSampleUri = voiceSampleUri,
                )
            }
        }
        if (missingRoles.isNotEmpty()) {
            dao.insertIfMissing(missingRoles)
        }
    }

    override suspend fun loadCharacters(): List<CharacterFiles> {
        return dao.findAll().mapNotNull { summary ->
            dao.findById(summary.id)?.toCharacterFiles()?.copy(lastMessageAt = summary.lastMessageAt)
        }
    }

    override suspend fun loadCharacterSummaries(): List<CharacterSummary> {
        return dao.findAll().map {
            CharacterSummary(
                id = it.id,
                name = it.name,
                avatarUri = it.avatarUri,
                lastMessageAt = it.lastMessageAt,
            )
        }
    }

    override suspend fun getCharacter(id: String): CharacterFiles? {
        return dao.findById(id)?.toCharacterFiles()
    }

    private fun characterSafeFileName(characterId: String): String {
        return characterId.encodeToByteArray()
            .joinToString(separator = "") { byte -> byte.toUByte().toString(16).padStart(2, '0') }
    }

    override suspend fun saveCharacter(
        character: CharacterFiles,
        avatarSource: CharacterAvatarSource?,
    ): CharacterFiles {
        val existing = dao.findById(character.id)
        val now = currentTimeMillis()
        val oldAvatarUri = existing?.avatarUri
        
        val avatarUri = if (avatarSource != null) {
            val sourceUri = avatarSource.uri
            if (sourceUri.startsWith(fileManager.cacheDir.toString())) {
                val extension = avatarSource.extension ?: sourceUri.substringAfterLast('.', "png")
                val safeId = characterSafeFileName(character.id)
                val targetFileName = "${safeId}_${now}.$extension"
                val targetPath = fileManager.appDataDir / "character_avatars".toPath() / targetFileName.toPath()
                fileManager.move(sourceUri.toPath(), targetPath)
                targetPath.toString()
            } else {
                sourceUri.takeIf(String::isNotBlank) ?: oldAvatarUri.orEmpty()
            }
        } else {
            character.avatarUri.takeIf(String::isNotBlank) ?: oldAvatarUri.orEmpty()
        }

        val oldVoiceSampleUri = existing?.voiceSampleUri
        val voiceSampleUri = if (character.voiceSampleUri != oldVoiceSampleUri) {
            character.voiceSampleUri?.let { uri ->
                if (uri.startsWith(fileManager.cacheDir.toString())) {
                    val extension = uri.substringAfterLast('.', "mp3")
                    val safeId = characterSafeFileName(character.id)
                    val targetFileName = "${safeId}_${now}.$extension"
                    val targetPath = fileManager.appDataDir / "character_voice_samples".toPath() / targetFileName.toPath()
                    fileManager.move(uri.toPath(), targetPath)
                    targetPath.toString()
                } else {
                    uri
                }
            }
        } else {
            character.voiceSampleUri
        }

        val sortOrder = if (existing != null) {
            existing.sortOrder
        } else {
            val maxSortOrder = dao.findMaxSortOrder() ?: -1
            maxSortOrder + 1
        }
        dao.upsert(
            character.toEntity(
                avatarUri = avatarUri,
                voiceSampleUri = voiceSampleUri,
                isBuiltin = existing?.isBuiltin ?: false,
                sortOrder = sortOrder,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
        if (avatarSource != null && oldAvatarUri != null && oldAvatarUri != avatarUri) {
            Napier.d { "删除老文件判断 avatarSource=$avatarSource, oldAvatarUri+$oldAvatarUri avatarUri=$avatarUri" }
            deleteFileIfAppOwned(oldAvatarUri)
        }
        if (character.voiceSampleUri != oldVoiceSampleUri && oldVoiceSampleUri != null && oldVoiceSampleUri != voiceSampleUri) {
            Napier.d { "删除老语音判断 oldVoiceSampleUri=$oldVoiceSampleUri, voiceSampleUri=$voiceSampleUri" }
            deleteFileIfAppOwned(oldVoiceSampleUri)
        }
        return character.copy(avatarUri = avatarUri, voiceSampleUri = voiceSampleUri, sortOrder = sortOrder)
    }

    private fun deleteFileIfAppOwned(uri: String) {
        if (uri.startsWith(fileManager.appDataDir.toString())) {
            fileManager.delete(uri.toPath())
        }
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        val now = currentTimeMillis()
        check(dao.updateSortOrder(id, sortOrder, now) == 1) {
            "Character does not exist: $id"
        }
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        val existing = dao.findById(id) ?: return
        check(dao.softDelete(id, deletedAt) == 1) {
            "Character does not exist: $id"
        }
        deleteFileIfAppOwned(existing.avatarUri)
        existing.voiceSampleUri?.let { deleteFileIfAppOwned(it) }
    }

    private fun CharacterFiles.toEntity(
        avatarUri: String,
        voiceSampleUri: String?,
        isBuiltin: Boolean,
        sortOrder: Int,
        createdAt: Long,
        updatedAt: Long = createdAt,
    ) = AgentRoleEntity(
        id = id,
        name = name,
        avatarUri = avatarUri,
        description = description,
        systemPrompt = prompt,
        openingMessage = openingMessage,
        temperature = temperature,
        topP = topP,
        sortOrder = sortOrder,
        isBuiltin = isBuiltin,
        createdAt = createdAt,
        updatedAt = updatedAt,
        voiceSampleUri = voiceSampleUri,
    )

    private fun AgentRoleEntity.toCharacterFiles() = CharacterFiles(
        id = id,
        name = name,
        description = description,
        prompt = systemPrompt,
        openingMessage = openingMessage,
        avatarUri = avatarUri,
        voiceSampleUri = voiceSampleUri,
        temperature = temperature,
        topP = topP,
        createdAt = createdAt,
        sortOrder = sortOrder,
    )
}
