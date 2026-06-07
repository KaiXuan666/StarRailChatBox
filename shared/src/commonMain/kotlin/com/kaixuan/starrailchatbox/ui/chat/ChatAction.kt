package com.kaixuan.starrailchatbox.ui.chat

sealed interface ChatAction {
    data class CharacterSelected(val characterId: String) : ChatAction
    data class MessageChanged(val message: String) : ChatAction
    data object SendClicked : ChatAction
    data class QuickReplyClicked(val message: String) : ChatAction
    data object NewSessionClicked : ChatAction
    data class SessionSelected(val sessionId: String) : ChatAction
    data class SessionDeleteClicked(val sessionId: String) : ChatAction
    data class HeaderActionClicked(val action: HeaderAction) : ChatAction
    data object CharacterEditOpened : ChatAction
    data class CharacterNameChanged(val name: String) : ChatAction
    data class CharacterPromptChanged(val prompt: String) : ChatAction
    data class CharacterOpeningMessageChanged(val openingMessage: String) : ChatAction
    data class CharacterAvatarChanged(val avatarBytes: ByteArray) : ChatAction
    data class CharacterTemperatureChanged(val temperature: Double) : ChatAction
    data class CharacterTopPChanged(val topP: Double) : ChatAction
    data object CharacterSaveClicked : ChatAction
    data class ComposerActionClicked(val action: ComposerAction) : ChatAction
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
