package com.kaixuan.starrailchatbox.ui.character.catalog

import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaixuan.starrailchatbox.data.character.catalog.PublicAllCharacters
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCategory
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterSummary
import com.kaixuan.starrailchatbox.data.character.catalog.PublicTag
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageHeader
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.navigation.Route
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.cancel
import starrailchatbox.shared.generated.resources.catalog_admin_category_hint
import starrailchatbox.shared.generated.resources.catalog_admin_category_title
import starrailchatbox.shared.generated.resources.catalog_admin_create
import starrailchatbox.shared.generated.resources.catalog_admin_create_category
import starrailchatbox.shared.generated.resources.catalog_admin_delete
import starrailchatbox.shared.generated.resources.catalog_admin_delete_category
import starrailchatbox.shared.generated.resources.catalog_admin_delete_category_back
import starrailchatbox.shared.generated.resources.catalog_admin_delete_category_confirm
import starrailchatbox.shared.generated.resources.catalog_admin_delete_category_empty
import starrailchatbox.shared.generated.resources.catalog_admin_delete_category_replacement
import starrailchatbox.shared.generated.resources.catalog_admin_delete_category_select
import starrailchatbox.shared.generated.resources.catalog_admin_delete_category_selected
import starrailchatbox.shared.generated.resources.catalog_admin_delete_category_title
import starrailchatbox.shared.generated.resources.catalog_admin_delete_message
import starrailchatbox.shared.generated.resources.catalog_admin_delete_title
import starrailchatbox.shared.generated.resources.catalog_admin_disable
import starrailchatbox.shared.generated.resources.catalog_admin_key_hint
import starrailchatbox.shared.generated.resources.catalog_admin_key_title
import starrailchatbox.shared.generated.resources.catalog_admin_move
import starrailchatbox.shared.generated.resources.catalog_admin_move_title
import starrailchatbox.shared.generated.resources.catalog_admin_pending_review
import starrailchatbox.shared.generated.resources.catalog_admin_refresh_taxonomy
import starrailchatbox.shared.generated.resources.catalog_admin_submit_review
import starrailchatbox.shared.generated.resources.catalog_admin_verify
import starrailchatbox.shared.generated.resources.catalog_all_characters

