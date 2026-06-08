package com.kaixuan.starrailchatbox.data.character

import com.kaixuan.starrailchatbox.data.database.dao.AgentRoleDao
import com.kaixuan.starrailchatbox.data.database.entity.AgentRoleEntity

class RoomCharacterStorage(
    private val dao: AgentRoleDao,
    private val avatarStorage: CharacterAvatarStorage,
    private val currentTimeMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : CharacterStorage {
    override suspend fun initializeDefaults(defaults: List<CharacterFiles>) {
        val now = currentTimeMillis()
        val missingRoles = defaults.mapIndexedNotNull { index, character ->
            if (dao.containsId(character.id)) {
                null
            } else {
                val avatarUri = avatarStorage.save(character.id, character.avatarBytes)
                character.toEntity(
                    avatarUri = avatarUri,
                    isBuiltin = true,
                    sortOrder = index,
                    createdAt = now,
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

    override suspend fun saveCharacter(character: CharacterFiles) {
        val existing = dao.findById(character.id)
        val now = currentTimeMillis()
        val avatarUri = avatarStorage.save(character.id, character.avatarBytes)
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
        systemPrompt = promptBytes.decodeToString(),
        openingMessage = openingMessage,
        temperature = temperature,
        topP = topP,
        sortOrder = sortOrder,
        isBuiltin = isBuiltin,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun AgentRoleEntity.toCharacterFiles() = CharacterFiles(
        id = id,
        name = name,
        promptBytes = systemPrompt.encodeToByteArray(),
        openingMessage = openingMessage,
        avatarBytes = avatarStorage.read(avatarUri),
        temperature = temperature,
        topP = topP,
        createdAt = createdAt,
    )
}
