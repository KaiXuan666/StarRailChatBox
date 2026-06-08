package com.kaixuan.starrailchatbox.ui.chat

import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource

sealed interface ChatAction {
    data class MessageChanged(val message: String) : ChatAction
    data object SendClicked : ChatAction
    data class QuickReplyClicked(val message: String) : ChatAction
    data object NewSessionClicked : ChatAction
    data class SessionSelected(val sessionId: String) : ChatAction
    data class SessionDeleteClicked(val sessionId: String) : ChatAction
    data class HeaderActionClicked(val action: HeaderAction) : ChatAction
    data class ComposerActionClicked(val action: ComposerAction) : ChatAction
    data object RefreshUserAvatar : ChatAction
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
