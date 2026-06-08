package com.kaixuan.starrailchatbox.data.character

class InMemoryCharacterStorage : CharacterStorage {
    private val characters = linkedMapOf<String, CharacterFiles>()
    private var initialized = false

    override suspend fun initializeDefaults(defaults: List<DefaultCharacterAsset>) {
        if (initialized) return
        defaults.forEachIndexed { index, it ->
            if (it.id !in characters) {
                characters[it.id] = CharacterFiles(
                    id = it.id,
                    name = it.name,
                    prompt = it.prompt,
                    openingMessage = it.openingMessage,
                    avatarUri = "memory:${it.id}",
                    temperature = it.temperature,
                    topP = it.topP,
                    sortOrder = index,
                )
            }
        }
        initialized = true
    }

    override suspend fun loadCharacters(): List<CharacterFiles> {
        return characters.values
            .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
            .map { it.copy(prompt = it.prompt.take(20)) }
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
        return saved
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        val existing = characters[id] ?: throw IllegalArgumentException("Character does not exist: $id")
        characters[id] = existing.copy(sortOrder = sortOrder)
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        characters.remove(id)
    }
}
