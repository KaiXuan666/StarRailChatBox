package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

sealed interface MessageContent {
    data class Custom(val text: String) : MessageContent
}

enum class MessageStatus {
    SENDING, SENT, FAILED
}

@Immutable
sealed interface ChatMessageUiModel {
    val id: String
    val timestamp: String
    val createdAt: Long
    val content: MessageContent
    val attachments: List<MessageAttachment>

    data class Received(
        override val id: String,
        override val timestamp: String,
        override val createdAt: Long,
        override val content: MessageContent,
        val senderId: String,
        override val attachments: List<MessageAttachment> = emptyList(),
    ) : ChatMessageUiModel

    data class Sent(
        override val id: String,
        override val timestamp: String,
        override val createdAt: Long,
        override val content: MessageContent,
        val isRead: Boolean,
        val status: MessageStatus = MessageStatus.SENT,
        override val attachments: List<MessageAttachment> = emptyList(),
    ) : ChatMessageUiModel
}

@Immutable
sealed interface ChatTimelineItem {
    val key: String

    data class Message(
        val message: ChatMessageUiModel,
    ) : ChatTimelineItem {
        override val key: String = message.id
    }

    data class DateDivider(
        override val key: String,
        val text: String,
    ) : ChatTimelineItem
}

@Immutable
enum class ChatHistoryAnchor {
    LATEST,
    OLDEST,
}

@Immutable
data class ChatMessagePagingData(
    val sessionId: String?,
    val flow: Flow<PagingData<ChatTimelineItem>>,
    val anchor: ChatHistoryAnchor = ChatHistoryAnchor.LATEST,
)

val EmptyChatMessagePagingData = ChatMessagePagingData(
    sessionId = null,
    flow = flowOf(PagingData.empty()),
)

fun staticChatMessagePagingData(
    messages: List<ChatMessageUiModel>,
    sessionId: String? = null,
) = ChatMessagePagingData(
    sessionId = sessionId,
    flow = flowOf(
        PagingData.from(messages.map(ChatTimelineItem::Message)),
    ),
)

@Immutable
sealed interface SelectedAttachment {
    val uri: String
    val name: String
    val extension: String

    data class File(
        override val uri: String,
        override val name: String,
        override val extension: String
    ) : SelectedAttachment

    data class Image(
        override val uri: String,
        override val name: String,
        override val extension: String
    ) : SelectedAttachment

    data class Voice(
        override val uri: String,
        override val name: String,
        val durationMs: Long,
        override val extension: String = "mp3"
    ) : SelectedAttachment
}

@Immutable
data class CharacterChatState(
    val activeSessionId: String? = null,
    val sessions: List<ConversationSummaryUiModel> = emptyList(),
    val messagePagingData: ChatMessagePagingData = EmptyChatMessagePagingData,
    val messageDraft: String = "",
    val isLoadingSession: Boolean = false,
    val isSending: Boolean = false,
    val scrollToLatestRequestId: Long = 0,
    val suggestions: List<String> = emptyList(),
    val isAttachmentPanelVisible: Boolean = false,
    val selectedAttachments: List<SelectedAttachment> = emptyList(),
    val isVoiceMode: Boolean = false,
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

    val messagePagingData: ChatMessagePagingData
        get() = characterStates[selectedCharacterId]?.messagePagingData
            ?: EmptyChatMessagePagingData

    val messageDraft: String
        get() = characterStates[selectedCharacterId]?.messageDraft.orEmpty()

    val isSending: Boolean
        get() = characterStates[selectedCharacterId]?.isSending ?: false

    val isLoadingSession: Boolean
        get() = characterStates[selectedCharacterId]?.isLoadingSession ?: false

    val suggestions: List<String>
        get() = characterStates[selectedCharacterId]?.suggestions.orEmpty()

    val isAttachmentPanelVisible: Boolean
        get() = characterStates[selectedCharacterId]?.isAttachmentPanelVisible ?: false

    val selectedAttachments: List<SelectedAttachment>
        get() = characterStates[selectedCharacterId]?.selectedAttachments.orEmpty()
}
