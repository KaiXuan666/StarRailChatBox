package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import com.kaixuan.starrailchatbox.platform.rememberCameraLauncher
import com.kaixuan.starrailchatbox.platform.rememberFilePicker
import com.kaixuan.starrailchatbox.platform.rememberImagePicker
import com.kaixuan.starrailchatbox.platform.rememberAudioRecorder
import starrailchatbox.shared.generated.resources.action_character_edit
import starrailchatbox.shared.generated.resources.action_conversation_management
import starrailchatbox.shared.generated.resources.add_attachment
import starrailchatbox.shared.generated.resources.attach_camera
import starrailchatbox.shared.generated.resources.attach_file
import starrailchatbox.shared.generated.resources.attach_gallery
import starrailchatbox.shared.generated.resources.app_title
import starrailchatbox.shared.generated.resources.character_selected_description
import starrailchatbox.shared.generated.resources.character_selection_description
import starrailchatbox.shared.generated.resources.message_care
import starrailchatbox.shared.generated.resources.message_comfort
import starrailchatbox.shared.generated.resources.message_placeholder
import starrailchatbox.shared.generated.resources.message_user_thanks
import starrailchatbox.shared.generated.resources.message_user_tired
import starrailchatbox.shared.generated.resources.message_welcome
import starrailchatbox.shared.generated.resources.no_characters
import starrailchatbox.shared.generated.resources.online
import starrailchatbox.shared.generated.resources.open_emoji
import starrailchatbox.shared.generated.resources.read_status
import starrailchatbox.shared.generated.resources.received_message_description
import starrailchatbox.shared.generated.resources.record_voice
import starrailchatbox.shared.generated.resources.nav_chat
import starrailchatbox.shared.generated.resources.send_message
import starrailchatbox.shared.generated.resources.sent_message_description
import starrailchatbox.shared.generated.resources.today
import starrailchatbox.shared.generated.resources.view_attachments
import starrailchatbox.shared.generated.resources.attachments_title
import starrailchatbox.shared.generated.resources.open_file
import starrailchatbox.shared.generated.resources.close
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.tooling.preview.Preview
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.character.CharactersUiState
import com.kaixuan.starrailchatbox.ui.character.CharacterAction
import com.kaixuan.starrailchatbox.ui.navigation.Route

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import starrailchatbox.shared.generated.resources.hold_to_speak
import starrailchatbox.shared.generated.resources.release_to_send
import starrailchatbox.shared.generated.resources.release_to_cancel
import starrailchatbox.shared.generated.resources.retry

import androidx.compose.runtime.DisposableEffect
import com.kaixuan.starrailchatbox.platform.rememberAudioPlayer
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode

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
    val characterId = selectedCharacter?.id
    val characterChatState = state.characterStates[characterId] ?: CharacterChatState()
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

    // 在 reverseLayout 模式下，索引 0 是列表底部。
    // 我们将消息按倒序排列，Header 放在最后（即最顶部）。
    
    val uriHandler = LocalUriHandler.current

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
                uriHandler.openUri(attachment.uri)
            }
        )
    }

    // 缓存每个角色的 LazyListState
    val pageListStates = remember { mutableMapOf<String, LazyListState>() }
    val currentStates = remember(characters) {
        characters.associate { character ->
            character.id to pageListStates.getOrPut(character.id) {
                // 在 reverseLayout 下，默认就在底部 (index 0)，无需特殊初始化
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

                val isAtBottom by remember {
                    derivedStateOf {
                        pageListState.firstVisibleItemIndex == 0
                    }
                }

                val isAtTop by remember {
                    derivedStateOf {
                        val lastVisibleItem = pageListState.layoutInfo.visibleItemsInfo.lastOrNull()
                        lastVisibleItem != null && lastVisibleItem.index == pageListState.layoutInfo.totalItemsCount - 1
                    }
                }

                // 只要不是既在顶又在底（即内容多于一屏），就显示按钮
                val showScrollButton by remember {
                    derivedStateOf {
                        pageListState.layoutInfo.totalItemsCount > 0 && !(isAtTop && isAtBottom)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = pageListState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        contentPadding = PaddingValues(
                            start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                            end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                            bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.lg,
                            top = StarRailSpacing.md,
                        ),
                        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
                    ) {
                        // 倒序排列：消息在前（index 0 在底），Header 在后（在顶）
                        pageMessages.asReversed().forEachIndexed { reversedIndex, message ->
                            // 计算原始索引以处理日期分割线逻辑
                            val index = pageMessages.size - 1 - reversedIndex

                            item(key = message.id) {
                                MessageItem(
                                    message = message,
                                    charactersById = charactersById,
                                    userAvatarUri = state.userAvatarUri,
                                    compact = compact,
                                    playingAudioUri = playingAudioUri,
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
                                            uriHandler.openUri(attachment.uri)
                                        }
                                    },
                                    onAvatarClick = {
                                        onMainAction(MainAction.NavigateTo(Route.ConversationManagement))
                                    },
                                    onAction = onAction
                                )
                            }

                            val showDivider = if (index > 0) {
                                val prevMessage = pageMessages[index - 1]
                                !com.kaixuan.starrailchatbox.platform.isSameDay(message.createdAt, prevMessage.createdAt)
                            } else {
                                false
                            }

                            if (showDivider) {
                                item(key = "date_${message.id}") {
                                    DateDivider(com.kaixuan.starrailchatbox.platform.formatHeaderDate(message.createdAt))
                                }
                            }
                        }

                        item(key = "header") {
                            ChatHeader(
                                selectedCharacter = pageCharacter,
                                compact = compact,
                                onAction = onAction,
                                onCharacterAction = onCharacterAction,
                                onMainAction = onMainAction,
                                modifier = Modifier.padding(bottom = StarRailSpacing.md)
                            )
                        }
                    }

                    if (showScrollButton) {
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    if (isAtTop) {
                                        // 如果在顶部，点击滚动到底部 (index 0)
                                        pageListState.scrollToItem(0)
                                    } else {
                                        // 否则滚动到顶部 (最后一个 item)
                                        pageListState.scrollToItem(pageListState.layoutInfo.totalItemsCount - 1)
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                                    bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.md,
                                )
                                .size(if (compact) 38.dp else 48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            ),
                            shadowElevation = 4.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                StarRailIcon(
                                    kind = StarRailIconKind.ARROW_UP,
                                    contentDescription = if (isAtTop) "滚动到底部" else "滚动到最顶部",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .size(if (compact) 20.dp else 24.dp)
                                        .graphicsLayer {
                                            if (isAtTop) rotationZ = 180f
                                        },
                                )
                            }
                        }
                    }
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

        if (previewImageUri != null) {
            FullScreenImagePreview(
                uri = previewImageUri!!,
                onDismiss = { previewImageUri = null }
            )
        }
    }
}

