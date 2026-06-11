package com.kaixuan.starrailchatbox.ui.character

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.importer.ImportedCharacterDraft

@Immutable
data class CharacterEditUiState(
    val characterId: String? = null,
    val name: String = "",
    val prompt: String = "",
    val openingMessage: String = "",
    val avatarUri: String = "",
    val pendingAvatarSource: CharacterAvatarSource? = null,
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
    val voiceSampleUri: String? = null,
    val isSaving: Boolean = false,
    val isPromptGenDialogOpen: Boolean = false,
    val promptGenInputText: String = "",
    val isGeneratingPrompt: Boolean = false,
    
    // Import status
    val importDraft: ImportedCharacterDraft? = null,
    val isImporting: Boolean = false,
    val importError: String? = null,
)

@Immutable
data class CharactersUiState(
    val characters: List<Character> = emptyList(),
    val selectedCharacterId: String? = null,
    val characterEdit: CharacterEditUiState = CharacterEditUiState(),
    val isLoadingCharacters: Boolean = true,
) {
    val selectedCharacter: Character?
        get() = characters.firstOrNull { it.id == selectedCharacterId }
            ?: characters.firstOrNull()
}

fun Character.toEditUiState() = CharacterEditUiState(
    characterId = id,
    name = name,
    prompt = prompt,
    openingMessage = openingMessage,
    avatarUri = avatarUri,
    temperature = temperature,
    topP = topP,
    voiceSampleUri = voiceSampleUri,
)
