package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource

sealed interface MessageContent {
    data class Custom(val text: String) : MessageContent
}

@Immutable
sealed interface ChatMessageUiModel {
    val id: String
    val timestamp: String
    val createdAt: Long
    val content: MessageContent

    data class Received(
        override val id: String,
        override val timestamp: String,
        override val createdAt: Long,
        override val content: MessageContent,
        val senderId: String,
    ) : ChatMessageUiModel

    data class Sent(
        override val id: String,
        override val timestamp: String,
        override val createdAt: Long,
        override val content: MessageContent,
        val isRead: Boolean,
    ) : ChatMessageUiModel
}

@Immutable
data class CharacterChatState(
    val activeSessionId: String? = null,
    val sessions: List<ConversationSummaryUiModel> = emptyList(),
    val messages: List<ChatMessageUiModel> = emptyList(),
    val messageDraft: String = "",
    val isLoadingSession: Boolean = false,
    val isSending: Boolean = false,
    val suggestions: List<String> = emptyList(),
)

@Immutable
data class ConversationSummaryUiModel(
    val id: String,
    val title: String,
    val preview: String,
    val updatedAt: String,
    val messageCount: Int,
)

@Immutable
data class ChatUiState(
    val selectedCharacterId: String? = null,
    val selectedCharacter: Character? = null,
    val characterStates: Map<String, CharacterChatState> = emptyMap(),
    val userAvatarUri: String? = null,
) {
    val activeSessionId: String?
        get() = characterStates[selectedCharacterId]?.activeSessionId

    val sessions: List<ConversationSummaryUiModel>
        get() = characterStates[selectedCharacterId]?.sessions.orEmpty()

    val messages: List<ChatMessageUiModel>
        get() = characterStates[selectedCharacterId]?.messages.orEmpty()

    val messageDraft: String
        get() = characterStates[selectedCharacterId]?.messageDraft.orEmpty()

    val isSending: Boolean
        get() = characterStates[selectedCharacterId]?.isSending ?: false

    val isLoadingSession: Boolean
        get() = characterStates[selectedCharacterId]?.isLoadingSession ?: false

    val suggestions: List<String>
        get() = characterStates[selectedCharacterId]?.suggestions.orEmpty()
}