@Composable
fun RecordingOverlay(
    isCancelTargeted: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isCancelTargeted) {
        Brush.verticalGradient(
            listOf(
                Color(0xFFCC4141).copy(alpha = 0.9f),
                Color(0xFFCC4141).copy(alpha = 0.5f),
                Color.Transparent
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF418FCC).copy(alpha = 0.9f),
                Color(0xFF418FCC).copy(alpha = 0.5f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 160.dp)
        ) {
            Text(
                text = stringResource(
                    if (isCancelTargeted) Res.string.release_to_cancel else Res.string.release_to_send
                ),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(32.dp))
            // Waveform (Simulated)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(30) { index ->
                    // 模拟波形高度
                    val height = when (index % 5) {
                        0 -> 12.dp
                        1 -> 24.dp
                        2 -> 38.dp
                        3 -> 28.dp
                        else -> 16.dp
                    }
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height)
                            .background(
                                if (isCancelTargeted) Color(0xFFFF9999) else Color.White,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

private suspend fun LazyListState.scrollToMessageBottomAfterLayout(
    messagesStartIndex: Int,
    lastMessageIndex: Int,
) {
    if (lastMessageIndex < 0) return

    withFrameNanos { }
    withFrameNanos { }

    scrollToItem(
        index = messagesStartIndex + lastMessageIndex,
        scrollOffset = Int.MAX_VALUE,
    )
}

@Composable
private fun ChatHeader(
    selectedCharacter: Character,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onCharacterAction: (CharacterAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.md else StarRailSpacing.xl,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterSummary(
            character = selectedCharacter,
            modifier = Modifier.weight(1f),
            compact = compact,
            onAvatarClick = {
                onMainAction(MainAction.NavigateTo(Route.ConversationManagement))
            }
        )
        HeaderActions(
            characterId = selectedCharacter.id,
            compact = compact,
            onAction = onAction,
            onCharacterAction = onCharacterAction,
            onMainAction = onMainAction,
        )
    }
}

@Composable
private fun CharacterSummary(
    character: Character,
    compact: Boolean,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = character.name
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.clickable { onAvatarClick() }) {
            CharacterAvatar(
                character = character,
                size = if (compact) 72.dp else 88.dp,
                selected = true,
                contentDescription = name,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
            ) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = if (compact) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    maxLines = 1,
                )
                StarRailIcon(
                    kind = StarRailIconKind.SPARKLE,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (compact) 18.dp else 24.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
            ) {
                Box(
                    Modifier
                        .size(if (compact) 8.dp else 10.dp)
                        .background(MaterialTheme.starRailColors.online, CircleShape),
                )
                Text(
                    text = stringResource(Res.string.online),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = if (compact) {
                        MaterialTheme.typography.bodySmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                )
            }
        }
    }
}

@Composable
private fun HeaderActions(
    characterId: String,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onCharacterAction: (CharacterAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        Triple(
            HeaderAction.CONVERSATION_MANAGEMENT,
            Res.string.action_conversation_management,
            StarRailIconKind.CONVERSATION,
        ),
        Triple(
            HeaderAction.CHARACTER_EDIT,
            Res.string.action_character_edit,
            StarRailIconKind.EDIT,
        ),
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.sm else StarRailSpacing.md
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { (action, labelResource, icon) ->
            val label = stringResource(labelResource)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    if (compact) StarRailSpacing.xxs else StarRailSpacing.xs
                ),
            ) {
                Surface(
                    onClick = {
                        when (action) {
                            HeaderAction.CONVERSATION_MANAGEMENT -> {
                                onMainAction(MainAction.NavigateTo(Route.ConversationManagement))
                            }
                            HeaderAction.CHARACTER_EDIT -> {
                                onCharacterAction(CharacterAction.CharacterEditOpened(characterId))
                                onMainAction(MainAction.NavigateTo(Route.CharacterEdit(characterId)))
                            }
                            HeaderAction.VOICE -> {
                                onAction(ChatAction.HeaderActionClicked(action))
                            }
                        }
                    },
                    modifier = Modifier.size(if (compact) 48.dp else 56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                    shadowElevation = 2.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        StarRailIcon(
                            kind = icon,
                            contentDescription = label,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(if (compact) 22.dp else 26.dp),
                        )
                    }
                }
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = if (compact) {
                        MaterialTheme.typography.labelMedium
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                )
            }
        }
    }
}

