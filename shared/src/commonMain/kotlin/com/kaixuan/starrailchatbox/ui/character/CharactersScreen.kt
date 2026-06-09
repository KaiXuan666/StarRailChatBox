package com.kaixuan.starrailchatbox.ui.character

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import com.kaixuan.starrailchatbox.platform.formatLocalTime
import com.kaixuan.starrailchatbox.ui.components.StarRailPageHeader
import starrailchatbox.shared.generated.resources.character_list_drag_tip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.navigation.Route
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.character_list_title
import starrailchatbox.shared.generated.resources.character_list_my_characters
import starrailchatbox.shared.generated.resources.character_list_create_btn
import starrailchatbox.shared.generated.resources.character_list_edit_desc
import starrailchatbox.shared.generated.resources.character_list_empty
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntOffset
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import starrailchatbox.shared.generated.resources.cancel
import starrailchatbox.shared.generated.resources.confirm
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_title
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_message
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_action
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun CharactersScreen(
    state: CharactersUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onAction: (CharacterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val cardHeightWithSpacing = if (compact) 96.dp else 104.dp
    val thresholdPx = with(density) { cardHeightWithSpacing.toPx() }

    val sortedCharacters = remember(state.characters) {
        state.characters.sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
    }

    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var currentList by remember(sortedCharacters) { mutableStateOf(sortedCharacters) }
    var deleteTargetCharacter by remember { mutableStateOf<Character?>(null) }

    LaunchedEffect(sortedCharacters) {
        if (draggingItemId == null) {
            currentList = sortedCharacters
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + StarRailSpacing.lg,
                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
            ),
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
    ) {
        StarRailPageHeader(
            title = stringResource(Res.string.character_list_title),
            compact = compact,
        )

        // "我的角色" 与 "新建角色" 标题行，以及拖动提示
        Column(verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.character_list_drag_tip),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall
                )

                Surface(
                    onClick = {
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


        }

        // 角色卡片列表
        if (currentList.isEmpty() && !state.isLoadingCharacters) {
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
            ) {
                items(
                    items = currentList,
                    key = { it.id }
                ) { character ->
                    val isDragging = character.id == draggingItemId
                    val cardIndex = currentList.indexOfFirst { it.id == character.id }
                    
                    val dragModifier = Modifier.pointerInput(character.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingItemId = character.id
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                
                                val index = currentList.indexOfFirst { it.id == draggingItemId }
                                if (index != -1) {
                                    if (dragOffsetY > thresholdPx && index < currentList.lastIndex) {
                                        val newList = currentList.toMutableList()
                                        val temp = newList[index]
                                        newList[index] = newList[index + 1]
                                        newList[index + 1] = temp
                                        currentList = newList
                                        dragOffsetY -= thresholdPx
                                    } else if (dragOffsetY < -thresholdPx && index > 0) {
                                        val newList = currentList.toMutableList()
                                        val temp = newList[index]
                                        newList[index] = newList[index - 1]
                                        newList[index - 1] = temp
                                        currentList = newList
                                        dragOffsetY += thresholdPx
                                    }
                                }
                            },
                            onDragEnd = {
                                onAction(CharacterAction.CharactersReordered(currentList))
                                draggingItemId = null
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggingItemId = null
                                dragOffsetY = 0f
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                if (isDragging) {
                                    translationY = dragOffsetY
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                    alpha = 0.95f
                                }
                            }
                    ) {
                        SwipeableCharacterCard(
                            character = character,
                            index = cardIndex + 1,
                            compact = compact,
                            onClick = {
                                if (draggingItemId == null) {
                                    onAction(CharacterAction.CharacterSelected(character.id))
                                    onMainAction(MainAction.NavigateTo(Route.CharacterChat(character.id)))
                                }
                            },
                            onEditClick = {
                                if (draggingItemId == null) {
                                    onMainAction(MainAction.NavigateTo(Route.CharacterEdit(character.id)))
                                }
                            },
                            onDeleteClick = {
                                if (character.id.startsWith("builtin:")) {
                                    onAction(CharacterAction.CharacterDeleteBuiltinClicked)
                                } else {
                                    deleteTargetCharacter = character
                                }
                            },
                            dragModifier = dragModifier,
                            isDragging = isDragging,
                            isAnyDragging = draggingItemId != null
                        )
                    }
                }
            }
        }

        if (deleteTargetCharacter != null) {
            val char = deleteTargetCharacter!!
            StarRailDialog(
                title = stringResource(Res.string.character_edit_delete_confirm_title),
                dismissText = stringResource(Res.string.cancel),
                confirmText = stringResource(Res.string.character_edit_delete_confirm_action),
                destructive = true,
                onDismissRequest = { deleteTargetCharacter = null },
                onConfirm = {
                    deleteTargetCharacter = null
                    onAction(CharacterAction.CharacterDeleteClicked(char.id))
                },
            ) {
                Text(
                    text = stringResource(
                        Res.string.character_edit_delete_confirm_message,
                        char.name,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun SwipeableCharacterCard(
    character: Character,
    index: Int,
    compact: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    dragModifier: Modifier,
    isDragging: Boolean,
    isAnyDragging: Boolean,
) {
    val density = LocalDensity.current
    val deleteButtonWidth = 80.dp
    val deleteButtonWidthPx = with(density) { deleteButtonWidth.toPx() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(isAnyDragging) {
        if (isAnyDragging) {
            offsetX.animateTo(0f)
        }
    }

    val progress = if (deleteButtonWidthPx > 0f) (-offsetX.value / deleteButtonWidthPx).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = progress
                }
        ) {
            Surface(
                onClick = {
                    onDeleteClick()
                    scope.launch { offsetX.animateTo(0f) }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(deleteButtonWidth),
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StarRailIcon(
                            kind = StarRailIconKind.DELETE,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "删除",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val draggableState = rememberDraggableState { delta ->
            scope.launch {
                offsetX.snapTo((offsetX.value + delta).coerceIn(-deleteButtonWidthPx, 0f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = !isAnyDragging,
                    onDragStopped = {
                        scope.launch {
                            if (offsetX.value < -deleteButtonWidthPx * 0.5f) {
                                // 触发删除弹窗并让卡片回弹
                                onDeleteClick()
                                offsetX.animateTo(0f)
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    }
                )
                .then(dragModifier)
        ) {
            CharacterCard(
                character = character,
                index = index,
                compact = compact,
                onClick = {
                    if (offsetX.value == 0f) {
                        onClick()
                    } else {
                        scope.launch { offsetX.animateTo(0f) }
                    }
                },
                onEditClick = {
                    if (offsetX.value == 0f) {
                        onEditClick()
                    }
                }
            )
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
                avatarUri = character.avatarUri,
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
                    text = character.lastMessageAt?.let { "上次聊天：${formatLocalTime(it)}" }.orEmpty(),
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
    avatarUri: String,
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
        AvatarImage(
            avatarUri = avatarUri,
            contentDescription = contentDescription,
            placeholderKind = StarRailIconKind.SPARKLE,
            placeholderSize = size * 0.44f,
        )
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

private val previewState = CharactersUiState(
    characters = listOf(
        Character(
            id = "builtin:三月七",
            name = "三月七",
            prompt = "热情开朗，元气满满的少女。",
            openingMessage = "今天想聊点什么呢？",
            avatarUri = ""
        ),
        Character(
            id = "builtin:黄泉",
            name = "黄泉",
            prompt = "神秘优雅，掌控生死的使者。",
            openingMessage = "你好。",
            avatarUri = ""
        ),
        Character(
            id = "builtin:流萤",
            name = "流萤",
            prompt = "温柔坚韧，追寻光明的少女。",
            openingMessage = "你好啊。",
            avatarUri = ""
        ),
        Character(
            id = "builtin:瑕蝶",
            name = "瑕蝶",
            prompt = "梦幻灵动，守护记忆的使者。",
            openingMessage = "欢迎回来。",
            avatarUri = ""
        )
    ),
    isLoadingCharacters = false
)
