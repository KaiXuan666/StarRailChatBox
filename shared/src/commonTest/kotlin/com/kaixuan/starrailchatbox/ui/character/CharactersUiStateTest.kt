package com.kaixuan.starrailchatbox.ui.character

import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class CharactersUiStateTest {
    @Test
    fun characterTabStateSelectsOnlyFromSummaries() {
        val summary = CharacterSummary(
            id = "character-id",
            name = "Character",
            avatarUri = "avatar",
        )

        val state = CharactersUiState(
            characters = listOf(summary),
            selectedCharacterId = summary.id,
            isLoadingCharacters = false,
        )

        assertEquals(summary, state.selectedCharacter)
    }
}
