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
data class CharacterEditUiState(
    val characterId: String? = null,
    val name: String = "",
    val prompt: String = "",
    val openingMessage: String = "",
    val avatarBytes: ByteArray = byteArrayOf(),
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
    val isSaving: Boolean = false,
    val isPromptGenDialogOpen: Boolean = false,
    val promptGenInputText: String = "",
    val isGeneratingPrompt: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CharacterEditUiState) return false
        return characterId == other.characterId &&
            name == other.name &&
            prompt == other.prompt &&
            openingMessage == other.openingMessage &&
            avatarBytes.contentEquals(other.avatarBytes) &&
            temperature == other.temperature &&
            topP == other.topP &&
            isSaving == other.isSaving &&
            isPromptGenDialogOpen == other.isPromptGenDialogOpen &&
            promptGenInputText == other.promptGenInputText &&
            isGeneratingPrompt == other.isGeneratingPrompt
    }

    override fun hashCode(): Int {
        var result = characterId?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + prompt.hashCode()
        result = 31 * result + openingMessage.hashCode()
        result = 31 * result + avatarBytes.contentHashCode()
        result = 31 * result + temperature.hashCode()
        result = 31 * result + topP.hashCode()
        result = 31 * result + isSaving.hashCode()
        result = 31 * result + isPromptGenDialogOpen.hashCode()
        result = 31 * result + promptGenInputText.hashCode()
        result = 31 * result + isGeneratingPrompt.hashCode()
        return result
    }
}

@Immutable
data class ChatUiState(
    val characters: List<Character> = emptyList(),
    val selectedCharacterId: String? = null,
    val characterStates: Map<String, CharacterChatState> = emptyMap(),
    val characterEdit: CharacterEditUiState = CharacterEditUiState(),
    val isLoadingCharacters: Boolean = true,
) {
    val selectedCharacter: Character?
        get() = characters.firstOrNull { it.id == selectedCharacterId }
            ?: characters.firstOrNull()

    val activeSessionId: String?
        get() = characterStates[selectedCharacter?.id]?.activeSessionId

    val sessions: List<ConversationSummaryUiModel>
        get() = characterStates[selectedCharacter?.id]?.sessions.orEmpty()

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
