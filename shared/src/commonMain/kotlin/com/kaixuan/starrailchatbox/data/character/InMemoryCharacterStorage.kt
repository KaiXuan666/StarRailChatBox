package com.kaixuan.starrailchatbox.data.character

class InMemoryCharacterStorage : CharacterStorage {
    private val characters = linkedMapOf<String, CharacterFiles>()
    private var initialized = false

    override suspend fun initializeDefaults(defaults: List<DefaultCharacterAsset>) {
        if (initialized) return
        defaults.forEach {
            if (it.id !in characters) {
                characters[it.id] = CharacterFiles(
                    id = it.id,
                    name = it.name,
                    prompt = it.prompt,
                    openingMessage = it.openingMessage,
                    avatarUri = "memory:${it.id}",
                    temperature = it.temperature,
                    topP = it.topP,
                )
            }
        }
        initialized = true
    }

    override suspend fun loadCharacters(): List<CharacterFiles> {
        return characters.values.toList()
    }

    override suspend fun saveCharacter(
        character: CharacterFiles,
        avatarSource: CharacterAvatarSource?,
    ): CharacterFiles {
        val saved = character.copy(
            avatarUri = avatarSource?.uri ?: character.avatarUri,
        )
        characters[character.id] = saved
        return saved
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        characters.remove(id)
    }
}
