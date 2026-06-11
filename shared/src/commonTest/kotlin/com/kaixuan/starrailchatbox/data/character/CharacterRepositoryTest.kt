package com.kaixuan.starrailchatbox.data.character

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class CharacterRepositoryTest {
    @Test
    fun injectedDefaultsCanBeLoaded() = runTest {
        val characters = DefaultCharacterRepository(InMemoryCharacterStorage()) {
            listOf(characterFiles("流萤"), characterFiles("黄泉"))
        }.loadCharacters()

        assertEquals("流萤", characters.first().name)
        assertEquals(setOf("流萤", "黄泉"), characters.map(Character::name).toSet())
        characters.forEach { character ->
            assertTrue(character.prompt.isNotBlank())
            assertTrue(character.openingMessage.isNotBlank())
            assertTrue(character.avatarUri.isNotBlank())
        }
    }

    @Test
    fun defaultsAreCopiedOnlyOnce() = runTest {
        val storage = InMemoryCharacterStorage()
        var defaults = listOf(characterFiles("流萤"))
        val repository = DefaultCharacterRepository(storage) { defaults }

        assertEquals(listOf("流萤"), repository.loadCharacters().map(Character::name))

        defaults = listOf(characterFiles("黄泉"))

        assertEquals(listOf("流萤"), repository.loadCharacters().map(Character::name))
    }

    @Test
    fun addedCharacterUsesNameAsFileIdentity() = runTest {
        val repository = DefaultCharacterRepository(InMemoryCharacterStorage()) { emptyList() }

        val character = repository.addCharacter(
            name = "  银狼  ",
            prompt = "测试 prompt",
            avatarSource = CharacterAvatarSource("picked://silver-wolf"),
        )

        assertEquals("银狼", character.id)
        assertEquals("测试 prompt", character.prompt)
        assertEquals("picked://silver-wolf", character.avatarUri)
        assertEquals(listOf("银狼"), repository.loadCharacters().map(Character::name))
    }

    @Test
    fun summaryQueryDoesNotRequireLoadingOrTruncatingPrompt() = runTest {
        val repository = DefaultCharacterRepository(InMemoryCharacterStorage()) { emptyList() }

        val longPrompt = "12345678901234567890_extra_long_prompt"
        repository.addCharacter(
            name = "流萤",
            prompt = longPrompt,
            avatarSource = null
        )

        val summary = repository.loadCharacterSummaries().single()
        assertEquals("流萤", summary.name)
        assertEquals("", summary.avatarUri)

        val full = repository.getCharacter(summary.id)
        assertNotNull(full)
        assertEquals(longPrompt, full.prompt)

        val loaded = repository.loadCharacters().single()
        assertEquals(longPrompt, loaded.prompt)
    }
}

private fun characterFiles(name: String) = DefaultCharacterAsset(
    id = name,
    name = name,
    prompt = "$name prompt",
    openingMessage = "欢迎 $name",
    avatarContent = byteArrayOf(1),
)
