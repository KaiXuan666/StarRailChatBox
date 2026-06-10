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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.platform.openUri
import com.kaixuan.starrailchatbox.platform.rememberAudioPlayer
import com.kaixuan.starrailchatbox.ui.character.CharacterAction
import com.kaixuan.starrailchatbox.ui.character.CharactersUiState
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.navigation.Route
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.app_title
import starrailchatbox.shared.generated.resources.no_characters
import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * 聊天会话主屏组件 (原 ChatContent 模块)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatSessionScreen(
    state: ChatUiState,
    charactersState: CharactersUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onCharacterAction: (CharacterAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    isCancelTargeted: Boolean = false,
) {
    val characters = charactersState.characters
    val selectedCharacter = charactersState.selectedCharacter
    val coroutineScope = rememberCoroutineScope()

    var attachmentsToShow by remember { mutableStateOf<List<MessageAttachment>?>(null) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = state.isAttachmentPanelVisible) {
        onAction(ChatAction.ComposerActionClicked(ComposerAction.ATTACH))
    }

    BackHandler(enabled = previewImageUri != null) {
        previewImageUri = null
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

    val initialPage = remember(characters, selectedCharacter) {
        val index = characters.indexOfFirst { it.id == selectedCharacter?.id }
        if (index != -1) index else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
    ) { characters.size }
    
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
    val currentStates = remember(characters) {
        characters.associate { character ->
            character.id to pageListStates.getOrPut(character.id) {
                LazyListState()
            }
        }
    }
    val initiallyPositionedCharacterIds = remember { mutableSetOf<String>() }

    LaunchedEffect(selectedCharacter?.id) {
        val targetPage = characters.indexOfFirst { it.id == selectedCharacter?.id }
        if (targetPage != -1 && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val targetCharacter = characters.getOrNull(pagerState.currentPage)
        if (targetCharacter != null && targetCharacter.id != selectedCharacter?.id) {
            onCharacterAction(CharacterAction.CharacterSelected(targetCharacter.id))
        }
    }

    LaunchedEffect(characters, selectedCharacter) {
        if (selectedCharacter != null) {
            val isTopFour = characters.sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
                .take(4)
                .any { it.id == selectedCharacter.id }
            if (!isTopFour) {
                onAction(ChatAction.RestoreMainCharacter)
            }
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
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = if (compact) StarRailSpacing.sm else StarRailSpacing.md)
                .padding(top = StarRailSpacing.md)
        )

        // 固定顶部的角色选择器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) StarRailSpacing.sm else StarRailSpacing.md)
                .padding(vertical = StarRailSpacing.sm)
        ) {
            CharacterSelector(
                characters = characters,
                selectedCharacterId = selectedCharacter?.id,
                compact = compact,
                onCharacterSelected = { characterId ->
                    val index = characters.indexOfFirst { it.id == characterId }
                    if (index != -1) {
                        coroutineScope.launch {
                            if (pagerState.currentPage != index) {
                                pagerState.scrollToPage(index)
                            }
                            withFrameNanos {}
                            val targetListState = currentStates[characterId]
                            if (targetListState != null) {
                                targetListState.scrollToItem(0)
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
            val pageCharacter = characters[page]
            val pageState = state.characterStates[pageCharacter.id] ?: CharacterChatState()
            val charactersById = remember(characters) {
                characters.associateBy(Character::id)
            }

            if (pageState.isLoadingSession) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                    ) {
                    CircularProgressIndicator()
                }
            } else {
                val pageListState = currentStates.getValue(pageCharacter.id)
                val pageMessages = pageState.messages
                val latestMessageId = pageMessages.lastOrNull()?.id

                LaunchedEffect(pageCharacter.id, latestMessageId) {
                    if (latestMessageId != null) {
                        if (pageCharacter.id !in initiallyPositionedCharacterIds) {
                            withFrameNanos {}
                            pageListState.scrollToItem(0)
                            initiallyPositionedCharacterIds += pageCharacter.id
                        } else {
                            // 已经处于最底部时，发送或收到消息，自动滚回底部
                            if (pageListState.firstVisibleItemIndex <= 1) {
                                pageListState.animateScrollToItem(0)
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
                            previewImageUri = attachment.uri
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

        }

        if (isRecording) {
            RecordingOverlay(
                isCancelTargeted = isCancelTargeted,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (previewImageUri != null) {
            FullScreenImagePreview(
                uri = previewImageUri!!,
                onDismiss = { previewImageUri = null }
            )
        }
    }
}