@Composable
private fun CharacterSelector(
    characters: List<Character>,
    selectedCharacterId: String?,
    compact: Boolean,
    onCharacterSelected: (String) -> Unit,
) {
    val displayedCharacters = remember(characters, compact) {
        val sorted = characters.sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
        if (compact) sorted.take(4) else sorted
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        val rowModifier = if (compact) {
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = StarRailSpacing.xxs,
                    vertical = StarRailSpacing.xs,
                )
        } else {
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = StarRailSpacing.sm,
                    vertical = StarRailSpacing.sm,
                )
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(
                if (compact) StarRailSpacing.xxs else StarRailSpacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            displayedCharacters.forEach { character ->
                CharacterSelectorItem(
                    character = character,
                    selected = character.id == selectedCharacterId,
                    compact = compact,
                    onClick = { onCharacterSelected(character.id) },
                    modifier = if (compact) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.width(104.dp)
                    },
                )
            }
        }
    }
}

@Composable
private fun CharacterSelectorItem(
    character: Character,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = character.name
    val selectionDescription = stringResource(
        if (selected) {
            Res.string.character_selected_description
        } else {
            Res.string.character_selection_description
        },
        name,
    )
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = selectionDescription
            }
            .padding(vertical = if (compact) StarRailSpacing.xxs else StarRailSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.xxs else StarRailSpacing.xs
        ),
    ) {
        CharacterAvatar(
            character = character,
            size = if (compact) 56.dp else 68.dp,
            selected = selected,
            contentDescription = null,
        )
        Text(
            text = name,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            style = if (compact) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
        Box(
            Modifier
                .width(if (compact) 56.dp else 72.dp)
                .height(if (compact) 3.dp else 4.dp)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.starRailColors.avatarRingStart,
                                MaterialTheme.starRailColors.avatarRingEnd,
                            ),
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Transparent),
                        )
                    },
                ),
        )
    }
}

@Composable
fun CharacterAvatar(
    character: Character,
    size: androidx.compose.ui.unit.Dp,
    selected: Boolean,
    contentDescription: String?,
) {
    val ringBrush = if (selected) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.starRailColors.avatarRingStart,
                MaterialTheme.starRailColors.avatarRingEnd,
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.colorScheme.outline,
            ),
        )
    }
    Box(
        modifier = Modifier
            .size(size)
            .background(ringBrush, CircleShape)
            .padding(if (selected) 3.dp else 2.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AvatarImage(
            avatarUri = character.avatarUri,
            contentDescription = contentDescription,
            placeholderKind = StarRailIconKind.PROFILE,
            placeholderSize = size / 2,
        )
    }
}

