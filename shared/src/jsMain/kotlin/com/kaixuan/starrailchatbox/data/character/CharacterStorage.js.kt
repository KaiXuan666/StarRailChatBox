package com.kaixuan.starrailchatbox.data.character

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.browser.localStorage

fun createCharacterStorage(directoryPath: String? = null): CharacterStorage {
    return BrowserCharacterStorage(directoryPath ?: "characters")
}

@OptIn(ExperimentalEncodingApi::class)
private class BrowserCharacterStorage(
    directoryName: String,
) : CharacterStorage {
    private val prefix = "starrailchatbox/$directoryName/"
    private val initializedKey = "${prefix}.initialized"
    private val namesKey = "${prefix}.names"

    override suspend fun initializeDefaults(defaults: List<DefaultCharacterAsset>) {
        if (localStorage.getItem(initializedKey) != null) return
        defaults.forEachIndexed { index, it ->
            saveCharacter(
                CharacterFiles(
                    id = it.id,
                    name = it.name,
                    prompt = it.prompt,
                    openingMessage = it.openingMessage,
                    avatarUri = "data:image/webp;base64,${Base64.encode(it.avatarContent)}",
                    temperature = it.temperature,
                    topP = it.topP,
                    sortOrder = index,
                ),
                avatarSource = null,
            )
        }
        localStorage.setItem(initializedKey, "1")
    }

    override suspend fun loadCharacters(): List<CharacterFiles> {
        return loadNames().mapNotNull { id ->
            val prompt = localStorage.getItem("$prefix$id.prompt")
            val legacyPrompt = localStorage.getItem("$prefix$id.md")
            val storedPrompt = prompt ?: legacyPrompt ?: return@mapNotNull null
            val avatarUri = localStorage.getItem("$prefix$id.avatarUri")
                ?: localStorage.getItem("$prefix$id.webp")?.let { "data:image/webp;base64,$it" }
                ?: return@mapNotNull null
            val name = localStorage.getItem("$prefix$id.name") ?: id
            val openingMessage = localStorage.getItem("$prefix$id.opening").orEmpty()
            val temperature = localStorage.getItem("$prefix$id.temperature")
                ?.toDoubleOrNull()
                ?: 0.85
            val topP = localStorage.getItem("$prefix$id.topP")
                ?.toDoubleOrNull()
                ?: 0.9
            val createdAt = localStorage.getItem("$prefix$id.createdAt")
                ?.toLongOrNull()
                ?: 0L
            val sortOrder = localStorage.getItem("$prefix$id.sortOrder")
                ?.toIntOrNull()
                ?: 0
            CharacterFiles(
                id = id,
                name = name,
                prompt = (if (prompt != null) {
                    storedPrompt
                } else {
                    Base64.decode(storedPrompt).decodeToString()
                }).take(20),
                openingMessage = openingMessage,
                avatarUri = avatarUri,
                temperature = temperature,
                topP = topP,
                createdAt = createdAt,
                sortOrder = sortOrder,
            )
        }.sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
    }

    override suspend fun getCharacter(id: String): CharacterFiles? {
        val prompt = localStorage.getItem("$prefix$id.prompt")
        val legacyPrompt = localStorage.getItem("$prefix$id.md")
        val storedPrompt = prompt ?: legacyPrompt ?: return null
        val avatarUri = localStorage.getItem("$prefix$id.avatarUri")
            ?: localStorage.getItem("$prefix$id.webp")?.let { "data:image/webp;base64,$it" }
            ?: return null
        val name = localStorage.getItem("$prefix$id.name") ?: id
        val openingMessage = localStorage.getItem("$prefix$id.opening").orEmpty()
        val temperature = localStorage.getItem("$prefix$id.temperature")
            ?.toDoubleOrNull()
            ?: 0.85
        val topP = localStorage.getItem("$prefix$id.topP")
            ?.toDoubleOrNull()
            ?: 0.9
        val createdAt = localStorage.getItem("$prefix$id.createdAt")
            ?.toLongOrNull()
            ?: 0L
        val sortOrder = localStorage.getItem("$prefix$id.sortOrder")
            ?.toIntOrNull()
            ?: 0
        return CharacterFiles(
            id = id,
            name = name,
            prompt = if (prompt != null) {
                storedPrompt
            } else {
                Base64.decode(storedPrompt).decodeToString()
            },
            openingMessage = openingMessage,
            avatarUri = avatarUri,
            temperature = temperature,
            topP = topP,
            createdAt = createdAt,
            sortOrder = sortOrder,
        )
    }

    override suspend fun saveCharacter(
        character: CharacterFiles,
        avatarSource: CharacterAvatarSource?,
    ): CharacterFiles {
        val existingSortOrder = localStorage.getItem("$prefix${character.id}.sortOrder")?.toIntOrNull()
        val sortOrder = if (existingSortOrder != null) {
            existingSortOrder
        } else {
            val maxSortOrder = loadCharacters().maxOfOrNull { it.sortOrder } ?: -1
            maxSortOrder + 1
        }
        val saved = character.copy(
            avatarUri = avatarSource?.uri ?: character.avatarUri,
            sortOrder = sortOrder,
        )
        localStorage.setItem("$prefix${character.id}.name", character.name)
        localStorage.setItem("$prefix${character.id}.prompt", character.prompt)
        localStorage.setItem("$prefix${character.id}.opening", character.openingMessage)
        localStorage.setItem("$prefix${character.id}.avatarUri", saved.avatarUri)
        localStorage.setItem("$prefix${character.id}.temperature", character.temperature.toString())
        localStorage.setItem("$prefix${character.id}.topP", character.topP.toString())
        localStorage.setItem("$prefix${character.id}.createdAt", character.createdAt.toString())
        localStorage.setItem("$prefix${character.id}.sortOrder", sortOrder.toString())
        val names = (loadNames() + character.id).distinct().sorted()
        localStorage.setItem(namesKey, names.joinToString("\n"))
        return saved
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        if (localStorage.getItem("$prefix$id.name") == null) {
            throw IllegalArgumentException("Character does not exist: $id")
        }
        localStorage.setItem("$prefix$id.sortOrder", sortOrder.toString())
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        listOf("name", "md", "prompt", "opening", "webp", "avatarUri", "temperature", "topP", "createdAt", "sortOrder").forEach { suffix ->
            localStorage.removeItem("$prefix$id.$suffix")
        }
        localStorage.setItem(namesKey, loadNames().filterNot { it == id }.joinToString("\n"))
    }

    private fun loadNames(): List<String> {
        return localStorage.getItem(namesKey)
            ?.lineSequence()
            ?.filter(String::isNotBlank)
            ?.toList()
            .orEmpty()
    }
}
