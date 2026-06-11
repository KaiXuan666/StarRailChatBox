package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.cancel
import starrailchatbox.shared.generated.resources.conversation_management_count
import starrailchatbox.shared.generated.resources.conversation_management_companion_days
import starrailchatbox.shared.generated.resources.conversation_management_current
import starrailchatbox.shared.generated.resources.conversation_management_delete
import starrailchatbox.shared.generated.resources.conversation_management_delete_confirm_action
import starrailchatbox.shared.generated.resources.conversation_management_delete_confirm_message
import starrailchatbox.shared.generated.resources.conversation_management_delete_confirm_title
import starrailchatbox.shared.generated.resources.conversation_management_description
import starrailchatbox.shared.generated.resources.conversation_management_empty
import starrailchatbox.shared.generated.resources.conversation_management_empty_desc
import starrailchatbox.shared.generated.resources.conversation_management_message_count
import starrailchatbox.shared.generated.resources.conversation_management_my_sessions
import starrailchatbox.shared.generated.resources.conversation_management_new
import starrailchatbox.shared.generated.resources.conversation_management_no_preview
import starrailchatbox.shared.generated.resources.conversation_management_open
import starrailchatbox.shared.generated.resources.conversation_management_title
import starrailchatbox.shared.generated.resources.navigation_back
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.main.MainAction
import kotlin.time.Clock

/**
 * 会话管理界面，用于查看、创建、切换和删除与特定角色的历史对话会话。
 */
@Composable
fun ConversationManagementScreen(
    state: ChatUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val character = state.selectedCharacter
    val sessions = state.sessions
    var pendingDelete by remember { mutableStateOf<ConversationSummaryUiModel?>(null) }

    BackHandler(enabled = pendingDelete == null) {
        onMainAction(MainAction.PopBackStack)
    }

    StarRailPageLayout(
        title = stringResource(Res.string.conversation_management_title),
        contentPadding = contentPadding,
        compact = compact,
        backContentDescription = stringResource(Res.string.navigation_back),
        onBackClick = { onMainAction(MainAction.PopBackStack) },
        modifier = modifier,
        contentSpacing = StarRailSpacing.lg,
    ) {
        if (character != null) {
            CharacterConversationCard(
                character = character,
                sessionCount = sessions.size,
                companionDays = calculateCompanionDays(
                    createdAt = character.createdAt,
                    now = Clock.System.now().toEpochMilliseconds(),
                ),
                compact = compact,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.conversation_management_my_sessions),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = {
                    onAction(ChatAction.NewSessionClicked)
                    onMainAction(MainAction.PopBackStack)
                },
                enabled = character != null && !state.isSending && !state.isLoadingSession,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 5.dp,
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = StarRailSpacing.md,
                        vertical = StarRailSpacing.sm,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.ADD,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(Res.string.conversation_management_new),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            EmptyConversationCard()
        } else {
            sessions.forEach { session ->
                ConversationCard(
                    session = session,
                    selected = session.id == state.activeSessionId,
                    enabled = !state.isSending && !state.isLoadingSession,
                    onOpen = {
                        onAction(ChatAction.SessionSelected(session.id))
                        onMainAction(MainAction.PopBackStack)
                    },
                    onDelete = {
                        pendingDelete = session
                    },
                )
            }
        }
    }

    pendingDelete?.let { session ->
        StarRailDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(Res.string.conversation_management_delete_confirm_title),
            dismissText = stringResource(Res.string.cancel),
            confirmText = stringResource(
                Res.string.conversation_management_delete_confirm_action,
            ),
            destructive = true,
            onConfirm = {
                pendingDelete = null
                onAction(ChatAction.SessionDeleteClicked(session.id))
            },
        ) {
            Text(
                text = stringResource(
                    Res.string.conversation_management_delete_confirm_message,
                    session.title,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CharacterConversationCard(
    character: Character,
    sessionCount: Int,
    companionDays: Int,
    compact: Boolean,
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        Row(
            modifier = Modifier.padding(if (compact) StarRailSpacing.md else StarRailSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(if (compact) 72.dp else 88.dp),
                shape = CircleShape,
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                AvatarImage(
                    avatarUri = character.avatarUri,
                    contentDescription = character.name,
                    placeholderKind = StarRailIconKind.SPARKLE,
                    placeholderSize = if (compact) 32.dp else 40.dp,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = character.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    StarRailIcon(
                        kind = StarRailIconKind.SPARKLE,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = stringResource(Res.string.conversation_management_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xxs),
            ) {
                Text(
                    text = stringResource(
                        Res.string.conversation_management_companion_days,
                        companionDays,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
//                Text(
//                    text = stringResource(Res.string.conversation_management_count, sessionCount),
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    style = MaterialTheme.typography.bodyMedium,
//                )
            }
        }
    }
}

@Composable
private fun ConversationCard(
    session: ConversationSummaryUiModel,
    selected: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f))
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(StarRailSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = session.title.take(1),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.title,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (selected) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = stringResource(Res.string.conversation_management_current),
                                modifier = Modifier.padding(
                                    horizontal = StarRailSpacing.xs,
                                    vertical = StarRailSpacing.xxs,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                Text(
                    text = session.preview.ifBlank {
                        stringResource(Res.string.conversation_management_no_preview)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${stringResource(
                        Res.string.conversation_management_message_count,
                        session.messageCount,
                    )} · ${session.updatedAt}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Surface(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StarRailIcon(
                        kind = StarRailIconKind.DELETE,
                        contentDescription = stringResource(
                            Res.string.conversation_management_delete,
                            session.title,
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

internal fun calculateCompanionDays(
    createdAt: Long,
    now: Long,
): Int {
    if (createdAt <= 0L || now < createdAt) return 0
    return ((now - createdAt) / MillisecondsPerDay + 1L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
}

private const val MillisecondsPerDay = 24L * 60L * 60L * 1_000L

@Composable
private fun EmptyConversationCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(StarRailSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        ) {
            StarRailIcon(
                kind = StarRailIconKind.CONVERSATION,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(Res.string.conversation_management_empty),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.conversation_management_empty_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ConversationManagementLightPreview() {
    ConversationManagementPreview(darkTheme = false)
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ConversationManagementDarkPreview() {
    ConversationManagementPreview(darkTheme = true)
}

@Composable
private fun ConversationManagementPreview(darkTheme: Boolean) {
    StarRailTheme(darkThemeOverride = darkTheme) {
        val previewChar = Character(
            id = "preview",
            name = "流萤",
            prompt = "",
            openingMessage = "",
            avatarUri = "",
            createdAt = Clock.System.now().toEpochMilliseconds() - 85L * MillisecondsPerDay,
        )
        ConversationManagementScreen(
            state = ChatUiState(
                selectedCharacterId = "preview",
                selectedCharacter = previewChar,
                characterStates = mapOf(
                    "preview" to CharacterChatState(
                        activeSessionId = "session-1",
                        sessions = listOf(
                            ConversationSummaryUiModel(
                                id = "session-1",
                                title = "今天的晚安聊天",
                                preview = "你今天辛苦了，要不要我陪你聊一会？",
                                updatedAt = "22:14",
                                messageCount = 12,
                            ),
                            ConversationSummaryUiModel(
                                id = "session-2",
                                title = "旅行计划讨论",
                                preview = "我们先从路线和预算开始吧。",
                                updatedAt = "昨天",
                                messageCount = 8,
                            ),
                        ),
                    ),
                ),
            ),
            contentPadding = PaddingValues(),
            compact = true,
            onAction = {},
            onMainAction = {},
        )
    }
}
