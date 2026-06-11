package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import kotlinx.coroutines.launch

@Composable
fun ChatMessageList(
    messages: List<ChatMessageUiModel>,
    listState: LazyListState,
    charactersById: Map<String, CharacterSummary>,
    userAvatarUri: String?,
    compact: Boolean,
    playingAudioUri: String?,
    contentPadding: PaddingValues,
    onViewAttachments: (List<MessageAttachment>) -> Unit,
    onOpenAttachment: (MessageAttachment) -> Unit,
    onAvatarClick: () -> Unit,
    onAction: (ChatAction) -> Unit,
    headerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }

    val isAtTop by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index == listState.layoutInfo.totalItemsCount - 1
        }
    }

    // 只要不是既在顶又在底（即内容多于一屏），就显示按钮
    val showScrollButton by remember {
        derivedStateOf {
            listState.layoutInfo.totalItemsCount > 0 && !(isAtTop && isAtBottom)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
        ) {
            // 倒序排列：消息在前（index 0 在底），Header 在后（在顶）
            messages.asReversed().forEachIndexed { reversedIndex, message ->
                // 计算原始索引以处理日期分割线逻辑
                val index = messages.size - 1 - reversedIndex

                item(key = message.id) {
                    MessageItem(
                        message = message,
                        charactersById = charactersById,
                        userAvatarUri = userAvatarUri,
                        compact = compact,
                        playingAudioUri = playingAudioUri,
                        onViewAttachments = onViewAttachments,
                        onOpenAttachment = onOpenAttachment,
                        onAvatarClick = onAvatarClick,
                        onAction = onAction
                    )
                }

                val showDivider = if (index > 0) {
                    val prevMessage = messages[index - 1]
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

            if (headerContent != null) {
                item(key = "header") {
                    headerContent()
                }
            }
        }

        if (showScrollButton) {
            Surface(
                onClick = {
                    coroutineScope.launch {
                        if (isAtTop) {
                            // 如果在顶部，点击滚动到底部 (index 0)
                            listState.scrollToItem(0)
                        } else {
                            // 否则滚动到顶部 (最后一个 item)
                            listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1)
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
