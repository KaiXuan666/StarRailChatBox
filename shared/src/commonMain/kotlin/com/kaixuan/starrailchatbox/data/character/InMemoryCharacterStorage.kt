package com.kaixuan.starrailchatbox.data.character

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class InMemoryCharacterStorage : CharacterStorage {
    private val characters = linkedMapOf<String, CharacterFiles>()
    private val summaries = MutableSharedFlow<List<CharacterSummary>>(replay = 1).apply {
        tryEmit(emptyList())
    }
    private var initialized = false

    override suspend fun initializeDefaults(defaults: List<DefaultCharacterAsset>) {
        if (initialized) return
        defaults.forEachIndexed { index, it ->
            if (it.id !in characters) {
                characters[it.id] = CharacterFiles(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    prompt = it.prompt,
                    openingMessage = it.openingMessage,
                    avatarUri = "memory:${it.id}",
                    voiceSampleUri = it.voiceSampleContent?.let { _ -> "memory:voice:${it.id}" },
                    temperature = it.temperature,
                    topP = it.topP,
                    sortOrder = index,
                )
            }
        }
        initialized = true
        publishSummaries()
    }

    override suspend fun loadCharacters(): List<CharacterFiles> {
        return characters.values
            .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
            .toList()
    }

    override suspend fun loadCharacterSummaries(): List<CharacterSummary> {
        return currentSummaries()
    }

    override fun observeCharacterSummaries(): Flow<List<CharacterSummary>> = summaries

    private fun currentSummaries(): List<CharacterSummary> =
        characters.values
            .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
            .map {
                CharacterSummary(
                    id = it.id,
                    name = it.name,
                    avatarUri = it.avatarUri,
                    lastMessageAt = it.lastMessageAt,
                )
            }

    private fun publishSummaries() {
        summaries.tryEmit(currentSummaries())
    }

    override suspend fun getCharacter(id: String): CharacterFiles? {
        return characters[id]
    }

    override suspend fun saveCharacter(
        character: CharacterFiles,
        avatarSource: CharacterAvatarSource?,
    ): CharacterFiles {
        val existing = characters[character.id]
        val sortOrder = existing?.sortOrder ?: characters.size
        val saved = character.copy(
            avatarUri = avatarSource?.uri ?: character.avatarUri,
            sortOrder = sortOrder,
        )
        characters[character.id] = saved
        publishSummaries()
        return saved
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        val existing = characters[id] ?: throw IllegalArgumentException("Character does not exist: $id")
        characters[id] = existing.copy(sortOrder = sortOrder)
        publishSummaries()
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        characters.remove(id)
        publishSummaries()
    }
}
