@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.kaixuan.starrailchatbox.data.character

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

class WasmJsCharacterStorage : CharacterStorage {
    private val key = "characters"

    private fun getStoredCharacters(): List<CharacterFiles> {
        val json = localStorage.getItem(key) ?: return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveStoredCharacters(characters: List<CharacterFiles>) {
        val json = Json.encodeToString(characters)
        localStorage.setItem(key, json)
    }

    override suspend fun initializeDefaults(defaults: List<DefaultCharacterAsset>) {
        val current = getStoredCharacters()
        val missing = defaults.filter { d -> current.none { it.id == d.id } }
        if (missing.isNotEmpty()) {
            val newDefaults = missing.mapIndexed { index, d ->
                CharacterFiles(
                    id = d.id,
                    name = d.name,
                    description = d.description,
                    prompt = d.prompt,
                    openingMessage = d.openingMessage,
                    avatarUri = "data:image/webp;base64," + Base64.encode(d.avatarContent),
                    temperature = d.temperature,
                    topP = d.topP,
                    sortOrder = current.size + index
                )
            }
            saveStoredCharacters(current + newDefaults)
        }
    }

    override suspend fun loadCharacters(): List<CharacterFiles> = getStoredCharacters()

    override suspend fun loadCharacterSummaries(): List<CharacterSummary> =
        getStoredCharacters().map {
            CharacterSummary(it.id, it.name, it.avatarUri, it.lastMessageAt)
        }

    override suspend fun getCharacter(id: String): CharacterFiles? = loadCharacters().find { it.id == id }

    override suspend fun saveCharacter(
        character: CharacterFiles,
        avatarSource: CharacterAvatarSource?
    ): CharacterFiles {
        val avatarUri = avatarSource?.uri ?: character.avatarUri
        val finalCharacter = character.copy(avatarUri = avatarUri)
        val current = getStoredCharacters().toMutableList()
        val index = current.indexOfFirst { it.id == character.id }
        if (index >= 0) {
            current[index] = finalCharacter
        } else {
            current.add(finalCharacter)
        }
        saveStoredCharacters(current)
        return finalCharacter
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        val current = getStoredCharacters()
        val updated = current.map { if (it.id == id) it.copy(sortOrder = sortOrder) else it }
        saveStoredCharacters(updated)
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        val current = getStoredCharacters()
        val filtered = current.filterNot { it.id == id }
        saveStoredCharacters(filtered)
    }
}
