package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.navigation.Route
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.character_list_title
import starrailchatbox.shared.generated.resources.character_list_my_characters
import starrailchatbox.shared.generated.resources.character_list_create_btn
import starrailchatbox.shared.generated.resources.character_list_edit_desc
import starrailchatbox.shared.generated.resources.character_list_empty

@Composable
fun CharactersScreen(
    state: ChatUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    StarRailPageLayout(
        title = stringResource(Res.string.character_list_title),
        contentPadding = contentPadding,
        compact = compact,
        modifier = modifier,
        contentSpacing = StarRailSpacing.md,
    ) {
        // "我的角色" 与 "新建角色" 标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = StarRailSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.character_list_my_characters),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Surface(
                onClick = {
                    // 跳转至新建角色页
                    onMainAction(MainAction.NavigateTo(Route.CharacterEdit(null)))
                },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.character_list_create_btn),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    StarRailIcon(
                        kind = StarRailIconKind.ADD,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // 角色卡片列表
        if (state.characters.isEmpty() && !state.isLoadingCharacters) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.character_list_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            state.characters.forEachIndexed { index, character ->
                CharacterCard(
                    character = character,
                    index = index + 1,
                    compact = compact,
                    onClick = {
                        // 选中该角色，并导航至对话 Tab
                        onAction(ChatAction.CharacterSelected(character.id))
                        onMainAction(MainAction.NavigationSelected(Route.ChatSession))
                    },
                    onEditClick = {
                        // 跳转至编辑角色页
                        onMainAction(MainAction.NavigateTo(Route.CharacterEdit(character.id)))
                    }
                )
            }
        }
    }
}

@Composable
private fun CharacterCard(
    character: Character,
    index: Int,
    compact: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = StarRailSpacing.md, vertical = StarRailSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧序号卡片
            Surface(
                modifier = Modifier.size(if (compact) 32.dp else 40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = index.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 圆形头像
            CharacterListAvatar(
                avatarBytes = character.avatarBytes,
                contentDescription = null,
                size = if (compact) 56.dp else 64.dp
            )

            // 中间文字描述（占据剩余可用空间）
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = character.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = character.prompt.takeIf { it.isNotBlank() } ?: character.openingMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 右侧编辑按钮
            Surface(
                onClick = onEditClick,
                modifier = Modifier.size(if (compact) 36.dp else 40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StarRailIcon(
                        kind = StarRailIconKind.EDIT,
                        contentDescription = stringResource(Res.string.character_list_edit_desc),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterListAvatar(
    avatarBytes: ByteArray,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
) {
    val colors = MaterialTheme.starRailColors
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.avatarRingStart,
                        colors.avatarRingEnd,
                    ),
                ),
                CircleShape,
            )
            .padding(2.5.dp) // 头像与外环边缘微小内边距，遵循 UI-Design.md 规则：“头像与渐变外环之间避免使用过大内边距”
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = remember(avatarBytes) {
            runCatching { avatarBytes.decodeToImageBitmap() }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            StarRailIcon(
                kind = StarRailIconKind.SPARKLE,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.44f),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharactersScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        CharactersScreen(
            state = previewState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharactersScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        CharactersScreen(
            state = previewState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {}
        )
    }
}

private val previewState = ChatUiState(
    characters = listOf(
        Character(
            id = "builtin:三月七",
            name = "三月七",
            prompt = "热情开朗，元气满满的少女。",
            openingMessage = "今天想聊点什么呢？",
            avatarBytes = byteArrayOf()
        ),
        Character(
            id = "builtin:黄泉",
            name = "黄泉",
            prompt = "神秘优雅，掌控生死的使者。",
            openingMessage = "你好。",
            avatarBytes = byteArrayOf()
        ),
        Character(
            id = "builtin:流萤",
            name = "流萤",
            prompt = "温柔坚韧，追寻光明的少女。",
            openingMessage = "你好啊。",
            avatarBytes = byteArrayOf()
        ),
        Character(
            id = "builtin:瑕蝶",
            name = "瑕蝶",
            prompt = "梦幻灵动，守护记忆的使者。",
            openingMessage = "欢迎回来。",
            avatarBytes = byteArrayOf()
        )
    ),
    isLoadingCharacters = false
)
