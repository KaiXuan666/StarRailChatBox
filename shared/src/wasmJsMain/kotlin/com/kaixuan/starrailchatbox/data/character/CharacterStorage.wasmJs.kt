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

    override suspend fun initializeDefaults(defaults: List<CharacterFiles>) {
        if (localStorage.getItem(initializedKey) != null) return
        defaults.forEach { saveCharacter(it) }
        localStorage.setItem(initializedKey, "1")
    }

    override suspend fun loadCharacters(): List<CharacterFiles> {
        return loadNames().mapNotNull { id ->
            val prompt = localStorage.getItem("$prefix$id.md") ?: return@mapNotNull null
            val avatar = localStorage.getItem("$prefix$id.webp") ?: return@mapNotNull null
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
            CharacterFiles(
                id = id,
                name = name,
                promptBytes = Base64.decode(prompt),
                openingMessage = openingMessage,
                avatarBytes = Base64.decode(avatar),
                temperature = temperature,
                topP = topP,
                createdAt = createdAt,
            )
        }.sortedBy(CharacterFiles::name)
    }

    override suspend fun saveCharacter(character: CharacterFiles) {
        localStorage.setItem("$prefix${character.id}.name", character.name)
        localStorage.setItem("$prefix${character.id}.md", Base64.encode(character.promptBytes))
        localStorage.setItem("$prefix${character.id}.opening", character.openingMessage)
        localStorage.setItem("$prefix${character.id}.webp", Base64.encode(character.avatarBytes))
        localStorage.setItem("$prefix${character.id}.temperature", character.temperature.toString())
        localStorage.setItem("$prefix${character.id}.topP", character.topP.toString())
        localStorage.setItem("$prefix${character.id}.createdAt", character.createdAt.toString())
        val names = (loadNames() + character.id).distinct().sorted()
        localStorage.setItem(namesKey, names.joinToString("\n"))
    }

    private fun loadNames(): List<String> {
        return localStorage.getItem(namesKey)
            ?.lineSequence()
            ?.filter(String::isNotBlank)
            ?.toList()
            .orEmpty()
    }
}
