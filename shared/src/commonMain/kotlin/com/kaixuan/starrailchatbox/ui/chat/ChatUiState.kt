package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.data.character.Character

sealed interface MessageContent {
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
data class CharacterChatState(
    val activeSessionId: String? = null,
    val messages: List<ChatMessageUiModel> = emptyList(),
    val messageDraft: String = "",
    val isLoadingSession: Boolean = false,
    val isSending: Boolean = false,
    val suggestions: List<String> = emptyList(),
)

@Immutable
data class ChatUiState(
    val characters: List<Character> = emptyList(),
    val selectedCharacterId: String? = null,
    val characterStates: Map<String, CharacterChatState> = emptyMap(),
    val isLoadingCharacters: Boolean = true,
) {
    val selectedCharacter: Character?
        get() = characters.firstOrNull { it.id == selectedCharacterId }
            ?: characters.firstOrNull()

    val activeSessionId: String?
        get() = characterStates[selectedCharacter?.id]?.activeSessionId

    val messages: List<ChatMessageUiModel>
        get() = characterStates[selectedCharacter?.id]?.messages.orEmpty()

    val messageDraft: String
        get() = characterStates[selectedCharacter?.id]?.messageDraft.orEmpty()

    val isSending: Boolean
        get() = characterStates[selectedCharacter?.id]?.isSending ?: false

    val isLoadingSession: Boolean
        get() = characterStates[selectedCharacter?.id]?.isLoadingSession ?: false

    val suggestions: List<String>
        get() = characterStates[selectedCharacter?.id]?.suggestions.orEmpty()
}
