package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.action_character_edit
import starrailchatbox.shared.generated.resources.action_conversation_management
import starrailchatbox.shared.generated.resources.add_attachment
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
import starrailchatbox.shared.generated.resources.send_message
import starrailchatbox.shared.generated.resources.sent_message_description
import starrailchatbox.shared.generated.resources.today
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.tooling.preview.Preview
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.navigation.Route

/**
 * 聊天会话主屏组件 (原 ChatContent 模块)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatSessionScreen(
    state: ChatUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val characters = state.characters
    val selectedCharacter = state.selectedCharacter
    val coroutineScope = rememberCoroutineScope()

    if (state.isLoadingCharacters) {
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

    // 缓存每个角色的 LazyListState
    val pageListStates = remember { mutableMapOf<String, LazyListState>() }
    val currentStates = remember(characters) {
        characters.associate { it.id to (pageListStates[it.id] ?: LazyListState()) }
    }

    val messagesStartIndex = 3

    LaunchedEffect(selectedCharacter?.id) {
        val targetPage = characters.indexOfFirst { it.id == selectedCharacter?.id }
        if (targetPage != -1 && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val targetCharacter = characters.getOrNull(pagerState.currentPage)
        if (targetCharacter != null && targetCharacter.id != selectedCharacter?.id) {
            onAction(ChatAction.CharacterSelected(targetCharacter.id))
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
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

            var previousPageMessageCount by remember { mutableStateOf(pageMessages.size) }
            var shouldPageScrollToBottomOnLoad by remember { mutableStateOf(false) }

            LaunchedEffect(pageCharacter.id) {
                previousPageMessageCount = pageMessages.size
                if (pageMessages.isNotEmpty()) {
                    pageListState.scrollToItem(messagesStartIndex + pageMessages.lastIndex)
                } else {
                    shouldPageScrollToBottomOnLoad = true
                }
            }

            LaunchedEffect(pageMessages, pageState.isLoadingSession) {
                if (shouldPageScrollToBottomOnLoad && !pageState.isLoadingSession && pageMessages.isNotEmpty()) {
                    shouldPageScrollToBottomOnLoad = false
                    pageListState.scrollToItem(messagesStartIndex + pageMessages.lastIndex)
                }
            }

            LaunchedEffect(pageMessages.size) {
                val lastVisibleIndex = pageListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val wasNearBottom = lastVisibleIndex != null &&
                    lastVisibleIndex >= pageListState.layoutInfo.totalItemsCount - 3
                if (
                    pageMessages.size > previousPageMessageCount &&
                    wasNearBottom &&
                    pageMessages.isNotEmpty()
                ) {
                    pageListState.scrollToItem(messagesStartIndex + pageMessages.lastIndex)
                }
                previousPageMessageCount = pageMessages.size
            }

            val showScrollToTop by remember {
                derivedStateOf {
                    pageListState.firstVisibleItemIndex > 0 || pageListState.firstVisibleItemScrollOffset > 0
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = pageListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                        end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                        bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
                ) {
                    item(key = "header") {
                        ChatHeader(
                            selectedCharacter = pageCharacter,
                            compact = compact,
                            onAction = onAction,
                            onMainAction = onMainAction,
                        )
                    }
                    stickyHeader(key = "characters") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .padding(top = StarRailSpacing.xxs),
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
                                            val targetListState = currentStates[characterId]
                                            if (targetListState != null) {
                                                val targetPageState = state.characterStates[characterId]
                                                val targetMessages = targetPageState?.messages.orEmpty()
                                                if (targetMessages.isNotEmpty()) {
                                                    targetListState.scrollToItem(messagesStartIndex + targetMessages.lastIndex)
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                    item(key = "date") {
                        DateDivider()
                    }
                    items(
                        items = pageMessages,
                        key = ChatMessageUiModel::id,
                    ) { message ->
                        MessageItem(
                            message = message,
                            charactersById = charactersById,
                            compact = compact,
                        )
                    }
                }

                if (showScrollToTop) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                pageListState.scrollToItem(0)
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
                                contentDescription = "滚动到最顶部",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(if (compact) 20.dp else 24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(
    selectedCharacter: Character,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.md else StarRailSpacing.lg,
        ),
    ) {
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
            )
            HeaderActions(
                characterId = selectedCharacter.id,
                compact = compact,
                onAction = onAction,
                onMainAction = onMainAction,
            )
        }
    }
}

@Composable
private fun CharacterSummary(
    character: Character,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val name = character.name
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterAvatar(
            character = character,
            size = if (compact) 72.dp else 88.dp,
            selected = true,
            contentDescription = name,
        )
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
            StarRailIconKind.SETTINGS,
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
                                onAction(ChatAction.CharacterEditOpened(characterId))
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
            characters.forEach { character ->
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
    val painter = remember(character.avatarBytes) {
        runCatching {
            BitmapPainter(character.avatarBytes.decodeToImageBitmap())
        }.getOrNull()
    }
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
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            StarRailIcon(
                kind = StarRailIconKind.PROFILE,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}

@Composable
private fun DateDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.width(90.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        StarRailIcon(
            kind = StarRailIconKind.SPARKLE,
            contentDescription = null,
            tint = MaterialTheme.starRailColors.constellation,
            modifier = Modifier.size(24.dp).padding(5.dp),
        )
        Text(
            text = stringResource(Res.string.today),
            modifier = Modifier.padding(horizontal = StarRailSpacing.xs),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        StarRailIcon(
            kind = StarRailIconKind.SPARKLE,
            contentDescription = null,
            tint = MaterialTheme.starRailColors.constellation,
            modifier = Modifier.size(24.dp).padding(5.dp),
        )
        HorizontalDivider(
            modifier = Modifier.width(90.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun MessageItem(
    message: ChatMessageUiModel,
    charactersById: Map<String, Character>,
    compact: Boolean,
) {
    when (message) {
        is ChatMessageUiModel.Received -> ReceivedMessage(
            message = message,
            sender = charactersById[message.senderId],
            compact = compact,
        )
        is ChatMessageUiModel.Sent -> SentMessage(
            message = message,
            compact = compact,
        )
    }
}

@Composable
private fun ReceivedMessage(
    message: ChatMessageUiModel.Received,
    sender: Character?,
    compact: Boolean,
) {
    val text = message.content.resolve()
    val senderName = sender?.name ?: message.senderId
    val semanticDescription = stringResource(
        Res.string.received_message_description,
        senderName,
        text,
        message.timestamp,
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticDescription },
    ) {
        val bubbleMaxWidth = maxWidth * if (compact) 0.72f else 0.74f
        Row(
            horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            if (sender != null) {
                CharacterAvatar(
                    character = sender,
                    size = if (compact) 40.dp else 44.dp,
                    selected = true,
                    contentDescription = null,
                )
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
                Text(
                    text = message.timestamp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SentMessage(
    message: ChatMessageUiModel.Sent,
    compact: Boolean,
) {
    val text = message.content.resolve()
    val semanticDescription = stringResource(
        Res.string.sent_message_description,
        text,
        message.timestamp,
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticDescription },
    ) {
        val bubbleMaxWidth = maxWidth * if (compact) 0.72f else 0.74f
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message.timestamp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (message.isRead) {
                        StarRailIcon(
                            kind = StarRailIconKind.CHECK,
                            contentDescription = stringResource(Res.string.read_status),
                            tint = MaterialTheme.starRailColors.successCheck,
                            modifier = Modifier.size(16.dp),
                        )
                    }
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
                Box(contentAlignment = Alignment.Center) {
                    StarRailIcon(
                        kind = StarRailIconKind.SPARKLE,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (compact) 18.dp else 26.dp),
                    )
                }
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        QuickReplies(
            suggestions = state.suggestions,
            compact = compact,
            onReplyClicked = {
                onAction(ChatAction.QuickReplyClicked(it))
            },
        )
        MessageComposer(
            value = state.messageDraft,
            isSending = state.isSending,
            compact = compact,
            onValueChange = {
                onAction(ChatAction.MessageChanged(it))
            },
            onSend = { onAction(ChatAction.SendClicked) },
            onComposerAction = {
                onAction(ChatAction.ComposerActionClicked(it))
            },
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
private fun MessageComposer(
    value: String,
    isSending: Boolean,
    compact: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onComposerAction: (ComposerAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
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
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minWidth = 0.dp, minHeight = if (compact) 38.dp else 56.dp)
                .animateContentSize(),
            placeholder = {
                Text(stringResource(Res.string.message_placeholder))
            },
            trailingIcon = {
                IconButton(
                    onClick = { onComposerAction(ComposerAction.EMOJI) },
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.SMILE,
                        contentDescription = stringResource(Res.string.open_emoji),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            minLines = 1,
            maxLines = 4,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
        ComposerIconButton(
            icon = StarRailIconKind.SEND,
            contentDescription = stringResource(Res.string.send_message),
            compact = compact,
            enabled = value.isNotBlank() && !isSending,
            primary = true,
            onClick = onSend,
            loading = isSending,
        )
        ComposerIconButton(
            icon = StarRailIconKind.MICROPHONE,
            contentDescription = stringResource(Res.string.record_voice),
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
private fun MessageContent.resolve(): String = when (this) {
    is MessageContent.Custom -> text
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ChatSessionScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        ChatSessionScreen(
            state = chatPreviewState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onAction = {},
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
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onAction = {},
            onMainAction = {}
        )
    }
}

private val chatPreviewState = ChatUiState(
    characters = listOf(
        previewCharacter("builtin:流萤", "流萤"),
        previewCharacter("builtin:三月七", "三月七"),
        previewCharacter("builtin:黄泉", "黄泉"),
        previewCharacter("builtin:瑕蝶", "瑕蝶"),
    ),
    selectedCharacterId = "builtin:流萤",
    characterStates = mapOf(
        "builtin:流萤" to CharacterChatState(
            activeSessionId = "preview-session",
            messages = listOf(
                ChatMessageUiModel.Received(
                    id = "preview-opening",
                    timestamp = "10:21",
                    content = MessageContent.Custom("今天要聊点什么呢？"),
                    senderId = "builtin:流萤",
                ),
                ChatMessageUiModel.Sent(
                    id = "preview-user-1",
                    timestamp = "10:22",
                    content = MessageContent.Custom("今天有点累，想和你聊聊天。"),
                    isRead = true,
                ),
                ChatMessageUiModel.Received(
                    id = "preview-assistant-1",
                    timestamp = "10:23",
                    content = MessageContent.Custom(
                        "好呀，我会认真听着。发生了什么让你觉得累呢？",
                    ),
                    senderId = "builtin:流萤",
                ),
                ChatMessageUiModel.Sent(
                    id = "preview-user-2",
                    timestamp = "10:24",
                    content = MessageContent.Custom("忙了一整天，不过现在感觉好多了。"),
                    isRead = true,
                ),
                ChatMessageUiModel.Received(
                    id = "preview-assistant-2",
                    timestamp = "10:25",
                    content = MessageContent.Custom(
                        "那就先放松一下吧。你已经很努力了，剩下的时间留给自己。",
                    ),
                    senderId = "builtin:流萤",
                ),
            ),
            messageDraft = "想听你讲一个星空下的故事",
            isLoadingSession = false,
            suggestions = listOf("讲讲星核猎手", "你喜欢橡木蛋糕卷吗", "关于这片星空...", "想听听你的过去"),
            )
    ),
    isLoadingCharacters = false,
)

private fun previewCharacter(
    id: String,
    name: String,
) = Character(
    id = id,
    name = name,
    prompt = "Preview prompt for $name",
    openingMessage = "今天要聊点什么呢？",
    avatarBytes = byteArrayOf(),
)

@Preview
@Composable
private fun ChatSessionBottomBarLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatSessionBottomBar(
                state = chatPreviewState,
                compact = true,
                onAction = {}
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
                onAction = {}
            )
        }
    }
}
