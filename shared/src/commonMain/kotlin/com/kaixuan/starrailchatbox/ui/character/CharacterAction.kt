package com.kaixuan.starrailchatbox.ui.character

import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource

sealed interface CharacterAction {
    data class CharacterSelected(val characterId: String) : CharacterAction
    data class CharacterEditOpened(val characterId: String?) : CharacterAction
    data class CharacterNameChanged(val name: String) : CharacterAction
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
    data class CharactersReordered(val orderedCharacters: List<Character>) : CharacterAction
}
