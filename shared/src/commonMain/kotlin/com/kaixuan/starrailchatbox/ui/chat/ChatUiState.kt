package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.runtime.Immutable

enum class CharacterId {
    LIU_YING,
    TIAN_SHU,
    LI_GUANG,
    XI,
}

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
        val sender: CharacterId,
    ) : ChatMessageUiModel

    data class Sent(
        override val id: String,
        override val timestamp: String,
        override val content: MessageContent,
        val isRead: Boolean,
    ) : ChatMessageUiModel
}

enum class NavigationDestination {
    CHAT,
    CHARACTERS,
    DISCOVER,
    PROFILE,
}

@Immutable
data class ChatUiState(
    val selectedCharacter: CharacterId = CharacterId.LIU_YING,
    val messages: List<ChatMessageUiModel> = initialMessages,
    val messageDraft: String = "",
    val selectedDestination: NavigationDestination = NavigationDestination.CHAT,
    val darkThemeOverride: Boolean? = null,
    val isSending: Boolean = false,
)

private val initialMessages = listOf(
    ChatMessageUiModel.Received(
        id = "message-1",
        timestamp = "10:21",
        content = MessageContent.Resource(ChatCopy.WELCOME),
        sender = CharacterId.LIU_YING,
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
        sender = CharacterId.LIU_YING,
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
        sender = CharacterId.LIU_YING,
    ),
)
