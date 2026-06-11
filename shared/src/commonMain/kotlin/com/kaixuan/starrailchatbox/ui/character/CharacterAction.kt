package com.kaixuan.starrailchatbox.ui.character

import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterSummary

sealed interface CharacterAction {
    data class CharacterSelected(val characterId: String) : CharacterAction
    data class CharacterEditOpened(val characterId: String?) : CharacterAction
    data class CharacterNameChanged(val name: String) : CharacterAction
    data class CharacterDescriptionChanged(val description: String) : CharacterAction
    data class CharacterPromptChanged(val prompt: String) : CharacterAction
    data class CharacterOpeningMessageChanged(val openingMessage: String) : CharacterAction
    data class CharacterAvatarChanged(val avatarSource: CharacterAvatarSource) : CharacterAction
    data class CharacterVoiceSampleChanged(
        val uri: String?,
        val name: String? = null,
        val extension: String? = null,
    ) : CharacterAction
    data class CharacterTemperatureChanged(val temperature: Double) : CharacterAction
    data class CharacterTopPChanged(val topP: Double) : CharacterAction
    data object CharacterSaveClicked : CharacterAction
    data class CharacterDeleteClicked(val characterId: String) : CharacterAction
    data object CharacterDeleteBuiltinClicked : CharacterAction
    data class CharacterPromptGenClicked(val defaultPromptRequestText: String) : CharacterAction
    data class CharacterPromptGenInputChanged(val text: String) : CharacterAction
    data object CharacterPromptGenConfirmClicked : CharacterAction
    data object CharacterPromptGenCancelClicked : CharacterAction
    data object CharacterRestoreDefaultClicked : CharacterAction
    data class CharactersReordered(val orderedCharacters: List<CharacterSummary>) : CharacterAction
    
    // Export
    data object CharacterExportClicked : CharacterAction
    data class CharacterExportDirectorySelected(val directory: io.github.vinceglb.filekit.PlatformFile) : CharacterAction
    
    // Import
    data object CharacterImportClicked : CharacterAction
    data class CharacterImportFileSelected(val path: String, val name: String, val extension: String) : CharacterAction
    data object CharacterImportWarningDismissed : CharacterAction
    data object CharacterImportCancelled : CharacterAction
}
