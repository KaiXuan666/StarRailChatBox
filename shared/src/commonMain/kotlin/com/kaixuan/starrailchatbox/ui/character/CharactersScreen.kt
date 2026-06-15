package com.kaixuan.starrailchatbox.ui.character

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextAlign
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCategory
import com.kaixuan.starrailchatbox.data.character.catalog.PublicTag
import com.kaixuan.starrailchatbox.data.character.sharing.ShareCategorySelection
import com.kaixuan.starrailchatbox.ui.character.catalog.SelectableCharacterTagChip
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.platform.formatLastChatTime
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import com.kaixuan.starrailchatbox.ui.components.StarRailDialogButton
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageHeader
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.navigation.Route
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.cancel
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_action
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_message
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_title
import starrailchatbox.shared.generated.resources.character_edit_export
import starrailchatbox.shared.generated.resources.character_export_dialog_message
import starrailchatbox.shared.generated.resources.character_export_dialog_title
import starrailchatbox.shared.generated.resources.character_export_local
import starrailchatbox.shared.generated.resources.character_list_create_btn
import starrailchatbox.shared.generated.resources.character_list_drag_tip
import starrailchatbox.shared.generated.resources.character_list_edit_desc
import starrailchatbox.shared.generated.resources.character_list_empty
import starrailchatbox.shared.generated.resources.character_list_help_card_desc
import starrailchatbox.shared.generated.resources.character_list_help_export_desc
import starrailchatbox.shared.generated.resources.character_list_help_export_title
import starrailchatbox.shared.generated.resources.character_list_help_import_desc
import starrailchatbox.shared.generated.resources.character_list_help_import_title
import starrailchatbox.shared.generated.resources.character_list_help_title
import starrailchatbox.shared.generated.resources.character_list_restore_default
import starrailchatbox.shared.generated.resources.character_list_help_what_is_card
import starrailchatbox.shared.generated.resources.character_list_title
import starrailchatbox.shared.generated.resources.character_share_public
import starrailchatbox.shared.generated.resources.character_share_category_confirm
import starrailchatbox.shared.generated.resources.character_share_category_create
import starrailchatbox.shared.generated.resources.character_share_category_label
import starrailchatbox.shared.generated.resources.character_share_category_loading
import starrailchatbox.shared.generated.resources.character_share_category_message
import starrailchatbox.shared.generated.resources.character_share_category_name_error
import starrailchatbox.shared.generated.resources.character_share_category_name_label
import starrailchatbox.shared.generated.resources.character_share_category_name_placeholder
import starrailchatbox.shared.generated.resources.character_share_category_placeholder
import starrailchatbox.shared.generated.resources.character_share_category_title
import starrailchatbox.shared.generated.resources.character_share_public_sharing
import starrailchatbox.shared.generated.resources.character_share_public_hint
import starrailchatbox.shared.generated.resources.character_share_tag_empty
import starrailchatbox.shared.generated.resources.character_share_tag_label
import starrailchatbox.shared.generated.resources.confirm
import kotlin.math.roundToInt


/**
 * 角色列表界面，展示所有已创建或导入的角色，支持排序、删除和导出操作。
 */
