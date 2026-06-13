package com.kaixuan.starrailchatbox.ui.character

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.character.importer.ImportedCharacterDraft

@Immutable
data class CharacterEditUiState(
    val characterId: String? = null,
    val name: String = "",
    val author: String = "",
    val description: String = "",
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
    val isAvatarGenDialogOpen: Boolean = false,
    val avatarGenInputText: String = "",
    val isGeneratingAvatar: Boolean = false,
    
    // Export status
    val isExporting: Boolean = false,
    val exportError: String? = null,
    
    // Import status
    val importDraft: ImportedCharacterDraft? = null,
    val isImporting: Boolean = false,
    val importError: String? = null,
)

@Immutable
data class CharactersUiState(
    val characters: List<CharacterSummary> = emptyList(),
    val selectedCharacterId: String? = null,
    val isLoadingCharacters: Boolean = true,
    val exportDialogCharacterId: String? = null,
    val pendingLocalExportCharacterId: String? = null,
    val sharingCharacterId: String? = null,
) {
    val selectedCharacter: CharacterSummary?
        get() = characters.firstOrNull { it.id == selectedCharacterId }
            ?: characters.firstOrNull()
}

@Immutable
data class ChatCharactersUiState(
    val characters: List<CharacterSummary> = emptyList(),
    val selectedCharacterId: String? = null,
    @Deprecated("Character editing state is owned by CharacterEditViewModel.")
    val characterEdit: CharacterEditUiState = CharacterEditUiState(),
    val isLoadingCharacters: Boolean = true,
) {
    val selectedCharacter: CharacterSummary?
        get() = characters.firstOrNull { it.id == selectedCharacterId }
            ?: characters.firstOrNull()
}

fun Character.toEditUiState() = CharacterEditUiState(
    characterId = id,
    name = name,
    author = author,
    description = description,
    prompt = prompt,
    openingMessage = openingMessage,
    avatarUri = avatarUri,
    temperature = temperature,
    topP = topP,
    voiceSampleUri = voiceSampleUri,
)
