package com.kaixuan.starrailchatbox.ui.chat

import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatSessionScreenTest {
    @Test
    fun compactPagerContainsOnlyFirstFourCharacters() {
        val characters = (1..6).map { index ->
            CharacterSummary(
                id = "character-$index",
                name = "Character $index",
                avatarUri = "",
                lastMessageAt = null,
            )
        }

        assertEquals(
            listOf("character-1", "character-2", "character-3", "character-4"),
            chatPagerCharacters(characters, compact = true).map(CharacterSummary::id),
        )
    }

    @Test
    fun nonCompactPagerContainsAllCharacters() {
        val characters = (1..6).map { index ->
            CharacterSummary(
                id = "character-$index",
                name = "Character $index",
                avatarUri = "",
                lastMessageAt = null,
            )
        }

        assertEquals(
            characters,
            chatPagerCharacters(characters, compact = false),
        )
    }
}
