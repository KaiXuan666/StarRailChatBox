package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.ui.character.ChatCharactersUiState

@Preview(widthDp = 360, heightDp = 800)
@Composable
fun ChatSessionScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        ChatSessionScreen(
            state = chatPreviewState,
            charactersState = charactersPreviewState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            isActive = true,
            onAction = {},
            onCharacterAction = {},
            onMainAction = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
fun ChatSessionScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        ChatSessionScreen(
            state = chatPreviewState,
            charactersState = charactersPreviewState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            isActive = true,
            onAction = {},
            onCharacterAction = {},
            onMainAction = {}
        )
    }
}

private val previewFullCharacters = listOf(
    previewCharacter("builtin:流萤", "流萤"),
    previewCharacter("builtin:三月七", "三月七"),
    previewCharacter("builtin:黄泉", "黄泉"),
    previewCharacter("builtin:瑕蝶", "瑕蝶"),
)
private val previewCharacters = previewFullCharacters.map {
    CharacterSummary(id = it.id, name = it.name, avatarUri = it.avatarUri)
}

val charactersPreviewState = ChatCharactersUiState(
    characters = previewCharacters,
    selectedCharacterId = "builtin:流萤",
    isLoadingCharacters = false,
)

val chatPreviewState = ChatUiState(
    selectedCharacterId = "builtin:流萤",
    selectedCharacter = previewFullCharacters.first(),
    characterStates = mapOf(
        "builtin:流萤" to CharacterChatState(
            activeSessionId = "preview-session",
            messages = listOf(
                ChatMessageUiModel.Received(
                    id = "preview-opening",
                    timestamp = "10:21",
                    createdAt = 1715832060000L,
                    content = MessageContent.Custom("今天要聊点什么呢？"),
                    senderId = "builtin:流萤",
                ),
                ChatMessageUiModel.Sent(
                    id = "preview-user-1",
                    timestamp = "10:22",
                    createdAt = 1715832120000L,
                    content = MessageContent.Custom("今天有点累，想和你聊聊天。"),
                    isRead = true,
                ),
                ChatMessageUiModel.Received(
                    id = "preview-assistant-1",
                    timestamp = "10:23",
                    createdAt = 1715832180000L,
                    content = MessageContent.Custom(
                        "好呀，我会认真听着。发生了什么让你觉得累呢？",
                    ),
                    senderId = "builtin:流萤",
                ),
                ChatMessageUiModel.Sent(
                    id = "preview-user-2",
                    timestamp = "10:24",
                    createdAt = 1715832240000L,
                    content = MessageContent.Custom("忙了一整天，不过现在感觉好多了。"),
                    isRead = true,
                ),
                ChatMessageUiModel.Received(
                    id = "preview-assistant-2",
                    timestamp = "10:25",
                    createdAt = 1715832300000L,
                    content = MessageContent.Custom(
                        "那就先放松一下吧。你已经很努力了，剩下的时间留给自己。",
                    ),
                    senderId = "builtin:流萤",
                ),
                ChatMessageUiModel.Sent(
                    id = "preview-user-voice",
                    timestamp = "10:26",
                    createdAt = 1715832360000L,
                    content = MessageContent.Custom(""),
                    isRead = true,
                    attachments = listOf(
                        MessageAttachment(
                            id = "voice-1",
                            messageId = "preview-user-voice",
                            name = "voice.m4a",
                            size = 0,
                            mimeType = "audio/m4a",
                            uri = "",
                            createdAt = 1715832360000L,
                            durationMs = 2000L
                        )
                    )
                ),
            ),
            messageDraft = "想听你讲一个星空下的故事",
            isLoadingSession = false,
            suggestions = listOf("讲讲星核猎手", "你喜欢橡木蛋糕卷吗", "关于这片星空...", "想听听你的过去"),
            )
    ),
)

fun previewCharacter(
    id: String,
    name: String,
) = Character(
    id = id,
    name = name,
    prompt = "Preview prompt for $name",
    openingMessage = "今天要聊点什么呢？",
    avatarUri = "",
)

@Preview
@Composable
fun ChatSessionBottomBarLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatSessionBottomBar(
                state = chatPreviewState,
                compact = true,
                onAction = {},
                onRecordingStateChanged = { _: Boolean, _: Boolean -> }
            )
        }
    }
}

@Preview
@Composable
fun ChatSessionBottomBarDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatSessionBottomBar(
                state = chatPreviewState,
                compact = true,
                onAction = {},
                onRecordingStateChanged = { _: Boolean, _: Boolean -> }
            )
        }
    }
}
