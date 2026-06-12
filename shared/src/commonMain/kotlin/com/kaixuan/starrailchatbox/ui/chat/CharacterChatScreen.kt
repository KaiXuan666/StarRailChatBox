package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.platform.openUri
import com.kaixuan.starrailchatbox.platform.rememberAudioPlayer
import com.kaixuan.starrailchatbox.ui.character.CharacterAction
import com.kaixuan.starrailchatbox.ui.character.ChatCharactersUiState
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.navigation.Route
import androidx.compose.ui.platform.LocalUriHandler

import com.kaixuan.starrailchatbox.platform.KmpFileManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.paging.PagingData
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.tooling.preview.Preview
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.ui.main.MainEffectMessage

/**
 * 二级对话界面，面向前四个角色以外的角色。
 */
@Composable
fun CharacterChatScreen(
    characterId: String,
    state: ChatUiState,
    charactersState: ChatCharactersUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onCharacterAction: (CharacterAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val character = remember(charactersState.characters, characterId) {
        charactersState.characters.firstOrNull { it.id == characterId }
    } ?: return

    val pageState = state.characterStates[characterId] ?: CharacterChatState()

    var isRecording by remember { mutableStateOf(false) }
    var isCancelTargeted by remember { mutableStateOf(false) }
    var attachmentsToShow by remember { mutableStateOf<List<MessageAttachment>?>(null) }
    var previewAttachment by remember { mutableStateOf<MessageAttachment?>(null) }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = pageState.isAttachmentPanelVisible) {
        onAction(ChatAction.ComposerActionClicked(ComposerAction.ATTACH))
    }

    BackHandler(enabled = previewAttachment != null) {
        previewAttachment = null
    }

    BackHandler {
        onMainAction(MainAction.PopBackStack)
    }
    val pageListState = rememberLazyListState()
    val pageMessages = pageState.messagePagingData.flow.collectAsLazyPagingItems()
    val latestMessage = (
        pageMessages.itemSnapshotList.items.firstOrNull() as? ChatTimelineItem.Message
        )?.message
    val latestMessageId = latestMessage?.id
    val charactersById = remember(charactersState.characters) {
        charactersState.characters.associateBy(CharacterSummary::id)
    }

    val audioPlayer = rememberAudioPlayer()
    var playingAudioUri by remember { mutableStateOf<String?>(null) }
    DisposableEffect(audioPlayer) {
        onDispose {
            audioPlayer.release()
        }
    }

    if (attachmentsToShow != null) {
        AttachmentsDialog(
            attachments = attachmentsToShow!!,
            onDismissRequest = { attachmentsToShow = null },
            onOpenAttachment = { attachment ->
                openUri(attachment.uri, attachment.mimeType)
            }
        )
    }

    LaunchedEffect(pageState.messagePagingData) {
        if (pageState.messagePagingData.sessionId == null) {
            pageListState.scrollToItem(0)
            return@LaunchedEffect
        }
        snapshotFlow {
            pageMessages.loadState.refresh to pageMessages.itemCount
        }.first { (loadState, itemCount) ->
            loadState is LoadState.NotLoading && itemCount > 0
        }
        when (pageState.messagePagingData.anchor) {
            ChatHistoryAnchor.LATEST -> pageListState.scrollToItem(0)
            ChatHistoryAnchor.OLDEST ->
                pageListState.scrollToItem(pageMessages.itemCount - 1)
        }
    }

    LaunchedEffect(pageState.scrollToLatestRequestId) {
        if (pageState.scrollToLatestRequestId == 0L) return@LaunchedEffect
        val latestMessageCached =
            pageMessages.loadState.prepend is LoadState.NotLoading &&
                (pageMessages.loadState.prepend as LoadState.NotLoading)
                    .endOfPaginationReached
        if (latestMessageCached) {
            pageListState.scrollToItem(0)
        } else {
            onAction(ChatAction.ScrollToLatestMessage)
        }
    }

    LaunchedEffect(latestMessageId) {
        if (
            latestMessage != null &&
            pageState.messagePagingData.anchor == ChatHistoryAnchor.LATEST
        ) {
            // 靠近最新消息时，发送或收到消息后自动调整阅读位置。
            if (pageListState.firstVisibleItemIndex <= 1) {
                pageListState.scrollToNewLatestMessage(latestMessage)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.97f),
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        ChatSessionBottomBar(
                            state = state,
                            compact = compact,
                            onAction = onAction,
                            onRecordingStateChanged = { recording, cancelTargeted ->
                                isRecording = recording
                                isCancelTargeted = cancelTargeted
                            }
                        )
                    }
                }
            }
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = scaffoldPadding.calculateTopPadding())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                            end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                            bottom = StarRailSpacing.md,
                        )
                ) {
                    CharacterChatHeader(
                        selectedCharacter = character,
                        compact = compact,
                        onAction = onAction,
                        onCharacterAction = onCharacterAction,
                        onMainAction = onMainAction,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (pageState.isLoadingSession) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        ChatMessageList(
                            messages = pageMessages,
                            listState = pageListState,
                            charactersById = charactersById,
                            userAvatarUri = state.userAvatarUri,
                            compact = compact,
                            isSending = pageState.isSending,
                            isTransientSession = pageState.messagePagingData.sessionId == null,
                            playingAudioUri = playingAudioUri,
                            contentPadding = PaddingValues(
                                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                                bottom = scaffoldPadding.calculateBottomPadding() + StarRailSpacing.lg,
                                top = StarRailSpacing.md,
                            ),
                            onViewAttachments = { attachments ->
                                attachmentsToShow = attachments
                            },
                            onOpenAttachment = { attachment ->
                                if (attachment.mimeType.startsWith("audio/")) {
                                    if (playingAudioUri == attachment.uri) {
                                        audioPlayer.stop()
                                        playingAudioUri = null
                                    } else {
                                        playingAudioUri = attachment.uri
                                        audioPlayer.play(attachment.uri) {
                                            if (playingAudioUri == attachment.uri) {
                                                playingAudioUri = null
                                            }
                                        }
                                    }
                                } else if (attachment.mimeType.startsWith("image/")) {
                                    previewAttachment = attachment
                                } else {
                                    openUri(attachment.uri, attachment.mimeType)
                                }
                            },
                            onAvatarClick = {
                                onMainAction(MainAction.NavigateTo(Route.ConversationManagement))
                            },
                            onAction = onAction
                        )
                    }
                }
            }
        }

        if (isRecording) {
            RecordingOverlay(
                isCancelTargeted = isCancelTargeted,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (previewAttachment != null) {
            FullScreenImagePreview(
                uri = previewAttachment!!.uri,
                onDismiss = { previewAttachment = null },
                onDownload = {
                    val attachment = previewAttachment!!
                    previewAttachment = null
                    coroutineScope.launch {
                        try {
                            val bytes = KmpFileManager.Default.readSourceBytes(attachment.uri)
                            KmpFileManager.Default.saveImageToGallery(bytes, attachment.name)
                            onMainAction(MainAction.ShowMessage(MainEffectMessage.IMAGE_SAVED))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onMainAction(MainAction.ShowMessage(MainEffectMessage.IMAGE_SAVE_FAILED))
                        }
                    }
                }
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterChatScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        CharacterChatScreen(
            characterId = "builtin:流萤",
            state = previewChatUiState,
            charactersState = previewChatCharactersUiState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onAction = {},
            onCharacterAction = {},
            onMainAction = {}
        )
    }
}

private val previewCharacterSummary = CharacterSummary(
    id = "builtin:流萤",
    name = "流萤",
    avatarUri = "",
    lastMessageAt = 0L
)

private val previewChatCharactersUiState = ChatCharactersUiState(
    characters = listOf(previewCharacterSummary),
    selectedCharacterId = "builtin:流萤",
    isLoadingCharacters = false
)

private val previewChatUiState = ChatUiState(
    selectedCharacterId = "builtin:流萤",
    characterStates = mapOf(
        "builtin:流萤" to CharacterChatState(
            messagePagingData = ChatMessagePagingData(
                sessionId = "preview",
                flow = flowOf(
                    PagingData.from(
                        listOf(
                            ChatTimelineItem.Message(
                                ChatMessageUiModel.Received(
                                    id = "1",
                                    content = MessageContent.Custom("你好呀，开拓者！"),
                                    timestamp = "10:00",
                                    senderId = "builtin:流萤",
                                    createdAt = 0L,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    )
)
