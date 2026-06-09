package com.kaixuan.starrailchatbox.data.character

import io.github.xxfast.kstore.storage.storeOf

class WasmJsCharacterStorage : CharacterStorage {
    private val kStore = storeOf<List<CharacterFiles>>(key = "characters")

    override suspend fun initializeDefaults(defaults: List<DefaultCharacterAsset>) {
        val current = kStore.get() ?: emptyList()
        val missing = defaults.filter { d -> current.none { it.id == d.id } }
        if (missing.isNotEmpty()) {
            val newDefaults = missing.mapIndexed { index, d ->
                CharacterFiles(
                    id = d.id,
                    name = d.name,
                    prompt = d.prompt,
                    openingMessage = d.openingMessage,
                    avatarUri = "data:image/webp;base64," + com.kaixuan.starrailchatbox.data.settings.encodeBase64(d.avatarContent),
                    temperature = d.temperature,
                    topP = d.topP,
                    sortOrder = current.size + index
                )
            }
            kStore.set(current + newDefaults)
        }
    }

    override suspend fun loadCharacters(): List<CharacterFiles> = kStore.get() ?: emptyList()

    override suspend fun getCharacter(id: String): CharacterFiles? = loadCharacters().find { it.id == id }

    override suspend fun saveCharacter(
        character: CharacterFiles,
        avatarSource: CharacterAvatarSource?
    ): CharacterFiles {
        val avatarUri = avatarSource?.uri ?: character.avatarUri
        val finalCharacter = character.copy(avatarUri = avatarUri)
        kStore.update { current ->
            val list = current?.toMutableList() ?: mutableListOf()
            val index = list.indexOfFirst { it.id == character.id }
            if (index >= 0) {
                list[index] = finalCharacter
            } else {
                list.add(finalCharacter)
            }
            list
        }
        return finalCharacter
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        kStore.update { current ->
            current?.map { if (it.id == id) it.copy(sortOrder = sortOrder) else it }
        }
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        kStore.update { current ->
            current?.filterNot { it.id == id }
        }
    }
}
