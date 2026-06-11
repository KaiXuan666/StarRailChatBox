package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.action_copy
import starrailchatbox.shared.generated.resources.read_status
import starrailchatbox.shared.generated.resources.received_message_description
import starrailchatbox.shared.generated.resources.retry
import starrailchatbox.shared.generated.resources.sent_message_description
import starrailchatbox.shared.generated.resources.view_attachments

@Composable
fun MessageItem(
    message: ChatMessageUiModel,
    charactersById: Map<String, CharacterSummary>,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReceivedMessage(
    message: ChatMessageUiModel.Received,
    sender: CharacterSummary?,
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
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

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
                        onClick = { onOpenAttachment(voiceAttachment) },
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
                    Box {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.starRailColors.receivedBubbleBorder,
                            ),
                            shadowElevation = 1.dp,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = { showMenu = true }
                            )
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
                        Box(Modifier.align(Alignment.TopEnd)) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = DpOffset(x = 0.dp, y = (-48).dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.action_copy)) },
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(text))
                                        showMenu = false
                                    }
                                )
                            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SentMessage(
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
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

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
                        onClick = { onOpenAttachment(voiceAttachment) },
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
                    Box {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.starRailColors.sentBubbleBorder,
                            ),
                            shadowElevation = 1.dp,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = { showMenu = true }
                            )
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
                        Box(Modifier.align(Alignment.TopEnd)) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = DpOffset(x = 0.dp, y = (-48).dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.action_copy)) },
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(text))
                                        showMenu = false
                                    }
                                )
                            }
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

@Composable
fun MessageStatusIcon(
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
fun MessageContent.resolve(): String = when (this) {
    is MessageContent.Custom -> text
}
