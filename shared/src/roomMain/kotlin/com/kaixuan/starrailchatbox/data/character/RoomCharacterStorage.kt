package com.kaixuan.starrailchatbox.data.character

import com.kaixuan.starrailchatbox.data.database.dao.AgentRoleDao
import com.kaixuan.starrailchatbox.data.database.entity.AgentRoleEntity

class RoomCharacterStorage(
    private val dao: AgentRoleDao,
    private val avatarStorage: CharacterAvatarStorage,
    private val voiceSampleStorage: CharacterVoiceSampleStorage,
    private val currentTimeMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : CharacterStorage {
    override suspend fun initializeDefaults(defaults: List<DefaultCharacterAsset>) {
        val now = currentTimeMillis()
        val missingRoles = defaults.mapIndexedNotNull { index, character ->
            if (dao.containsId(character.id)) {
                null
            } else {
                val avatarUri = avatarStorage.saveBytes(character.id, character.avatarContent)
                val voiceSampleUri = character.voiceSampleContent?.let { bytes ->
                    voiceSampleStorage.saveBytes(character.id, bytes)
                }
                AgentRoleEntity(
                    id = character.id,
                    name = character.name,
                    avatarUri = avatarUri,
                    description = "",
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
        return dao.findAll().map { entity -> entity.toCharacterFiles() }
    }

    override suspend fun getCharacter(id: String): CharacterFiles? {
        return dao.findById(id)?.toCharacterFiles()
    }

    override suspend fun saveCharacter(
        character: CharacterFiles,
        avatarSource: CharacterAvatarSource?,
    ): CharacterFiles {
        val existing = dao.findById(character.id)
        val now = currentTimeMillis()
        val oldAvatarUri = existing?.avatarUri
        val avatarUri = avatarSource
            ?.let { avatarStorage.copyFrom(character.id, it.uri) }
            ?: character.avatarUri.takeIf(String::isNotBlank)
            ?: oldAvatarUri.orEmpty()
        val sortOrder = if (existing != null) {
            existing.sortOrder
        } else {
            val maxSortOrder = dao.findMaxSortOrder() ?: -1
            maxSortOrder + 1
        }
        dao.upsert(
            character.toEntity(
                avatarUri = avatarUri,
                isBuiltin = existing?.isBuiltin ?: false,
                sortOrder = sortOrder,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
        if (avatarSource != null && oldAvatarUri != null && oldAvatarUri != avatarUri) {
            avatarStorage.delete(oldAvatarUri)
        }
        return character.copy(avatarUri = avatarUri, voiceSampleUri = character.voiceSampleUri, sortOrder = sortOrder)
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
        avatarStorage.delete(existing.avatarUri)
        existing.voiceSampleUri?.let { voiceSampleStorage.delete(it) }
    }

    private fun CharacterFiles.toEntity(
        avatarUri: String,
        isBuiltin: Boolean,
        sortOrder: Int,
        createdAt: Long,
        updatedAt: Long = createdAt,
    ) = AgentRoleEntity(
        id = id,
        name = name,
        avatarUri = avatarUri,
        description = "",
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