@Composable
private fun DateDivider(dateText: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = StarRailSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f).padding(horizontal = StarRailSpacing.md),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
        StarRailIcon(
            kind = StarRailIconKind.SPARKLE,
            contentDescription = null,
            tint = MaterialTheme.starRailColors.constellation,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = dateText,
            modifier = Modifier.padding(horizontal = StarRailSpacing.sm),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        StarRailIcon(
            kind = StarRailIconKind.SPARKLE,
            contentDescription = null,
            tint = MaterialTheme.starRailColors.constellation,
            modifier = Modifier.size(18.dp),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f).padding(horizontal = StarRailSpacing.md),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MessageItem(
    message: ChatMessageUiModel,
    charactersById: Map<String, Character>,
    userAvatarUri: String?,
    compact: Boolean,
    playingAudioUri: String?,
    onViewAttachments: (List<MessageAttachment>) -> Unit,
    onOpenAttachment: (MessageAttachment) -> Unit,
    onAvatarClick: () -> Unit,
    onAction: (ChatAction) -> Unit,
) {
    when (message) {
        is ChatMessageUiModel.Received -> ReceivedMessage(
            message = message,
            sender = charactersById[message.senderId],
            compact = compact,
            playingAudioUri = playingAudioUri,
            onViewAttachments = onViewAttachments,
            onOpenAttachment = onOpenAttachment,
            onAvatarClick = onAvatarClick,
        )
        is ChatMessageUiModel.Sent -> SentMessage(
            message = message,
            userAvatarUri = userAvatarUri,
            compact = compact,
            playingAudioUri = playingAudioUri,
            onViewAttachments = onViewAttachments,
            onOpenAttachment = onOpenAttachment,
            onAction = onAction,
        )
    }
}

@Composable
private fun ReceivedMessage(
    message: ChatMessageUiModel.Received,
    sender: Character?,
    compact: Boolean,
    playingAudioUri: String?,
    onViewAttachments: (List<MessageAttachment>) -> Unit,
    onOpenAttachment: (MessageAttachment) -> Unit,
    onAvatarClick: () -> Unit,
) {
    val text = message.content.resolve()
    val senderName = sender?.name ?: message.senderId
    val semanticDescription = stringResource(
        Res.string.received_message_description,
        senderName,
        text,
        message.timestamp,
    )
    val voiceAttachment = message.attachments.find { it.mimeType.startsWith("audio/") }
    val isVoiceOnly = voiceAttachment != null

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticDescription },
    ) {
        val bubbleMaxWidth = maxWidth * if (compact) 0.72f else 0.74f

        if (isVoiceOnly) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (sender != null) {
                        Box(modifier = Modifier.clickable { onAvatarClick() }) {
                            CharacterAvatar(
                                character = sender,
                                size = if (compact) 40.dp else 44.dp,
                                selected = true,
                                contentDescription = null,
                            )
                        }
                    }
                    VoiceMessageBubble(
                        durationMs = voiceAttachment.durationMs ?: 0L,
                        compact = compact,
                        isSent = false,
                        isPlaying = playingAudioUri == voiceAttachment.uri,
                        onClick = { onOpenAttachment(voiceAttachment) }
                    )
                }
                Row(
                    modifier = Modifier.padding(
                        start = if (sender != null) {
                            (if (compact) 40.dp else 44.dp) + StarRailSpacing.sm
                        } else 0.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)
                ) {
                    Text(
                        text = message.timestamp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                if (sender != null) {
                    Box(modifier = Modifier.clickable { onAvatarClick() }) {
                        CharacterAvatar(
                            character = sender,
                            size = if (compact) 40.dp else 44.dp,
                            selected = true,
                            contentDescription = null,
                        )
                    }
                }
                Column(
                    modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.starRailColors.receivedBubbleBorder,
                        ),
                        shadowElevation = 1.dp,
                    ) {
                        Column {
                            if (text.isNotBlank()) {
                                Text(
                                    text = text,
                                    modifier = Modifier.padding(
                                        horizontal = if (compact) 12.dp else StarRailSpacing.md,
                                        vertical = if (compact) 8.dp else StarRailSpacing.sm,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            MessageAttachments(
                                attachments = message.attachments,
                                onOpenAttachment = onOpenAttachment,
                                compact = compact,
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)
                    ) {
                        val attachments = message.attachments
                        val hasAttachmentsBtn = attachments.any { !it.mimeType.startsWith("image/") } && attachments.isNotEmpty()
                        if (hasAttachmentsBtn) {
                            Text(
                                text = stringResource(Res.string.view_attachments),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { onViewAttachments(attachments) }
                            )
                        }
                        Text(
                            text = message.timestamp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SentMessage(
    message: ChatMessageUiModel.Sent,
    userAvatarUri: String?,
    compact: Boolean,
    playingAudioUri: String?,
    onViewAttachments: (List<MessageAttachment>) -> Unit,
    onOpenAttachment: (MessageAttachment) -> Unit,
    onAction: (ChatAction) -> Unit,
) {
    val text = message.content.resolve()
    val semanticDescription = stringResource(
        Res.string.sent_message_description,
        text,
        message.timestamp,
    )
    val voiceAttachment = message.attachments.find { it.mimeType.startsWith("audio/") }
    val isVoiceOnly = voiceAttachment != null

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticDescription },
    ) {
        val bubbleMaxWidth = maxWidth * if (compact) 0.72f else 0.74f

        if (isVoiceOnly) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VoiceMessageBubble(
                        durationMs = voiceAttachment.durationMs ?: 0L,
                        compact = compact,
                        isSent = true,
                        isPlaying = playingAudioUri == voiceAttachment.uri,
                        onClick = { onOpenAttachment(voiceAttachment) }
                    )
                    Spacer(Modifier.width(StarRailSpacing.sm))
                    Surface(
                        modifier = Modifier.size(if (compact) 32.dp else 48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.starRailColors.sentBubbleBorder,
                        ),
                    ) {
                        AvatarImage(
                            avatarUri = userAvatarUri.orEmpty(),
                            contentDescription = null,
                            placeholderKind = StarRailIconKind.SPARKLE,
                            placeholderSize = if (compact) 18.dp else 26.dp,
                            isUser = true,
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(
                        end = (if (compact) 32.dp else 48.dp) + StarRailSpacing.sm
                    ),
                    horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message.timestamp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    MessageStatusIcon(
                        status = message.status,
                        isRead = message.isRead,
                        onRetry = { onAction(ChatAction.RetrySendMessage(message.id)) },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.starRailColors.sentBubbleBorder,
                        ),
                        shadowElevation = 1.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            if (text.isNotBlank()) {
                                Text(
                                    text = text,
                                    modifier = Modifier.padding(
                                        horizontal = if (compact) 12.dp else StarRailSpacing.md,
                                        vertical = if (compact) 8.dp else StarRailSpacing.sm,
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            MessageAttachments(
                                attachments = message.attachments,
                                onOpenAttachment = onOpenAttachment,
                                compact = compact,
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val attachments = message.attachments
                        val hasAttachmentsBtn = attachments.any { !it.mimeType.startsWith("image/") } && attachments.isNotEmpty()
                        if (hasAttachmentsBtn) {
                            Text(
                                text = stringResource(Res.string.view_attachments),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { onViewAttachments(attachments) }
                            )
                        }
                        Text(
                            text = message.timestamp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        MessageStatusIcon(
                            status = message.status,
                            isRead = message.isRead,
                            onRetry = { onAction(ChatAction.RetrySendMessage(message.id)) },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.width(StarRailSpacing.sm))
                Surface(
                    modifier = Modifier.size(if (compact) 32.dp else 48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.starRailColors.sentBubbleBorder,
                    ),
                ) {
                    AvatarImage(
                        avatarUri = userAvatarUri.orEmpty(),
                        contentDescription = null,
                        placeholderKind = StarRailIconKind.SPARKLE,
                        placeholderSize = if (compact) 18.dp else 26.dp,
                        isUser = true,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageAttachments(
    attachments: List<MessageAttachment>,
    onOpenAttachment: (MessageAttachment) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val images = remember(attachments) {
        attachments.filter { it.mimeType.startsWith("image/") }
    }
    if (images.isEmpty()) return

    FlowRow(
        modifier = modifier.padding(StarRailSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
    ) {
        images.forEach { attachment ->
            val isSingle = images.size == 1
            Box(
                modifier = Modifier
                    .then(
                        if (isSingle) Modifier.widthIn(max = 240.dp).aspectRatio(16f / 9f)
                        else Modifier.size(if (compact) 80.dp else 100.dp)
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { onOpenAttachment(attachment) },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = attachment.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun VoiceMessageBubble(
    durationMs: Long,
    compact: Boolean,
    isSent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationSec = (durationMs / 1000).coerceAtLeast(1)
    // 根据时长调整气泡宽度，模仿语音气泡效果
    val minWidth = if (compact) 80.dp else 100.dp
    val maxWidth = if (compact) 160.dp else 200.dp
    val bubbleWidth = (minWidth + (maxWidth - minWidth) * (durationSec.toFloat() / 60f).coerceAtMost(1f))

    val backgroundColor = if (isSent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (isSent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isSent) {
        MaterialTheme.starRailColors.sentBubbleBorder
    } else {
        MaterialTheme.starRailColors.receivedBubbleBorder
    }

    // 播放时的透明度微动画（0.4f -> 1.0f 呼吸渐变效果）
    val infiniteTransition = rememberInfiniteTransition()
    val animatedAlpha by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
        modifier = modifier.width(bubbleWidth)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else StarRailSpacing.md,
                vertical = if (compact) 8.dp else StarRailSpacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
        ) {
            if (isSent) {
                Text(
                    text = "${durationSec}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
                Spacer(Modifier.width(StarRailSpacing.xs))
                StarRailIcon(
                    kind = StarRailIconKind.VOICE_WAVE,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(if (compact) 18.dp else 22.dp)
                        .graphicsLayer { 
                            rotationY = 180f 
                            alpha = animatedAlpha
                        }
                )
            } else {
                StarRailIcon(
                    kind = StarRailIconKind.VOICE_WAVE,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(if (compact) 18.dp else 22.dp)
                        .graphicsLayer {
                            alpha = animatedAlpha
                        }
                )
                Spacer(Modifier.width(StarRailSpacing.xs))
                Text(
                    text = "${durationSec}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun AttachmentsDialog(
    attachments: List<MessageAttachment>,
    onDismissRequest: () -> Unit,
    onOpenAttachment: (MessageAttachment) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.attachments_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                attachments.forEach { attachment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onOpenAttachment(attachment) }
                            .padding(StarRailSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)
                    ) {
                        val isImage = attachment.mimeType.startsWith("image/")
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            StarRailIcon(
                                kind = if (isImage) StarRailIconKind.GALLERY else StarRailIconKind.FILE,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attachment.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = attachment.mimeType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onOpenAttachment(attachment) }) {
                            Text(stringResource(Res.string.open_file))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text(stringResource(Res.string.close))
            }
        }
    )
}

@Composable
private fun FullScreenImagePreview(
    uri: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
            onClick = onDismiss
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * 底部会话发送与快捷回复区域组件
 */
@Composable
fun ChatSessionBottomBar(
    state: ChatUiState,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onRecordingStateChanged: (isRecording: Boolean, isCancelTargeted: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val characterId = state.selectedCharacterId
    val isVoiceMode = characterId?.let { state.characterStates[it]?.isVoiceMode } ?: false
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberImagePicker { picked ->
        picked?.let { 
            coroutineScope.launch {
                val compressedUri = com.kaixuan.starrailchatbox.platform.compressImageIfPossible(it.uri)
                onAction(ChatAction.ImageSelected(compressedUri, it.name))
            }
        }
    }
    val filePicker = rememberFilePicker { picked ->
        picked?.let { onAction(ChatAction.FileSelected(it.uri, it.name)) }
    }
    val cameraLauncher = rememberCameraLauncher { picked ->
        picked?.let { 
            coroutineScope.launch {
                val compressedUri = com.kaixuan.starrailchatbox.platform.compressImageIfPossible(it.uri)
                onAction(ChatAction.ImageSelected(compressedUri, it.name))
            }
        }
    }

    val interceptedOnAction: (ChatAction) -> Unit = { action ->
        if (action is ChatAction.ComposerActionClicked) {
            when (action.action) {
                ComposerAction.PICK_IMAGE -> imagePicker()
                ComposerAction.PICK_FILE -> filePicker()
                ComposerAction.TAKE_PHOTO -> cameraLauncher()
                else -> onAction(action)
            }
        } else {
            onAction(action)
        }
    }

    Column(modifier = modifier) {
        if (state.selectedAttachments.isNotEmpty()) {
            SelectedAttachmentsArea(
                attachments = state.selectedAttachments,
                compact = compact,
                onAddClicked = { interceptedOnAction(ChatAction.ComposerActionClicked(ComposerAction.ATTACH)) },
                onRemoveClicked = { interceptedOnAction(ChatAction.RemoveAttachment(it)) },
            )
        } else {
            QuickReplies(
                suggestions = state.suggestions,
                compact = compact,
                onReplyClicked = {
                    interceptedOnAction(ChatAction.QuickReplyClicked(it))
                },
            )
        }
        MessageComposer(
            value = state.messageDraft,
            isSending = state.isSending,
            isVoiceMode = isVoiceMode,
            attachments = state.selectedAttachments,
            compact = compact,
            onValueChange = {
                interceptedOnAction(ChatAction.MessageChanged(it))
            },
            onSend = { interceptedOnAction(ChatAction.SendClicked) },
            onComposerAction = {
                interceptedOnAction(ChatAction.ComposerActionClicked(it))
            },
            onRecordingStateChanged = onRecordingStateChanged,
            onVoiceFinished = { uri, duration ->
                interceptedOnAction(ChatAction.VoiceRecordingFinished(uri, duration))
            }
        )
        if (state.isAttachmentPanelVisible) {
            AttachmentPanel(
                compact = compact,
                onAction = { interceptedOnAction(ChatAction.ComposerActionClicked(it)) }
            )
        }
    }
}

@Composable
private fun AttachmentPanel(
    compact: Boolean,
    onAction: (ComposerAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (compact) StarRailSpacing.md else StarRailSpacing.lg,
                end = if (compact) StarRailSpacing.md else StarRailSpacing.lg,
                bottom = if (compact) StarRailSpacing.md else StarRailSpacing.lg,
                top = StarRailSpacing.xxs,
            )
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.lg else StarRailSpacing.xl
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AttachmentItem(
            icon = StarRailIconKind.FILE,
            label = stringResource(Res.string.attach_file),
            compact = compact,
            onClick = { onAction(ComposerAction.PICK_FILE) }
        )
        AttachmentItem(
            icon = StarRailIconKind.CAMERA,
            label = stringResource(Res.string.attach_camera),
            compact = compact,
            onClick = { onAction(ComposerAction.TAKE_PHOTO) }
        )
        AttachmentItem(
            icon = StarRailIconKind.GALLERY,
            label = stringResource(Res.string.attach_gallery),
            compact = compact,
            onClick = { onAction(ComposerAction.PICK_IMAGE) }
        )
    }
}

@Composable
private fun AttachmentItem(
    icon: StarRailIconKind,
    label: String,
    compact: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(StarRailSpacing.xs)
    ) {
        Surface(
            modifier = Modifier.size(if (compact) 52.dp else 60.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                StarRailIcon(
                    kind = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(if (compact) 26.dp else 30.dp)
                )
            }
        }
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickReplies(
    suggestions: List<String>,
    compact: Boolean,
    onReplyClicked: (String) -> Unit,
) {
    if (suggestions.isEmpty()) return

    // 限制最多展示 4 个 suggestions，并将其分为最多 2 行（每行最多 2 个）
    val chunkedSuggestions = suggestions.take(4).chunked(2)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                vertical = if (compact) StarRailSpacing.xxs else StarRailSpacing.xs,
            ),
        verticalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.xxs else StarRailSpacing.xs
        ),
    ) {
        chunkedSuggestions.forEach { rowSuggestions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 32.dp else 40.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    if (compact) StarRailSpacing.xxs else StarRailSpacing.xs
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowSuggestions.forEach { suggestion ->
                    Surface(
                        onClick = { onReplyClicked(suggestion) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (compact) StarRailSpacing.xs else StarRailSpacing.sm),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = suggestion,
                                style = if (compact) {
                                    MaterialTheme.typography.labelMedium
                                } else {
                                    MaterialTheme.typography.labelLarge
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // 如果行元素少于 2 个，则在右侧填充一个 weight(1f) 的 Spacer，保证第一个元素只占一半宽度
                if (rowSuggestions.size < 2) {
                    Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun SelectedAttachmentsArea(
    attachments: List<SelectedAttachment>,
    compact: Boolean,
    onAddClicked: () -> Unit,
    onRemoveClicked: (SelectedAttachment) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                vertical = StarRailSpacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        attachments.forEach { attachment ->
            AttachmentPreviewItem(
                attachment = attachment,
                compact = compact,
                onRemove = { onRemoveClicked(attachment) }
            )
        }

//        Surface(
//            onClick = onAddClicked,
//            modifier = Modifier.size(if (compact) 48.dp else 56.dp),
//            shape = MaterialTheme.shapes.medium,
//            color = MaterialTheme.colorScheme.surfaceContainerHigh,
//            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
//        ) {
//            Box(contentAlignment = Alignment.Center) {
//                StarRailIcon(
//                    kind = StarRailIconKind.ADD,
//                    contentDescription = "继续添加",
//                    tint = MaterialTheme.colorScheme.onSurface,
//                    modifier = Modifier.size(if (compact) 24.dp else 28.dp)
//                )
//            }
//        }
    }
}

@Composable
private fun AttachmentPreviewItem(
    attachment: SelectedAttachment,
    compact: Boolean,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(if (compact) 56.dp else 64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        when (attachment) {
            is SelectedAttachment.Image -> {
                AvatarImage(
                    avatarUri = attachment.uri,
                    contentDescription = null,
                    placeholderKind = StarRailIconKind.GALLERY,
                    placeholderSize = 24.dp,
                )
            }
            is SelectedAttachment.File -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    StarRailIcon(
                        kind = StarRailIconKind.FILE,
                        contentDescription = attachment.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is SelectedAttachment.Voice -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    StarRailIcon(
                        kind = StarRailIconKind.MICROPHONE,
                        contentDescription = attachment.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Surface(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(16.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ) {
            Box(contentAlignment = Alignment.Center) {
                StarRailIcon(
                    kind = StarRailIconKind.CLOSE,
                    contentDescription = "移除",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    value: String,
    isSending: Boolean,
    isVoiceMode: Boolean,
    attachments: List<SelectedAttachment>,
    compact: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onComposerAction: (ComposerAction) -> Unit,
    onRecordingStateChanged: (Boolean, Boolean) -> Unit,
    onVoiceFinished: (String, Long) -> Unit,
) {
    var dragOffsetY by remember { mutableStateOf(0f) }
    val audioRecorder = rememberAudioRecorder()
    var isRecordingActive by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                top = if (compact) StarRailSpacing.xs else StarRailSpacing.md,
                bottom = StarRailSpacing.sm,
            ),
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComposerIconButton(
            icon = StarRailIconKind.ADD,
            contentDescription = stringResource(Res.string.add_attachment),
            compact = compact,
            onClick = { onComposerAction(ComposerAction.ATTACH) },
        )

        if (isVoiceMode) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 38.dp else 52.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                if (audioRecorder.hasPermission()) {
                                    dragOffsetY = 0f
                                    isRecordingActive = true
                                    audioRecorder.startRecording()
                                    onRecordingStateChanged(true, false)
                                } else {
                                    isRecordingActive = false
                                    audioRecorder.requestPermission { granted ->
                                        // 权限申请成功，下一次长按便能开始录音
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isRecordingActive) {
                                    isRecordingActive = false
                                    if (dragOffsetY < -160f) {
                                        audioRecorder.stopRecording(cancel = true)
                                        onRecordingStateChanged(false, false)
                                    } else {
                                        onRecordingStateChanged(false, false)
                                        val result = audioRecorder.stopRecording(cancel = false)
                                        if (result != null) {
                                            onVoiceFinished(result.uri, result.durationMs)
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                if (isRecordingActive) {
                                    isRecordingActive = false
                                    audioRecorder.stopRecording(cancel = true)
                                    onRecordingStateChanged(false, false)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (isRecordingActive) {
                                    dragOffsetY += dragAmount.y
                                    onRecordingStateChanged(true, dragOffsetY < -160f)
                                }
                            }
                        )
                    },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.hold_to_speak),
                        style = if (compact) {
                            MaterialTheme.typography.labelLarge
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        } else {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 0.dp, minHeight = if (compact) 38.dp else 52.dp)
                    .animateContentSize(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                minLines = 1,
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.message_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }

        if (!isVoiceMode && (value.isNotBlank() || attachments.isNotEmpty() || isSending)) {
            ComposerIconButton(
                icon = StarRailIconKind.SEND,
                contentDescription = stringResource(Res.string.send_message),
                compact = compact,
                enabled = !isSending,
                primary = true,
                onClick = onSend,
                loading = isSending,
            )
        }

        ComposerIconButton(
            icon = if (isVoiceMode) StarRailIconKind.KEYBOARD else StarRailIconKind.MICROPHONE,
            contentDescription = stringResource(if (isVoiceMode) Res.string.nav_chat else Res.string.record_voice),
            compact = compact,
            onClick = { onComposerAction(ComposerAction.VOICE) },
        )
    }
}

@Composable
private fun ComposerIconButton(
    icon: StarRailIconKind,
    contentDescription: String,
    compact: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    loading: Boolean = false,
) {
    val containerColor = if (primary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (primary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(if (compact) 38.dp else 52.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = if (primary) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        shadowElevation = if (primary && enabled) 5.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else {
                StarRailIcon(
                    kind = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(if (compact) 20.dp else 24.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(
    status: MessageStatus,
    isRead: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        MessageStatus.SENDING -> {
            CircularProgressIndicator(
                modifier = modifier.size(14.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        }
        MessageStatus.FAILED -> {
            Box(
                modifier = modifier
                    .size(16.dp)
                    .clickable { onRetry() },
                contentAlignment = Alignment.Center
            ) {
                StarRailIcon(
                    kind = StarRailIconKind.RETRY,
                    contentDescription = stringResource(Res.string.retry),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        MessageStatus.SENT -> {
            if (isRead) {
                StarRailIcon(
                    kind = StarRailIconKind.CHECK,
                    contentDescription = stringResource(Res.string.read_status),
                    tint = MaterialTheme.starRailColors.successCheck,
                    modifier = modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageContent.resolve(): String = when (this) {
    is MessageContent.Custom -> text
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ChatSessionScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        ChatSessionScreen(
            state = chatPreviewState,
            charactersState = charactersPreviewState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onAction = {},
            onCharacterAction = {},
            onMainAction = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ChatSessionScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        ChatSessionScreen(
            state = chatPreviewState,
            charactersState = charactersPreviewState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onAction = {},
            onCharacterAction = {},
            onMainAction = {}
        )
    }
}

private val previewCharacters = listOf(
    previewCharacter("builtin:流萤", "流萤"),
    previewCharacter("builtin:三月七", "三月七"),
    previewCharacter("builtin:黄泉", "黄泉"),
    previewCharacter("builtin:瑕蝶", "瑕蝶"),
)

private val charactersPreviewState = CharactersUiState(
    characters = previewCharacters,
    selectedCharacterId = "builtin:流萤",
    isLoadingCharacters = false,
)

private val chatPreviewState = ChatUiState(
    selectedCharacterId = "builtin:流萤",
    selectedCharacter = previewCharacters.first(),
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

private fun previewCharacter(
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
private fun ChatSessionBottomBarLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatSessionBottomBar(
                state = chatPreviewState,
                compact = true,
                onAction = {},
                onRecordingStateChanged = { _, _ -> }
            )
        }
    }
}

@Preview
@Composable
private fun ChatSessionBottomBarDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatSessionBottomBar(
                state = chatPreviewState,
                compact = true,
                onAction = {},
                onRecordingStateChanged = { _, _ -> }
            )
        }
    }
}

/**
 * 二级对话界面，面向前四个角色以外的角色。
 */
@Composable
fun CharacterChatScreen(
    characterId: String,
    state: ChatUiState,
    charactersState: CharactersUiState,
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
    
    // ... (保持现有逻辑)

    val pageState = state.characterStates[characterId] ?: CharacterChatState()

    var isRecording by remember { mutableStateOf(false) }
    var isCancelTargeted by remember { mutableStateOf(false) }
    var attachmentsToShow by remember { mutableStateOf<List<MessageAttachment>?>(null) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = pageState.isAttachmentPanelVisible) {
        onAction(ChatAction.ComposerActionClicked(ComposerAction.ATTACH))
    }

    BackHandler(enabled = previewImageUri != null) {
        previewImageUri = null
    }

    BackHandler {
        onMainAction(MainAction.PopBackStack)
    }
    val pageListState = rememberLazyListState()
    val pageMessages = pageState.messages
    val latestMessageId = remember(pageMessages) { pageMessages.lastOrNull()?.id }
    val charactersById = remember(charactersState.characters) {
        charactersState.characters.associateBy(Character::id)
    }

    val uriHandler = LocalUriHandler.current

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
                uriHandler.openUri(attachment.uri)
            }
        )
    }

    LaunchedEffect(characterId) {
        // 在 reverseLayout 下，切换角色默认就在底部
    }

    LaunchedEffect(latestMessageId) {
        if (latestMessageId != null) {
            // 已经处于最底部时，发送或收到消息，自动滚回底部
            if (pageListState.firstVisibleItemIndex <= 1) {
                pageListState.animateScrollToItem(0)
            }
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            pageListState.firstVisibleItemIndex == 0
        }
    }

    val isAtTop by remember {
        derivedStateOf {
            val lastVisibleItem = pageListState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index == pageListState.layoutInfo.totalItemsCount - 1
        }
    }

    // 只要不是既在顶又在底（即内容多于一屏），就显示按钮
    val showScrollButton by remember {
        derivedStateOf {
            pageListState.layoutInfo.totalItemsCount > 0 && !(isAtTop && isAtBottom)
        }
    }

    val coroutineScope = rememberCoroutineScope()

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
                        LazyColumn(
                            state = pageListState,
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = true,
                            contentPadding = PaddingValues(
                                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                                bottom = scaffoldPadding.calculateBottomPadding() + StarRailSpacing.lg,
                                top = StarRailSpacing.md,
                            ),
                            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
                        ) {
                            pageMessages.asReversed().forEachIndexed { reversedIndex, message ->
                                val index = pageMessages.size - 1 - reversedIndex

                                item(key = message.id) {
                                    MessageItem(
                                        message = message,
                                        charactersById = charactersById,
                                        userAvatarUri = state.userAvatarUri,
                                        compact = compact,
                                        playingAudioUri = playingAudioUri,
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
                                                uriHandler.openUri(attachment.uri)
                                            }
                                        },
                                        onAvatarClick = {
                                            onMainAction(MainAction.NavigateTo(Route.ConversationManagement))
                                        },
                                        onAction = onAction
                                    )
                                }

                                val showDivider = if (index > 0) {
                                    val prevMessage = pageMessages[index - 1]
                                    !com.kaixuan.starrailchatbox.platform.isSameDay(message.createdAt, prevMessage.createdAt)
                                } else {
                                    false
                                }

                                if (showDivider) {
                                    item(key = "date_${message.id}") {
                                        DateDivider(com.kaixuan.starrailchatbox.platform.formatHeaderDate(message.createdAt))
                                    }
                                }
                            }
                        }

                        if (showScrollButton) {
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        if (isAtTop) {
                                            pageListState.scrollToItem(0)
                                        } else {
                                            pageListState.scrollToItem(pageListState.layoutInfo.totalItemsCount - 1)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(
                                        start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                                        bottom = scaffoldPadding.calculateBottomPadding() + StarRailSpacing.md,
                                    )
                                    .size(if (compact) 38.dp else 48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                ),
                                shadowElevation = 4.dp,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    StarRailIcon(
                                        kind = StarRailIconKind.ARROW_UP,
                                        contentDescription = if (isAtTop) "滚动到底部" else "滚动到最顶部",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .size(if (compact) 20.dp else 24.dp)
                                            .graphicsLayer {
                                                if (isAtTop) rotationZ = 180f
                                            },
                                    )
                                }
                            }
                        }
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

        if (previewImageUri != null) {
            FullScreenImagePreview(
                uri = previewImageUri!!,
                onDismiss = { previewImageUri = null }
            )
        }
    }
}

@Composable
private fun CharacterChatHeader(
    selectedCharacter: Character,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onCharacterAction: (CharacterAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.md else StarRailSpacing.lg,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
        ) {
            IconButton(
                onClick = { onMainAction(MainAction.PopBackStack) },
                modifier = Modifier.size(if (compact) 32.dp else 40.dp),
            ) {
                StarRailIcon(
                    kind = StarRailIconKind.CHEVRON_LEFT,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(if (compact) 24.dp else 28.dp),
                )
            }
            Text(
                text = stringResource(Res.string.app_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = if (compact) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.headlineLarge
                },
                maxLines = 1,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                if (compact) StarRailSpacing.md else StarRailSpacing.xl,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CharacterSummary(
                character = selectedCharacter,
                modifier = Modifier.weight(1f),
                compact = compact,
                onAvatarClick = {
                    onMainAction(MainAction.NavigateTo(Route.ConversationManagement))
                }
            )
            HeaderActions(
                characterId = selectedCharacter.id,
                compact = compact,
                onAction = onAction,
                onCharacterAction = onCharacterAction,
                onMainAction = onMainAction,
            )
        }
    }
}

