package com.kaixuan.starrailchatbox.data.character

class InMemoryCharacterStorage : CharacterStorage {
    private val characters = linkedMapOf<String, CharacterFiles>()
    private var initialized = false

    override suspend fun initializeDefaults(defaults: List<CharacterFiles>) {
        if (initialized) return
        defaults.forEach {
            if (it.name !in characters) {
                characters[it.name] = it
            }
        }
        initialized = true
    }

    override suspend fun loadCharacters(): List<CharacterFiles> {
        return characters.values.sortedBy(CharacterFiles::name)
    }

    override suspend fun saveCharacter(character: CharacterFiles) {
        characters[character.name] = character
    }
}