@OptIn(ExperimentalLayoutApi::class)
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

    val sortedCharacters = state.characters

    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var currentList by remember(sortedCharacters) { mutableStateOf(sortedCharacters) }
    var deleteTargetCharacter by remember { mutableStateOf<CharacterSummary?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

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
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + StarRailSpacing.xs,
                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
            ),
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
    ) {
        StarRailPageHeader(
            title = stringResource(Res.string.character_list_title),
            compact = compact,
            modifier = Modifier.fillMaxWidth()
        )

        // "我的角色" 与 "新建角色" 标题行，以及拖动提示
        Column(verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                // 辅助操作：导入与帮助
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
                ) {
                    Surface(
                        onClick = { showHelpDialog = true },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "帮助",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            StarRailIcon(
                                kind = StarRailIconKind.INFO,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            onAction(CharacterAction.CharacterImportClicked)
                        },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "导入",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            StarRailIcon(
                                kind = StarRailIconKind.FILE,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(StarRailSpacing.xs))

                // 主要操作：新建角色
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

            Text(
                text = stringResource(Res.string.character_list_drag_tip),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall
            )
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
                                deleteTargetCharacter = character
                            },
                            onExportClick = {
                                if (draggingItemId == null) {
                                    onAction(CharacterAction.CharacterExportClicked(character.id))
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

        if (state.exportDialogCharacterId != null) {
            val isSharing = state.sharingCharacterId == state.exportDialogCharacterId
            StarRailDialog(
                title = stringResource(Res.string.character_export_dialog_title),
                onDismissRequest = {
                    onAction(CharacterAction.CharacterExportDialogDismissed)
                },
            ) {
                Text(
                    text = stringResource(Res.string.character_export_dialog_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StarRailDialogButton(
                        text = stringResource(Res.string.character_export_local),
                        onClick = {
                            onAction(CharacterAction.CharacterExportLocalClicked)
                        },
                        primary = true,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StarRailDialogButton(
                        text = stringResource(
                            if (isSharing) {
                                Res.string.character_share_public_sharing
                            } else {
                                Res.string.character_share_public
                            },
                        ),
                        onClick = {
                            if (!isSharing) {
                                onAction(CharacterAction.CharacterSharePublicClicked)
                            }
                        },
                        primary = true,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                    )
                    Text(
                        text = stringResource(Res.string.character_share_public_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }

        if (state.shareCategoryDialogCharacterId != null) {
            val isSharing = state.sharingCharacterId == state.shareCategoryDialogCharacterId
            val canConfirm = state.canConfirmShareCategory &&
                !state.isLoadingShareCategories &&
                !isSharing
            StarRailDialog(
                title = stringResource(Res.string.character_share_category_title),
                dismissText = if (isSharing || state.isLoadingShareCategories) {
                    null
                } else {
                    stringResource(Res.string.cancel)
                },
                confirmText = if (canConfirm) {
                    stringResource(Res.string.character_share_category_confirm)
                } else {
                    null
                },
                onDismissRequest = {
                    onAction(CharacterAction.CharacterShareCategoryDialogDismissed)
                },
                onConfirm = {
                    onAction(CharacterAction.CharacterShareCategoryConfirmed)
                },
            ) {
                Text(
                    text = stringResource(Res.string.character_share_category_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.isLoadingShareCategories || isSharing) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = StarRailSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Text(
                            text = stringResource(
                                if (isSharing) {
                                    Res.string.character_share_public_sharing
                                } else {
                                    Res.string.character_share_category_loading
                                },
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    var categoryMenuExpanded by remember { mutableStateOf(false) }
                    val categorySelection = state.shareCategorySelection
                    val selectedCategory = (categorySelection as? ShareCategorySelection.Existing)
                        ?.let { selected -> state.shareCategories.firstOrNull { it.id == selected.id } }
                    val isCustomCategory = categorySelection is ShareCategorySelection.Proposed
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                    ) {
                        Text(
                            text = stringResource(Res.string.character_share_category_label),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { categoryMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedCategory != null || isCustomCategory) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = StarRailSpacing.md, vertical = StarRailSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = when {
                                            selectedCategory != null -> selectedCategory.name
                                            isCustomCategory ->
                                                stringResource(Res.string.character_share_category_create)
                                            else ->
                                                stringResource(Res.string.character_share_category_placeholder)
                                        },
                                        color = if (selectedCategory == null && !isCustomCategory) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    StarRailIcon(
                                        kind = StarRailIconKind.CHEVRON_RIGHT,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                rotationZ = if (categoryMenuExpanded) -90f else 90f
                                            },
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(
                                                Res.string.character_share_category_create,
                                            ),
                                            color = if (isCustomCategory) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                    },
                                    onClick = {
                                        categoryMenuExpanded = false
                                        onAction(
                                            CharacterAction.CharacterShareCustomCategorySelected,
                                        )
                                    },
                                    trailingIcon = if (isCustomCategory) {
                                        {
                                            StarRailIcon(
                                                kind = StarRailIconKind.CHECK,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                )
                                state.shareCategories.forEach { category ->
                                    val selected =
                                        (categorySelection as? ShareCategorySelection.Existing)
                                            ?.id == category.id
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = category.name,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            )
                                        },
                                        onClick = {
                                            categoryMenuExpanded = false
                                            onAction(
                                                CharacterAction.CharacterShareCategorySelected(category.id),
                                            )
                                        },
                                        trailingIcon = if (selected) {
                                            {
                                                StarRailIcon(
                                                    kind = StarRailIconKind.CHECK,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                    )
                                }
                            }
                        }

                        if (categorySelection is ShareCategorySelection.Proposed) {
                            val validName = isValidProposedCategoryName(categorySelection.name)
                            OutlinedTextField(
                                value = categorySelection.name,
                                onValueChange = {
                                    onAction(
                                        CharacterAction.CharacterShareCustomCategoryNameChanged(it),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        stringResource(
                                            Res.string.character_share_category_name_label,
                                        ),
                                    )
                                },
                                placeholder = {
                                    Text(
                                        stringResource(
                                            Res.string.character_share_category_name_placeholder,
                                        ),
                                    )
                                },
                                supportingText = if (
                                    categorySelection.name.isNotEmpty() && !validName
                                ) {
                                    {
                                        Text(
                                            stringResource(
                                                Res.string.character_share_category_name_error,
                                            ),
                                        )
                                    }
                                } else {
                                    null
                                },
                                isError = categorySelection.name.isNotEmpty() && !validName,
                                singleLine = true,
                            )
                        }

                        Text(
                            text = stringResource(Res.string.character_share_tag_label),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (state.shareTags.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.character_share_tag_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xxs),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                state.shareTags.forEach { tag ->
                                    SelectableCharacterTagChip(
                                        name = tag.name,
                                        selected = tag.id in state.selectedShareTagIds,
                                        onClick = {
                                            onAction(CharacterAction.CharacterShareTagToggled(tag.id))
                                        },
                                    )
                                }
                            }
                        }
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

        if (showHelpDialog) {
            StarRailDialog(
                title = stringResource(Res.string.character_list_help_title),
                confirmText = stringResource(Res.string.confirm),
                neutralText = stringResource(Res.string.character_list_restore_default),
                onDismissRequest = { showHelpDialog = false },
                onConfirm = { showHelpDialog = false },
                onNeutral = {
                    showHelpDialog = false
                    onAction(CharacterAction.RestoreBuiltinCharactersClicked)
                },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
                    modifier = Modifier.padding(bottom = StarRailSpacing.sm)
                ) {
                    HelpSection(
                        title = stringResource(Res.string.character_list_help_what_is_card),
                        description = stringResource(Res.string.character_list_help_card_desc)
                    )
                    HelpSection(
                        title = stringResource(Res.string.character_list_help_import_title),
                        description = stringResource(Res.string.character_list_help_import_desc)
                    )
                    HelpSection(
                        title = stringResource(Res.string.character_list_help_export_title),
                        description = stringResource(Res.string.character_list_help_export_desc),
                        boldPart = "以文件形式发送，或发送图片时勾选原图"
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    description: String,
    boldPart: String? = null,
) {
    val descriptionAnnotated = remember(description, boldPart) {
        if (boldPart != null && description.contains(boldPart)) {
            val startIndex = description.indexOf(boldPart)
            buildAnnotatedString {
                append(description.substring(0, startIndex))
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(boldPart)
                }
                append(description.substring(startIndex + boldPart.length))
            }
        } else {
            AnnotatedString(description)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = descriptionAnnotated,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwipeableCharacterCard(
    character: CharacterSummary,
    index: Int,
    compact: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit,
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
                },
                onExportClick = {
                    if (offsetX.value == 0f) {
                        onExportClick()
                    }
                }
            )
        }
    }
}

@Composable
private fun CharacterCard(
    character: CharacterSummary,
    index: Int,
    compact: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onExportClick: () -> Unit,
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
            modifier = Modifier.padding(
                start = StarRailSpacing.sm,
                end = StarRailSpacing.md,
                top = StarRailSpacing.sm,
                bottom = StarRailSpacing.sm
            ),
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

            Spacer(modifier = Modifier.width(StarRailSpacing.sm))

            // 圆形头像
            CharacterListAvatar(
                avatarUri = character.avatarUri,
                contentDescription = null,
                size = if (compact) 56.dp else 64.dp
            )

            Spacer(modifier = Modifier.width(StarRailSpacing.md))

            // 中间文字描述（占据剩余可用空间）
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = character.name,
                    modifier = Modifier.basicMarquee(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                character.lastMessageAt?.let { lastMessageAt ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StarRailIcon(
                            kind = StarRailIconKind.CLOCK,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = formatLastChatTime(lastMessageAt),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(StarRailSpacing.sm))

            // 右侧操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 导出按钮
                Surface(
                    onClick = onExportClick,
                    modifier = Modifier.size(if (compact) 36.dp else 40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        StarRailIcon(
                            kind = StarRailIconKind.EXPORT,
                            contentDescription = stringResource(Res.string.character_edit_export),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                        )
                    }
                }

                // 编辑按钮
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

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterExportDialogLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        CharactersScreen(
            state = previewState.copy(exportDialogCharacterId = "builtin:三月七"),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterExportDialogDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        CharactersScreen(
            state = previewState.copy(exportDialogCharacterId = "builtin:三月七"),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterShareCategoryDialogLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        CharactersScreen(
            state = previewState.copy(
                shareCategoryDialogCharacterId = "role",
                shareCategories = previewCategories,
                shareTags = previewTags,
                shareCategorySelection = ShareCategorySelection.Existing("game"),
                selectedShareTagIds = setOf("gentle", "healing"),
            ),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterShareCategoryDialogDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        CharactersScreen(
            state = previewState.copy(
                shareCategoryDialogCharacterId = "role",
                shareCategories = previewCategories,
                shareTags = previewTags,
                shareCategorySelection = ShareCategorySelection.Existing("game"),
                selectedShareTagIds = setOf("gentle", "healing"),
            ),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {},
        )
    }
}

private val previewCategories = listOf(
    PublicCategory(
        id = "game",
        name = "游戏角色",
        sortOrder = 1,
        characterCount = 12,
        firstPageUrl = "",
    ),
    PublicCategory(
        id = "original",
        name = "原创角色",
        sortOrder = 2,
        characterCount = 8,
        firstPageUrl = "",
    ),
)

private val previewTags = listOf(
    PublicTag(id = "gentle", name = "温柔", sortOrder = 1, firstPageUrl = ""),
    PublicTag(id = "tsundere", name = "傲娇", sortOrder = 2, firstPageUrl = ""),
    PublicTag(id = "healing", name = "治愈", sortOrder = 3, firstPageUrl = ""),
)

private val previewState = CharactersUiState(
    characters = listOf(
        CharacterSummary(
            id = "builtin:三月七",
            name = "三月七",
            avatarUri = ""
        ),
        CharacterSummary(
            id = "builtin:黄泉",
            name = "黄泉",
            avatarUri = ""
        ),
        CharacterSummary(
            id = "builtin:流萤",
            name = "流萤",
            avatarUri = ""
        ),
        CharacterSummary(
            id = "builtin:瑕蝶",
            name = "瑕蝶",
            avatarUri = ""
        )
    ),
    isLoadingCharacters = false
)
