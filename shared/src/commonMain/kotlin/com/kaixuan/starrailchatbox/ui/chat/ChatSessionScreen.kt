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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.action_profile
import starrailchatbox.shared.generated.resources.action_settings
import starrailchatbox.shared.generated.resources.action_voice
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
import starrailchatbox.shared.generated.resources.quick_reply_mood
import starrailchatbox.shared.generated.resources.quick_reply_night
import starrailchatbox.shared.generated.resources.quick_reply_story
import starrailchatbox.shared.generated.resources.quick_reply_today
import starrailchatbox.shared.generated.resources.read_status
import starrailchatbox.shared.generated.resources.received_message_description
import starrailchatbox.shared.generated.resources.record_voice
import starrailchatbox.shared.generated.resources.send_message
import starrailchatbox.shared.generated.resources.chat_empty_greeting
import starrailchatbox.shared.generated.resources.sent_message_description
import starrailchatbox.shared.generated.resources.today
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import androidx.compose.foundation.ExperimentalFoundationApi
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
    val listState = rememberLazyListState()
    val messagesStartIndex = 3
    var previousMessageCount by remember { mutableStateOf(state.messages.size) }
    val selectedCharacter = state.selectedCharacter
    val charactersById = remember(state.characters) {
        state.characters.associateBy(Character::id)
    }

    LaunchedEffect(state.messages.size) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        val wasNearBottom = lastVisibleIndex != null &&
            lastVisibleIndex >= listState.layoutInfo.totalItemsCount - 3
        if (
            state.messages.size > previousMessageCount &&
            wasNearBottom &&
            state.messages.isNotEmpty()
        ) {
            listState.animateScrollToItem(messagesStartIndex + state.messages.lastIndex)
        }
        previousMessageCount = state.messages.size
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(
            start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
            top = StarRailSpacing.lg,
            end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
            bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
    ) {
        item(key = "header") {
            if (selectedCharacter != null) {
                ChatHeader(
                    selectedCharacter = selectedCharacter,
                    compact = compact,
                    onAction = onAction,
                    onMainAction = onMainAction,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(StarRailSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isLoadingCharacters) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = stringResource(Res.string.no_characters),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        stickyHeader(key = "characters") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(top = StarRailSpacing.xxs),
            ) {
                CharacterSelector(
                    characters = state.characters,
                    selectedCharacterId = selectedCharacter?.id,
                    compact = compact,
                    onCharacterSelected = {
                        onAction(ChatAction.CharacterSelected(it))
                    },
                )
            }
        }
        item(key = "date") {
            DateDivider()
        }
        items(
            items = state.messages,
            key = ChatMessageUiModel::id,
        ) { message ->
            MessageItem(
                message = message,
                charactersById = charactersById,
                compact = compact,
            )
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
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        Triple(HeaderAction.VOICE, Res.string.action_voice, StarRailIconKind.VOICE),
        Triple(HeaderAction.PROFILE, Res.string.action_profile, StarRailIconKind.PROFILE),
        Triple(HeaderAction.SETTINGS, Res.string.action_settings, StarRailIconKind.SETTINGS),
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
                        if (action == HeaderAction.SETTINGS) {
                            onMainAction(MainAction.NavigationSelected(Route.Settings))
                        } else {
                            onAction(ChatAction.HeaderActionClicked(action))
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = if (compact) StarRailSpacing.xxs else StarRailSpacing.sm,
                    vertical = if (compact) StarRailSpacing.xs else StarRailSpacing.sm,
                ),
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
                    modifier = Modifier.width(if (compact) 82.dp else 104.dp),
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
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(3.dp),
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

private data class QuickReply(
    val label: StringResource,
    val icon: StarRailIconKind,
)

@Composable
private fun QuickReplies(
    compact: Boolean,
    onReplyClicked: (String) -> Unit,
) {
    val replies = listOf(
        QuickReply(Res.string.quick_reply_mood, StarRailIconKind.HEART),
        QuickReply(Res.string.quick_reply_today, StarRailIconKind.CHAT),
        QuickReply(Res.string.quick_reply_story, StarRailIconKind.SPARKLE),
        QuickReply(Res.string.quick_reply_night, StarRailIconKind.MOON),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                vertical = if (compact) StarRailSpacing.xxs else StarRailSpacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.xxs else StarRailSpacing.xs
        ),
    ) {
        replies.forEach { reply ->
            val label = stringResource(reply.label)
            Surface(
                onClick = { onReplyClicked(label) },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (compact) 12.dp else StarRailSpacing.md,
                        vertical = if (compact) 4.dp else StarRailSpacing.xs,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (compact) StarRailSpacing.xxs else StarRailSpacing.xs
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StarRailIcon(
                        kind = reply.icon,
                        contentDescription = null,
                        tint = if (reply.icon == StarRailIconKind.HEART ||
                            reply.icon == StarRailIconKind.MOON
                        ) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                    )
                    Text(
                        text = label,
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
    is MessageContent.Resource -> stringResource(copy.resource())
}

private fun ChatCopy.resource(): StringResource = when (this) {
    ChatCopy.EMPTY_GREETING -> Res.string.chat_empty_greeting
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ChatSessionScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        ChatSessionScreen(
            state = ChatUiState(),
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
            state = ChatUiState(),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onAction = {},
            onMainAction = {}
        )
    }
}
