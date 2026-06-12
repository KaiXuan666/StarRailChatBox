package com.kaixuan.starrailchatbox.ui.chat

import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun shortLatestMessageKeepsBottomAlignment() {
        assertEquals(
            0,
            latestMessageScrollOffset(
                messageSize = 400,
                viewportSize = 600,
            ),
        )
    }

    @Test
    fun viewportHeightLatestMessageKeepsBottomAlignment() {
        assertEquals(
            0,
            latestMessageScrollOffset(
                messageSize = 600,
                viewportSize = 600,
            ),
        )
    }

    @Test
    fun tallLatestMessageScrollsToItsStart() {
        assertEquals(
            300,
            latestMessageScrollOffset(
                messageSize = 900,
                viewportSize = 600,
            ),
        )
    }

    @Test
    fun staleLayoutItemIsNotUsedForLatestMessagePosition() {
        assertFalse(
            isLatestMessageLayoutItem(
                itemIndex = 0,
                itemKey = "previous-message",
                latestMessageId = "latest-message",
            ),
        )
    }

    @Test
    fun currentLatestLayoutItemCanBeUsedForPositioning() {
        assertTrue(
            isLatestMessageLayoutItem(
                itemIndex = 0,
                itemKey = "latest-message",
                latestMessageId = "latest-message",
            ),
        )
    }
}
