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
        return loadNames().mapNotNull { name ->
            val prompt = localStorage.getItem("$prefix$name.md") ?: return@mapNotNull null
            val avatar = localStorage.getItem("$prefix$name.webp") ?: return@mapNotNull null
            val openingMessage = localStorage.getItem("$prefix$name.opening").orEmpty()
            val createdAt = localStorage.getItem("$prefix$name.createdAt")
                ?.toLongOrNull()
                ?: 0L
            CharacterFiles(
                id = name,
                name = name,
                promptBytes = Base64.decode(prompt),
                openingMessage = openingMessage,
                avatarBytes = Base64.decode(avatar),
                createdAt = createdAt,
            )
        }.sortedBy(CharacterFiles::name)
    }

    override suspend fun saveCharacter(character: CharacterFiles) {
        localStorage.setItem("$prefix${character.name}.md", Base64.encode(character.promptBytes))
        localStorage.setItem("$prefix${character.name}.opening", character.openingMessage)
        localStorage.setItem("$prefix${character.name}.webp", Base64.encode(character.avatarBytes))
        localStorage.setItem("$prefix${character.name}.createdAt", character.createdAt.toString())
        val names = (loadNames() + character.name).distinct().sorted()
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
