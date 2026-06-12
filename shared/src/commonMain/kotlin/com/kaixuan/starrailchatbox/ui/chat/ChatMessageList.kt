package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.retry
import starrailchatbox.shared.generated.resources.scroll_to_latest_message
import starrailchatbox.shared.generated.resources.scroll_to_oldest_message

@Composable
fun ChatMessageList(
    messages: LazyPagingItems<ChatTimelineItem>,
    listState: LazyListState,
    charactersById: Map<String, CharacterSummary>,
    userAvatarUri: String?,
    compact: Boolean,
    isSending: Boolean,
    historyAnchor: ChatHistoryAnchor,
    playingAudioUri: String?,
    contentPadding: PaddingValues,
    onViewAttachments: (List<MessageAttachment>) -> Unit,
    onOpenAttachment: (MessageAttachment) -> Unit,
    onAvatarClick: () -> Unit,
    onAction: (ChatAction) -> Unit,
    headerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val refreshState = messages.loadState.refresh
    val appendState = messages.loadState.append
    val historyLoaded = appendState is LoadState.NotLoading &&
        appendState.endOfPaginationReached

    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }

    val isAtTop by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null &&
                lastVisibleItem.index == listState.layoutInfo.totalItemsCount - 1
        }
    }

    val showScrollButton by remember(historyAnchor) {
        derivedStateOf {
            listState.layoutInfo.totalItemsCount > 0 &&
                (historyAnchor == ChatHistoryAnchor.OLDEST || !(isAtTop && isAtBottom))
        }
    }
    val scrollsToOldest = historyAnchor == ChatHistoryAnchor.LATEST && isAtBottom

    if (refreshState is LoadState.Loading && messages.itemCount == 0) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
        ) {
            items(
                count = messages.itemCount,
                key = { index -> messages.peek(index)?.key ?: "placeholder_$index" },
                contentType = { index ->
                    when (messages.peek(index)) {
                        is ChatTimelineItem.Message -> "message"
                        is ChatTimelineItem.DateDivider -> "date"
                        null -> "placeholder"
                    }
                },
            ) { index ->
                when (val item = messages[index]) {
                    is ChatTimelineItem.Message -> {
                        val displayedMessage = if (
                            index == 0 &&
                            isSending &&
                            item.message is ChatMessageUiModel.Sent &&
                            item.message.status == MessageStatus.SENT
                        ) {
                            item.message.copy(status = MessageStatus.SENDING)
                        } else {
                            item.message
                        }
                        MessageItem(
                            message = displayedMessage,
                            charactersById = charactersById,
                            userAvatarUri = userAvatarUri,
                            compact = compact,
                            playingAudioUri = playingAudioUri,
                            onViewAttachments = onViewAttachments,
                            onOpenAttachment = onOpenAttachment,
                            onAvatarClick = onAvatarClick,
                            onAction = onAction,
                        )
                    }
                    is ChatTimelineItem.DateDivider -> DateDivider(item.text)
                    null -> Unit
                }
            }

            when (appendState) {
                is LoadState.Loading -> item(key = "history_loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(StarRailSpacing.md),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is LoadState.Error -> item(key = "history_error") {
                    PagingRetryItem(onRetry = messages::retry)
                }
                is LoadState.NotLoading -> {
                    if (historyLoaded && headerContent != null) {
                        item(key = "header") {
                            headerContent()
                        }
                    }
                }
            }

            if (refreshState is LoadState.Error && messages.itemCount == 0) {
                item(key = "refresh_error") {
                    PagingRetryItem(onRetry = messages::retry)
                }
            }
        }

        if (showScrollButton) {
            Surface(
                onClick = {
                    if (scrollsToOldest) {
                        onAction(ChatAction.ScrollToOldestMessage)
                    } else {
                        onAction(ChatAction.ScrollToLatestMessage)
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
                        contentDescription = stringResource(
                            if (scrollsToOldest) {
                                Res.string.scroll_to_oldest_message
                            } else {
                                Res.string.scroll_to_latest_message
                            },
                        ),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(if (compact) 20.dp else 24.dp)
                            .graphicsLayer {
                                if (!scrollsToOldest) rotationZ = 180f
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun PagingRetryItem(onRetry: () -> Unit) {
    Text(
        text = stringResource(Res.string.retry),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRetry)
            .padding(StarRailSpacing.md),
    )
}

@Composable
fun DateDivider(dateText: String) {
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
