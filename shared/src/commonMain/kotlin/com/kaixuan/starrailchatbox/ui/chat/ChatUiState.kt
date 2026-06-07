package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.data.character.Character

enum class ChatCopy {
    EMPTY_GREETING,
}

sealed interface MessageContent {
    data class Resource(val copy: ChatCopy) : MessageContent
    data class Custom(val text: String) : MessageContent
}

@Immutable
sealed interface ChatMessageUiModel {
    val id: String
    val timestamp: String
    val content: MessageContent

    data class Received(
        override val id: String,
        override val timestamp: String,
        override val content: MessageContent,
        val senderId: String,
    ) : ChatMessageUiModel

    data class Sent(
        override val id: String,
        override val timestamp: String,
        override val content: MessageContent,
        val isRead: Boolean,
    ) : ChatMessageUiModel
}

@Immutable
data class ChatUiState(
    val characters: List<Character> = emptyList(),
    val selectedCharacterId: String? = null,
    val activeSessionId: String? = null,
    val messages: List<ChatMessageUiModel> = emptyList(),
    val messageDraft: String = "",
    val isSending: Boolean = false,
    val isLoadingCharacters: Boolean = true,
    val isLoadingSession: Boolean = false,
) {
    val selectedCharacter: Character?
        get() = characters.firstOrNull { it.id == selectedCharacterId }
            ?: characters.firstOrNull()
}
