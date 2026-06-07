package com.kaixuan.starrailchatbox.data.character

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterRepositoryTest {
    @Test
    fun packagedDefaultsCanBeLoaded() = runTest {
        val characters = DefaultCharacterRepository(InMemoryCharacterStorage()).loadCharacters()

        assertEquals("流萤", characters.first().name)
        assertEquals(setOf("三月七", "流萤", "瑕蝶", "黄泉"), characters.map(Character::name).toSet())
        characters.forEach { character ->
            assertTrue(character.prompt.isNotBlank())
            assertEquals("今天要聊点什么呢？", character.openingMessage)
            assertTrue(character.avatarBytes.isNotEmpty())
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
            avatarBytes = byteArrayOf(1, 2, 3),
        )

        assertEquals("银狼", character.id)
        assertEquals("测试 prompt", character.prompt)
        assertContentEquals(byteArrayOf(1, 2, 3), character.avatarBytes)
        assertEquals(listOf("银狼"), repository.loadCharacters().map(Character::name))
    }
}

private fun characterFiles(name: String) = CharacterFiles(
    id = name,
    name = name,
    promptBytes = "$name prompt".encodeToByteArray(),
    openingMessage = "欢迎 $name",
    avatarBytes = byteArrayOf(1),
)