@Composable
fun CharacterCatalogRoute(
    model: CharacterCatalogViewModel,
    isActive: Boolean,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    val state by model.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isActive) {
        if (isActive) {
            model.onAction(CharacterCatalogAction.LoadCatalog)
        }
    }

    LaunchedEffect(model.effects) {
        model.effects.collect { effect ->
            when (effect) {
                is CharacterCatalogEffect.ShowToast -> {
                     snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    CharacterCatalogScreen(
        state = state,
        contentPadding = contentPadding,
        compact = compact,
        onMainAction = onMainAction,
        onAction = model::onAction,
        resolveUrl = { url -> model.resolveUrl(url) },
        modifier = modifier,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CharacterCatalogScreen(
    state: CharacterCatalogUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onAction: (CharacterCatalogAction) -> Unit,
    resolveUrl: (String) -> String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    val tabs = remember(state.allCharacters, state.categories) {
        val list = mutableListOf<CategoryTab>()
        state.allCharacters?.let {
            list.add(CategoryTab(null, state.allCharacters.name, it.firstPageUrl))
        }
        state.categories.filter { it.characterCount > 0 }.forEach { cat ->
            list.add(CategoryTab(cat.id, cat.name, cat.firstPageUrl))
        }
        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                )
            ),
    ) {
        if (tabs.isEmpty() && state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val initialCategoryIndex = catalogCategoryIndex(
                tabs = tabs,
                selectedCategoryId = state.selectedCategoryId,
            )
            val pagerState = rememberPagerState(
                initialPage = initialCategoryIndex,
                pageCount = { tabs.size }
            )
            val categoryListState = rememberLazyListState(
                initialFirstVisibleItemIndex = initialCategoryIndex,
            )
            val selectedCategoryId by rememberUpdatedState(state.selectedCategoryId)

            // 监听 selectedCategoryId 的变化，并自动滚动分类列表和 Pager
            LaunchedEffect(state.selectedCategoryId, tabs) {
                if (tabs.isNotEmpty()) {
                    val targetIndex = catalogCategoryIndex(
                        tabs = tabs,
                        selectedCategoryId = state.selectedCategoryId,
                    )
                    if (pagerState.currentPage != targetIndex) {
                        pagerState.animateScrollToPage(targetIndex)
                    }
                }
            }

            // 视觉反馈跟随当前页立即更新，业务选中状态仍等 Pager 停稳后再提交。
            LaunchedEffect(pagerState, tabs) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    if (page in tabs.indices) {
                        categoryListState.scrollToItem(page)
                    }
                }
            }

            // Pager 停稳后再同步分类，避免跨页动画经过中间页时反向打断目标切换。
            LaunchedEffect(pagerState, tabs) {
                snapshotFlow {
                    CatalogPagerSyncSnapshot(
                        settledPage = pagerState.settledPage,
                        isScrollInProgress = pagerState.isScrollInProgress,
                    )
                }.collect { snapshot ->
                    catalogPageSelectionAction(
                        tabs = tabs,
                        snapshot = snapshot,
                        selectedCategoryId = selectedCategoryId,
                    )?.let { action ->
                        onAction(action)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + StarRailSpacing.xs,
                        end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                    ),
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
            ) {
                StarRailPageHeader(
                    title = "角色工坊",
                    compact = compact,
                    backContentDescription = if (onBackClick != null) "返回" else null,
                    onBackClick = onBackClick,
                    onTitleLongClick = { onAction(CharacterCatalogAction.TitleClicked) },
                )

                if (state.adminModeEnabled) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                        maxItemsInEachRow = if (compact) 2 else 4,
                    ) {
                        Button(
                            onClick = { onAction(CharacterCatalogAction.CreateCategoryClicked) },
                            enabled = !state.isAdminBusy && !state.isLoading,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(Res.string.catalog_admin_create_category))
                        }
                        OutlinedButton(
                            onClick = { onAction(CharacterCatalogAction.DeleteCategoryClicked) },
                            enabled = !state.isAdminBusy && !state.isLoading,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = StarRailSpacing.xs),
                        ) {
                            Text(stringResource(Res.string.catalog_admin_delete_category))
                        }
                        OutlinedButton(
                            onClick = { onAction(CharacterCatalogAction.RefreshTaxonomy) },
                            enabled = !state.isAdminBusy && !state.isLoading,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = StarRailSpacing.xs),
                        ) {
                            Text(stringResource(Res.string.catalog_admin_refresh_taxonomy))
                        }
                        OutlinedButton(
                            onClick = { onAction(CharacterCatalogAction.DisableAdminMode) },
                            enabled = !state.isAdminBusy && !state.isLoading,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = StarRailSpacing.xs),
                        ) {
                            Text(stringResource(Res.string.catalog_admin_disable))
                        }
                    }
                }

                // 1. 分类筛选横滑栏和标签筛选按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 品类横向滑动列表
                    androidx.compose.foundation.lazy.LazyRow(
                        state = categoryListState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
                    ) {
                        items(tabs.size) { index ->
                            val tab = tabs[index]
                            val isSelected = index == pagerState.currentPage
                            CategoryBadge(
                                name = tab.name,
                                isSelected = isSelected,
                            ) {
                                onAction(if (tab.id == null) CharacterCatalogAction.SelectAll else CharacterCatalogAction.SelectCategory(tab.id))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(StarRailSpacing.sm))

                    // 漏斗过滤按钮
                    Surface(
                        onClick = { onAction(CharacterCatalogAction.ToggleTagFilter) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (state.selectedTagIds.isNotEmpty()) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (state.selectedTagIds.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            StarRailIcon(
                                kind = StarRailIconKind.FILTER,
                                contentDescription = "过滤标签",
                                tint = if (state.selectedTagIds.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onAction(CharacterCatalogAction.RefreshCatalog) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        if (pageIndex >= tabs.size) return@HorizontalPager
                        val tab = tabs[pageIndex]
                        val catState = state.categoryStates[tab.id] ?: CategoryState(tab.id, tab.firstPageUrl)

                        val pageListState = rememberLazyListState()

                        // 监听滑动到底部触发加载该 Category 的下一页
                        val shouldLoadMore = remember {
                            derivedStateOf {
                                val layoutInfo = pageListState.layoutInfo
                                val totalItemsNumber = layoutInfo.totalItemsCount
                                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                totalItemsNumber > 0 && lastVisibleItemIndex >= (totalItemsNumber - 5)
                            }
                        }

                        LaunchedEffect(shouldLoadMore.value) {
                            if (shouldLoadMore.value && !catState.isPageLoading && catState.page < catState.totalPages) {
                                onAction(CharacterCatalogAction.LoadNextPage)
                            }
                        }

                        // 预加载触发
                        LaunchedEffect(tab.id) {
                            if (catState.characters.isEmpty() && !catState.isPageLoading) {
                                onAction(CharacterCatalogAction.PreloadCategory(tab.id, tab.firstPageUrl))
                            }
                        }

                        LazyColumn(
                            state = pageListState,
                            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (catState.filteredCharacters.isEmpty()) {
                                if (state.isLoading && catState.characters.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(200.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                } else {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(200.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "没有找到符合条件的角色",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = catState.filteredCharacters,
                                    key = { it.id }
                                ) { char ->
                                    SwipeableCharacterCatalogItem(
                                        adminModeEnabled = state.adminModeEnabled,
                                        deletePending = state.pendingDeleteCharacterKeys.contains(char.characterKey),
                                        onMove = {
                                            onAction(CharacterCatalogAction.MoveCharacterClicked(char))
                                        },
                                        onDelete = {
                                            onAction(CharacterCatalogAction.DeleteCharacterClicked(char))
                                        },
                                    ) {
                                        CharacterCatalogItem(
                                            char = char,
                                            tags = state.tags,
                                            isImporting = state.importingCharacterIds.contains(char.id),
                                            isImported = state.importedCharacterIds.contains(char.id),
                                            resolveUrl = resolveUrl,
                                            onImportClick = {
                                                onAction(CharacterCatalogAction.ImportCharacterClicked(char))
                                            },
                                            onItemClick = {
                                                onMainAction(
                                                    MainAction.NavigateTo(
                                                        Route.CharacterCatalogDetail(
                                                            characterId = char.id,
                                                            detailUrl = char.detailUrl,
                                                            name = char.name,
                                                            avatarUrl = char.avatarUrl,
                                                        )
                                                    )
                                                )
                                            },
                                        )
                                    }
                                }

                                if (catState.isPageLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = StarRailSpacing.md),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 悬浮在右上侧的标签筛选面板
        androidx.compose.animation.AnimatedVisibility(
            visible = state.isTagFilterOpen,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 100.dp, end = StarRailSpacing.md)
                .zIndex(10f)
        ) {
            TagFilterPanel(
                tags = state.tags,
                selectedTagIds = state.selectedTagIds,
                onTagToggle = { onAction(CharacterCatalogAction.ToggleTag(it)) },
                onClearClick = { onAction(CharacterCatalogAction.ClearTags) },
                onCloseClick = { onAction(CharacterCatalogAction.ToggleTagFilter) }
            )
        }

        if (state.showAdminKeyDialog) {
            StarRailDialog(
                title = stringResource(Res.string.catalog_admin_key_title),
                confirmText = stringResource(Res.string.catalog_admin_verify),
                dismissText = stringResource(Res.string.cancel),
                onConfirm = { onAction(CharacterCatalogAction.ConfirmAdminKey) },
                onDismissRequest = {
                    onAction(CharacterCatalogAction.DismissAdminKeyDialog)
                },
            ) {
                OutlinedTextField(
                    value = state.adminKeyDraft,
                    onValueChange = {
                        onAction(CharacterCatalogAction.AdminKeyChanged(it))
                    },
                    label = { Text(stringResource(Res.string.catalog_admin_key_hint)) },
                    singleLine = true,
                    enabled = !state.isAdminBusy,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.showCreateCategoryDialog) {
            StarRailDialog(
                title = stringResource(Res.string.catalog_admin_category_title),
                confirmText = stringResource(Res.string.catalog_admin_create),
                dismissText = stringResource(Res.string.cancel),
                onConfirm = { onAction(CharacterCatalogAction.ConfirmCreateCategory) },
                onDismissRequest = {
                    onAction(CharacterCatalogAction.DismissCreateCategoryDialog)
                },
            ) {
                OutlinedTextField(
                    value = state.categoryNameDraft,
                    onValueChange = {
                        onAction(CharacterCatalogAction.CategoryNameChanged(it))
                    },
                    label = { Text(stringResource(Res.string.catalog_admin_category_hint)) },
                    singleLine = true,
                    enabled = !state.isAdminBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.showDeleteCategoryDialog) {
            val deletingCategory = state.dynamicCategories.firstOrNull {
                it.id == state.deletingCategoryId
            }
            val canConfirmDelete =
                deletingCategory != null && state.replacementCategoryId != null
            StarRailDialog(
                title = stringResource(Res.string.catalog_admin_delete_category_title),
                confirmText = if (canConfirmDelete) {
                    stringResource(Res.string.catalog_admin_delete_category_confirm)
                } else {
                    null
                },
                dismissText = stringResource(Res.string.cancel),
                neutralText = if (deletingCategory != null) {
                    stringResource(Res.string.catalog_admin_delete_category_back)
                } else {
                    null
                },
                destructive = true,
                onConfirm = if (canConfirmDelete) {
                    { onAction(CharacterCatalogAction.ConfirmDeleteCategory) }
                } else {
                    null
                },
                onNeutral = if (deletingCategory != null) {
                    { onAction(CharacterCatalogAction.DeleteCategorySelectionCleared) }
                } else {
                    null
                },
                onDismissRequest = {
                    onAction(CharacterCatalogAction.DismissDeleteCategoryDialog)
                },
            ) {
                if (deletingCategory == null) {
                    Text(
                        text = stringResource(
                            if (state.dynamicCategories.isEmpty()) {
                                Res.string.catalog_admin_delete_category_empty
                            } else {
                                Res.string.catalog_admin_delete_category_select
                            },
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (state.dynamicCategories.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                        ) {
                            items(
                                items = state.dynamicCategories,
                                key = { it.id },
                            ) { category ->
                                OutlinedButton(
                                    onClick = {
                                        onAction(
                                            CharacterCatalogAction.DeleteCategorySelected(
                                                category.id,
                                            ),
                                        )
                                    },
                                    enabled = !state.isAdminBusy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) {
                                    Text(
                                        text = category.name,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(
                            Res.string.catalog_admin_delete_category_selected,
                            deletingCategory.name,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(
                            Res.string.catalog_admin_delete_category_replacement,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                    ) {
                        items(
                            items = state.categories.filter { it.id != deletingCategory.id },
                            key = { it.id },
                        ) { category ->
                            val selected = category.id == state.replacementCategoryId
                            if (selected) {
                                Button(
                                    onClick = {
                                        onAction(
                                            CharacterCatalogAction.DeleteCategoryReplacementSelected(
                                                category.id,
                                            ),
                                        )
                                    },
                                    enabled = !state.isAdminBusy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) {
                                    Text(category.name)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        onAction(
                                            CharacterCatalogAction.DeleteCategoryReplacementSelected(
                                                category.id,
                                            ),
                                        )
                                    },
                                    enabled = !state.isAdminBusy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) {
                                    Text(category.name)
                                }
                            }
                        }
                    }
                }
            }
        }

        state.movingCharacter?.let { character ->
            StarRailDialog(
                title = stringResource(Res.string.catalog_admin_move_title),
                dismissText = stringResource(Res.string.cancel),
                onDismissRequest = {
                    onAction(CharacterCatalogAction.DismissMoveCharacterDialog)
                },
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                state.categories
                    .filter { it.id != character.primaryCategoryId }
                    .forEach { category ->
                        OutlinedButton(
                            onClick = {
                                onAction(
                                    CharacterCatalogAction.ConfirmMoveCharacter(category.id),
                                )
                            },
                            enabled = !state.isAdminBusy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(category.name)
                        }
                    }
            }
        }

        state.deletingCharacter?.let { character ->
            StarRailDialog(
                title = stringResource(Res.string.catalog_admin_delete_title),
                confirmText = stringResource(Res.string.catalog_admin_submit_review),
                dismissText = stringResource(Res.string.cancel),
                destructive = true,
                onConfirm = { onAction(CharacterCatalogAction.ConfirmDeleteCharacter) },
                onDismissRequest = {
                    onAction(CharacterCatalogAction.DismissDeleteCharacterDialog)
                },
            ) {
                Text(
                    text = stringResource(
                        Res.string.catalog_admin_delete_message,
                        character.name,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarRailIcon(
                kind = StarRailIconKind.COMPASS,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "搜索角色或作者",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.CLOSE,
                        contentDescription = "清除搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        },
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Text(
            text = name,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CharacterCatalogItem(
    char: PublicCharacterSummary,
    tags: List<PublicTag>,
    isImporting: Boolean,
    isImported: Boolean,
    resolveUrl: (String) -> String,
    onImportClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 伪热度计算，根据 ID 哈希生成
    val hotValue = remember(char.id) {
        val hash = char.id.hashCode().absoluteValue
        val base = (hash % 80 + 20) / 10.0
        "${base}k"
    }

    Surface(
        onClick = onItemClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StarRailSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // 头像，带精美科幻渐变环
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.starRailColors.avatarRingStart.copy(alpha = 0.8f),
                                    MaterialTheme.starRailColors.avatarRingEnd.copy(alpha = 0.8f),
                                )
                            )
                        )
                        .padding(2.5.dp)
                ) {
                    AvatarImage(
                        avatarUri = char.avatarUrl?.let { resolveUrl(it) }.orEmpty(),
                        contentDescription = char.name,
                        placeholderKind = StarRailIconKind.PROFILE,
                        placeholderSize = 36.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(StarRailSpacing.md))

                // 文本信息列
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = char.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee()
                        )

                        Spacer(modifier = Modifier.width(StarRailSpacing.sm))

                        // 右上角热度
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StarRailIcon(
                                kind = StarRailIconKind.SPARKLE,
                                contentDescription = null,
                                tint = Color(0xFFFF8F00),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = hotValue,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8F00)
                            )
                        }
                    }

                    // 描述
                    Text(
                        text = char.description.takeIf { it.isNotBlank() } ?: "暂无角色背景描述说明。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )

                    CharacterTagChips(
                        tagNames = resolveCharacterTagNames(char.tagIds, tags),
                    )

                    // 作者
                    Text(
                        text = "作者：${char.author.takeIf { it.isNotBlank() } ?: "星轨旅人"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // 右下角导入动作按钮
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp
                    )
                } else if (isImported) {
                    StarRailIcon(
                        kind = StarRailIconKind.CHECK,
                        contentDescription = "已导入",
                        tint = MaterialTheme.starRailColors.successCheck,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Surface(
                        onClick = onImportClick,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            StarRailIcon(
                                kind = StarRailIconKind.ADD,
                                contentDescription = "导入角色",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun SwipeableCharacterCatalogItem(
    adminModeEnabled: Boolean,
    deletePending: Boolean,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 88.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(adminModeEnabled) {
        if (!adminModeEnabled) {
            offsetX.animateTo(0f)
        }
    }

    val progress = if (actionWidthPx > 0f) {
        (offsetX.value.absoluteValue / actionWidthPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = progress },
        ) {
            CatalogSwipeAction(
                text = stringResource(Res.string.catalog_admin_move),
                icon = StarRailIconKind.EDIT,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    onMove()
                    scope.launch { offsetX.animateTo(0f) }
                },
                modifier = Modifier.align(Alignment.CenterStart).width(actionWidth),
            )
            CatalogSwipeAction(
                text = stringResource(
                    if (deletePending) {
                        Res.string.catalog_admin_pending_review
                    } else {
                        Res.string.catalog_admin_delete
                    },
                ),
                icon = StarRailIconKind.DELETE,
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                enabled = !deletePending,
                onClick = {
                    onDelete()
                    scope.launch { offsetX.animateTo(0f) }
                },
                modifier = Modifier.align(Alignment.CenterEnd).width(actionWidth),
            )
        }

        val draggableState = rememberDraggableState { delta ->
            scope.launch {
                offsetX.snapTo(
                    (offsetX.value + delta).coerceIn(-actionWidthPx, actionWidthPx),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = adminModeEnabled,
                    onDragStopped = {
                        scope.launch {
                            when {
                                offsetX.value > actionWidthPx * 0.5f -> onMove()
                                offsetX.value < -actionWidthPx * 0.5f && !deletePending -> onDelete()
                            }
                            offsetX.animateTo(0f)
                        }
                    },
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun CatalogSwipeAction(
    text: String,
    icon: StarRailIconKind,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = color,
        contentColor = contentColor,
        modifier = modifier.fillMaxHeight(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            StarRailIcon(
                kind = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterPanel(
    tags: List<PublicTag>,
    selectedTagIds: Set<String>,
    onTagToggle: (String) -> Unit,
    onClearClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(StarRailSpacing.md),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "标签筛选",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.CLOSE,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Tags 列表
            if (tags.isEmpty()) {
                Text(
                    text = "没有可筛选的标签",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        val isSelected = selectedTagIds.contains(tag.id)
                        SelectableCharacterTagChip(
                            name = tag.name,
                            selected = isSelected,
                            onClick = { onTagToggle(tag.id) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 底部控制
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onClearClick()
                    }
                ) {
                    Text(
                        text = "重置",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(StarRailSpacing.sm))
                Button(
                    onClick = onCloseClick,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "完成",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterCatalogAdminLightPreview() {
    CharacterCatalogAdminPreview(darkTheme = false)
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterCatalogAdminDarkPreview() {
    CharacterCatalogAdminPreview(darkTheme = true)
}

@Composable
private fun CharacterCatalogAdminPreview(darkTheme: Boolean) {
    val category = PublicCategory(
        id = "general",
        name = "综合",
        sortOrder = 1,
        characterCount = 1,
        firstPageUrl = "/page1.json",
    )
    val dynamicCategory = PublicCategory(
        id = "cat_123456789abc",
        name = "自定义分类",
        sortOrder = 2,
        characterCount = 0,
        firstPageUrl = "/dynamic/page1.json",
    )
    val character = PublicCharacterSummary(
        characterKey = "a".repeat(64),
        id = "preview",
        name = "流萤",
        author = "星轨旅人",
        description = "用于预览管理员操作按钮的公共角色。",
        primaryCategoryId = "general",
        tagIds = listOf("gentle", "healing"),
        updatedAt = "2026-06-14T00:00:00Z",
        revision = "r_preview",
        detailUrl = "/detail.json",
    )
    StarRailTheme(darkThemeOverride = darkTheme) {
        CharacterCatalogScreen(
            state = CharacterCatalogUiState(
                allCharacters = PublicAllCharacters(
                    name = "全部",
                    characterCount = 1,
                    firstPageUrl = "/all/page1.json",
                ),
                categories = listOf(category, dynamicCategory),
                tags = listOf(
                    PublicTag("gentle", "温柔", 1, "/tags/gentle/page1.json"),
                    PublicTag("healing", "治愈", 2, "/tags/healing/page1.json"),
                ),
                selectedCategoryId = null,
                activeFirstPageUrl = "/all/page1.json",
                characters = listOf(character),
                filteredCharacters = listOf(character),
                adminSupported = true,
                adminModeEnabled = true,
                showDeleteCategoryDialog = true,
            ),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {},
            resolveUrl = { it },
        )
    }
}

data class CategoryTab(
    val id: String?,
    val name: String,
    val firstPageUrl: String
)

internal data class CatalogPagerSyncSnapshot(
    val settledPage: Int,
    val isScrollInProgress: Boolean,
)

internal fun catalogCategoryIndex(
    tabs: List<CategoryTab>,
    selectedCategoryId: String?,
): Int = tabs.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0)

internal fun catalogPageSelectionAction(
    tabs: List<CategoryTab>,
    snapshot: CatalogPagerSyncSnapshot,
    selectedCategoryId: String?,
): CharacterCatalogAction? {
    if (snapshot.isScrollInProgress || snapshot.settledPage !in tabs.indices) {
        return null
    }

    val categoryId = tabs[snapshot.settledPage].id
    if (categoryId == selectedCategoryId) {
        return null
    }

    return if (categoryId == null) {
        CharacterCatalogAction.SelectAll
    } else {
        CharacterCatalogAction.SelectCategory(categoryId)
    }
}
