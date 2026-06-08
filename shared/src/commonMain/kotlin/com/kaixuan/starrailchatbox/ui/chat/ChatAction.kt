package com.kaixuan.starrailchatbox.ui.chat

import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource

sealed interface ChatAction {
    data class CharacterSelected(val characterId: String) : ChatAction
    data class MessageChanged(val message: String) : ChatAction
    data object SendClicked : ChatAction
    data class QuickReplyClicked(val message: String) : ChatAction
    data object NewSessionClicked : ChatAction
    data class SessionSelected(val sessionId: String) : ChatAction
    data class SessionDeleteClicked(val sessionId: String) : ChatAction
    data class HeaderActionClicked(val action: HeaderAction) : ChatAction
    data class CharacterEditOpened(val characterId: String?) : ChatAction
    data class CharacterNameChanged(val name: String) : ChatAction
    data class CharacterPromptChanged(val prompt: String) : ChatAction
    data class CharacterOpeningMessageChanged(val openingMessage: String) : ChatAction
    data class CharacterAvatarChanged(val avatarSource: CharacterAvatarSource) : ChatAction
    data class CharacterTemperatureChanged(val temperature: Double) : ChatAction
    data class CharacterTopPChanged(val topP: Double) : ChatAction
    data object CharacterSaveClicked : ChatAction
    data class CharacterDeleteClicked(val characterId: String) : ChatAction
    data class ComposerActionClicked(val action: ComposerAction) : ChatAction
    data class CharacterPromptGenClicked(val defaultPromptRequestText: String) : ChatAction
    data class CharacterPromptGenInputChanged(val text: String) : ChatAction
    data object CharacterPromptGenConfirmClicked : ChatAction
    data object CharacterPromptGenCancelClicked : ChatAction
    data class CharactersReordered(val orderedCharacters: List<Character>) : ChatAction
}

enum class HeaderAction {
    VOICE,
    CONVERSATION_MANAGEMENT,
    CHARACTER_EDIT,
}

enum class ComposerAction {
    ATTACH,
    EMOJI,
    VOICE,
}
