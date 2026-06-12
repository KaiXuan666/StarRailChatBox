package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.app_title
import starrailchatbox.shared.generated.resources.no_characters
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import androidx.compose.foundation.ExperimentalFoundationApi
import com.kaixuan.starrailchatbox.ui.main.MainEffectMessage

/**
 * 聊天会话主屏组件，支持多角色分页切换和消息列表展示。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatSessionScreen(
    state: ChatUiState,
    charactersState: ChatCharactersUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    isActive: Boolean,
    onAction: (ChatAction) -> Unit,
    onCharacterAction: (CharacterAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    isCancelTargeted: Boolean = false,
) {
    val characters = charactersState.characters
    val pagerCharacters = remember(characters, compact) {
        chatPagerCharacters(characters, compact)
    }
    val selectedCharacter = charactersState.selectedCharacter
    val coroutineScope = rememberCoroutineScope()

    var attachmentsToShow by remember { mutableStateOf<List<MessageAttachment>?>(null) }
    var previewAttachment by remember { mutableStateOf<MessageAttachment?>(null) }

    BackHandler(enabled = state.isAttachmentPanelVisible) {
        onAction(ChatAction.ComposerActionClicked(ComposerAction.ATTACH))
    }

    BackHandler(enabled = previewAttachment != null) {
        previewAttachment = null
    }

    if (charactersState.isLoadingCharacters) {
        Box(
            modifier = modifier.fillMaxSize().padding(StarRailSpacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (characters.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(StarRailSpacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.no_characters),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    val initialPage = remember(pagerCharacters, selectedCharacter) {
        val index = pagerCharacters.indexOfFirst { it.id == selectedCharacter?.id }
        if (index != -1) index else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
    ) { pagerCharacters.size }
    
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

    // 缓存每个角色的 LazyListState
    val pageListStates = remember { mutableMapOf<String, LazyListState>() }
    val currentStates = remember(pagerCharacters) {
        pagerCharacters.associate { character ->
            character.id to pageListStates.getOrPut(character.id) {
                LazyListState()
            }
        }
    }
    val initiallyPositionedCharacterIds = remember { mutableSetOf<String>() }

    LaunchedEffect(isActive, selectedCharacter?.id, pagerCharacters) {
        if (!isActive) return@LaunchedEffect
        val targetPage = pagerCharacters.indexOfFirst { it.id == selectedCharacter?.id }
        if (targetPage != -1 && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(isActive, pagerState.currentPage, pagerCharacters, selectedCharacter?.id) {
        if (!isActive) return@LaunchedEffect
        if (selectedCharacter != null &&
            pagerCharacters.none { it.id == selectedCharacter.id }
        ) {
            return@LaunchedEffect
        }
        val targetCharacter = pagerCharacters.getOrNull(pagerState.currentPage)
        if (targetCharacter != null && targetCharacter.id != selectedCharacter?.id) {
            onCharacterAction(CharacterAction.CharacterSelected(targetCharacter.id))
        }
    }

    LaunchedEffect(isActive, compact, pagerCharacters, selectedCharacter?.id) {
        if (isActive &&
            compact &&
            selectedCharacter != null &&
            pagerCharacters.none { it.id == selectedCharacter.id }
        ) {
            onAction(ChatAction.RestoreMainCharacter)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
        // 固定顶部的标题栏
        Text(
            text = stringResource(Res.string.app_title),
            color = MaterialTheme.colorScheme.onBackground,
            style = if (compact) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.headlineLarge
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = if (compact) StarRailSpacing.sm else StarRailSpacing.md)
                .padding(top = StarRailSpacing.lg)
        )

        // 固定顶部的角色选择器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) StarRailSpacing.sm else StarRailSpacing.md)
                .padding(vertical = StarRailSpacing.sm)
        ) {
            CharacterSelector(
                characters = pagerCharacters,
                selectedCharacterId = selectedCharacter?.id,
                compact = compact,
                onCharacterSelected = { characterId ->
                    val index = pagerCharacters.indexOfFirst { it.id == characterId }
                    if (index != -1) {
                        onCharacterAction(CharacterAction.CharacterSelected(characterId))
                        coroutineScope.launch {
                            if (pagerState.currentPage != index) {
                                pagerState.scrollToPage(index)
                            }
                        }
                    }
                }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val pageCharacter = pagerCharacters[page]
            val pageState = state.characterStates[pageCharacter.id] ?: CharacterChatState()
            val charactersById = remember(characters) {
                characters.associateBy(CharacterSummary::id)
            }

            val pageListState = currentStates.getValue(pageCharacter.id)
            val pageMessages = pageState.messagePagingData.flow.collectAsLazyPagingItems()
            val latestMessage = (
                pageMessages.itemSnapshotList.items.firstOrNull()
                    as? ChatTimelineItem.Message
                )?.message
            val latestMessageId = latestMessage?.id

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
                withFrameNanos {}
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

            LaunchedEffect(pageCharacter.id, latestMessageId, pageMessages.itemCount) {
                if (
                    latestMessage != null &&
                    pageState.messagePagingData.anchor == ChatHistoryAnchor.LATEST
                ) {
                    if (pageCharacter.id !in initiallyPositionedCharacterIds) {
                        withFrameNanos {}
                        pageListState.scrollToItem(0)
                        initiallyPositionedCharacterIds += pageCharacter.id
                    } else {
                        // 靠近最新消息时，发送或收到消息后自动调整阅读位置。
                        if (pageListState.firstVisibleItemIndex <= 1) {
                            pageListState.scrollToNewLatestMessage(latestMessage)
                        }
                    }
                }
            }

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
                        bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.lg,
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
                    onAction = onAction,
                    headerContent = {
                        ChatHeader(
                            selectedCharacter = pageCharacter,
                            compact = compact,
                            onAction = onAction,
                            onCharacterAction = onCharacterAction,
                            onMainAction = onMainAction,
                            modifier = Modifier.padding(bottom = StarRailSpacing.md)
                        )
                    }
            )
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

internal suspend fun LazyListState.scrollToNewLatestMessage(
    latestMessage: ChatMessageUiModel,
) {
    if (latestMessage !is ChatMessageUiModel.Received) {
        animateScrollToItem(0)
        return
    }

    val latestMessageId = latestMessage.id
    latestMessageScrollOffsetOrNull(latestMessageId)?.let { scrollOffset ->
        animateScrollToItem(0, scrollOffset)
        correctLatestMessageScrollPosition(latestMessageId)
        return
    }

    val previousIndex = firstVisibleItemIndex
    val previousScrollOffset = firstVisibleItemScrollOffset
    scrollToItem(0)

    val scrollOffset = snapshotFlow {
        latestMessageScrollOffsetOrNull(latestMessageId)
    }.first { it != null } ?: 0
    if (scrollOffset > 0) {
        scrollToItem(0, scrollOffset)
    } else {
        scrollToItem(
            index = previousIndex.coerceAtMost(layoutInfo.totalItemsCount - 1),
            scrollOffset = previousScrollOffset,
        )
        animateScrollToItem(0)
    }
    correctLatestMessageScrollPosition(latestMessageId)
}

private suspend fun LazyListState.correctLatestMessageScrollPosition(
    latestMessageId: String,
) {
    withFrameNanos {}
    val correctedScrollOffset = latestMessageScrollOffsetOrNull(latestMessageId)
        ?: return
    if (
        firstVisibleItemIndex == 0 &&
        firstVisibleItemScrollOffset != correctedScrollOffset
    ) {
        scrollToItem(0, correctedScrollOffset)
    }
}

private fun LazyListState.latestMessageScrollOffsetOrNull(
    latestMessageId: String,
): Int? {
    val latestItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
        isLatestMessageLayoutItem(
            itemIndex = item.index,
            itemKey = item.key,
            latestMessageId = latestMessageId,
        )
    } ?: return null
    val contentViewportSize = (
        layoutInfo.viewportSize.height -
            layoutInfo.beforeContentPadding -
            layoutInfo.afterContentPadding
        ).coerceAtLeast(0)
    if (contentViewportSize == 0) return null

    return latestMessageScrollOffset(
        messageSize = latestItem.size,
        viewportSize = contentViewportSize,
    )
}

internal fun isLatestMessageLayoutItem(
    itemIndex: Int,
    itemKey: Any,
    latestMessageId: String,
): Boolean = itemIndex == 0 && itemKey == latestMessageId

internal fun latestMessageScrollOffset(
    messageSize: Int,
    viewportSize: Int,
): Int = (messageSize - viewportSize).coerceAtLeast(0)

internal fun chatPagerCharacters(
    characters: List<CharacterSummary>,
    compact: Boolean,
): List<CharacterSummary> = if (compact) characters.take(4) else characters
