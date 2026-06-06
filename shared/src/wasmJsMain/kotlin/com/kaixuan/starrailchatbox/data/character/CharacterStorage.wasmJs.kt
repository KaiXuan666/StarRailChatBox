package com.kaixuan.starrailchatbox.data.character

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.browser.localStorage

actual fun createCharacterStorage(directoryPath: String?): CharacterStorage {
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
            CharacterFiles(
                name = name,
                promptBytes = Base64.decode(prompt),
                avatarBytes = Base64.decode(avatar),
            )
        }.sortedBy(CharacterFiles::name)
    }

    override suspend fun saveCharacter(character: CharacterFiles) {
        localStorage.setItem("$prefix${character.name}.md", Base64.encode(character.promptBytes))
        localStorage.setItem("$prefix${character.name}.webp", Base64.encode(character.avatarBytes))
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
