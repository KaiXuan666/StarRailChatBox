package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.data.character.Character

enum class ChatCopy {
    WELCOME,
    USER_TIRED,
    COMFORT,
    USER_THANKS,
    CARE,
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
    val messages: List<ChatMessageUiModel> = initialMessages,
    val messageDraft: String = "",
    val isSending: Boolean = false,
    val isLoadingCharacters: Boolean = true,
) {
    val selectedCharacter: Character?
        get() = characters.firstOrNull { it.id == selectedCharacterId }
            ?: characters.firstOrNull()
}

private val initialMessages = listOf(
    ChatMessageUiModel.Received(
        id = "message-1",
        timestamp = "10:21",
        content = MessageContent.Resource(ChatCopy.WELCOME),
        senderId = "流萤",
    ),
    ChatMessageUiModel.Sent(
        id = "message-2",
        timestamp = "10:22",
        content = MessageContent.Resource(ChatCopy.USER_TIRED),
        isRead = true,
    ),
    ChatMessageUiModel.Received(
        id = "message-3",
        timestamp = "10:23",
        content = MessageContent.Resource(ChatCopy.COMFORT),
        senderId = "流萤",
    ),
    ChatMessageUiModel.Sent(
        id = "message-4",
        timestamp = "10:24",
        content = MessageContent.Resource(ChatCopy.USER_THANKS),
        isRead = true,
    ),
    ChatMessageUiModel.Received(
        id = "message-5",
        timestamp = "10:25",
        content = MessageContent.Resource(ChatCopy.CARE),
        senderId = "流萤",
    ),
)
