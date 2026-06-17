package com.kaixuan.starrailchatbox.ui.character.catalog.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.platform.AudioPlayer
import com.kaixuan.starrailchatbox.platform.rememberAudioPlayer
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.components.StarRailPrimaryButton
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import com.kaixuan.starrailchatbox.ui.character.catalog.CharacterTagChips
import com.kaixuan.starrailchatbox.ui.character.catalog.resolveCharacterTagNames
import com.kaixuan.starrailchatbox.ui.appendFailureDetail
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.navigation.Route
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.character_catalog_tags_label

@Composable
fun CharacterCatalogDetailRoute(
    route: Route.CharacterCatalogDetail,
    koin: Koin,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val model = viewModel {
        koin.get<CharacterCatalogDetailViewModel> {
            parametersOf(route.characterId, route.detailUrl, route.name, route.avatarUrl)
        }
    }
    val state by model.uiState.collectAsStateWithLifecycle()

    BackHandler {
        onMainAction(MainAction.PopBackStack)
    }

    LaunchedEffect(model.effects) {
        model.effects.collect { effect ->
            when (effect) {
                is CharacterCatalogDetailEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(
                        appendFailureDetail(effect.message, effect.detail),
                    )
                }
            }
        }
    }

    CharacterCatalogDetailScreen(
        state = state,
        contentPadding = contentPadding,
        compact = compact,
        onMainAction = onMainAction,
        onAction = model::onAction,
        resolveUrl = { url -> model.resolveUrl(url) },
        modifier = modifier,
    )
}

@Composable
fun CharacterCatalogDetailScreen(
    state: CharacterCatalogDetailUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onAction: (CharacterCatalogDetailAction) -> Unit,
    resolveUrl: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val audioPlayer = rememberAudioPlayer()
    var showReportDialog by remember { mutableStateOf(false) }

    val detail = state.detail
    val avatarUri = detail?.avatarUrl?.let { resolveUrl(it) } ?: state.initialAvatarUrl?.let { resolveUrl(it) }.orEmpty()
    val name = detail?.name ?: state.initialName

    StarRailPageLayout(
        title = "角色详情",
        contentPadding = contentPadding,
        compact = compact,
        backContentDescription = "返回",
        onBackClick = { onMainAction(MainAction.PopBackStack) },
        modifier = modifier,
        contentSpacing = StarRailSpacing.sm,
        action = {
            Surface(
                onClick = { showReportDialog = true },
                color = Color.Transparent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .height(if (compact) 32.dp else 40.dp)
                        .padding(horizontal = StarRailSpacing.xs),
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.INFO,
                        contentDescription = "投诉",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "投诉",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isImported) {
                    StarRailPrimaryButton(
                        text = "开始对话",
                        onClick = {
                            onMainAction(MainAction.NavigateTo(Route.CharacterChat(state.characterId)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    StarRailPrimaryButton(
                        text = if (state.isImporting) "导入中..." else "导入角色",
                        onClick = { onAction(CharacterCatalogDetailAction.ImportClicked) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isImporting && !state.isLoading && detail != null,
                    )
                }
            }
        }
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (detail != null) {
            val categoryName = when (detail.primaryCategoryId) {
                "game" -> "游戏"
                "anime" -> "动漫"
                "original" -> "原创"
                else -> detail.primaryCategoryId
            }

            // 1. 角色身份卡片（头像、作者）
            CharacterReadOnlyIdentityCard(
                name = name,
                author = detail.author.takeIf { it.isNotBlank() } ?: "星轨旅人",
                avatarUri = avatarUri,
                categoryName = categoryName,
                tagNames = resolveCharacterTagNames(detail.tagIds, state.tags),
                compact = compact,
            )

            // 2. 角色背景描述
            if (detail.description.isNotBlank()) {
                CharacterReadOnlyDescriptionCard(description = detail.description)
            }

            // 3. 系统提示词 (System Prompt)
            if (detail.systemPrompt.isNotBlank()) {
                CharacterReadOnlyTextCard(
                    title = "系统提示词 (Prompt)",
                    value = detail.systemPrompt,
                    onCopyClick = {
                        clipboardManager.setText(AnnotatedString(detail.systemPrompt))
                    }
                )
            }

            // 4. 开场白 (Opening Message)
            if (detail.openingMessage.isNotBlank()) {
                CharacterReadOnlyTextCard(
                    title = "开场白",
                    value = detail.openingMessage,
                    onCopyClick = {
                        clipboardManager.setText(AnnotatedString(detail.openingMessage))
                    }
                )
            }

            // 5. 语音试听样本
            if (!detail.voiceSampleUrl.isNullOrBlank()) {
                CharacterReadOnlyVoiceSampleCard(
                    isDownloading = state.isVoiceDownloading,
                    localPath = state.voiceSampleLocalPath,
                    onDownloadClick = { onAction(CharacterCatalogDetailAction.PlayVoiceClicked) },
                    audioPlayer = audioPlayer,
                )
            }

            // 6. 模型参数 Temperature
            CharacterReadOnlySliderCard(
                title = "温度 (Temperature)",
                value = detail.temperature,
                valueRange = 0.0..2.0,
            )

            // 7. 模型参数 Top P
            CharacterReadOnlySliderCard(
                title = "核心采样 (Top P)",
                value = detail.topP,
                valueRange = 0.0..1.0,
            )
        }
    }

    if (showReportDialog) {
        StarRailDialog(
            title = "版权投诉说明",
            confirmText = "我知道了",
            onConfirm = { showReportDialog = false },
            onDismissRequest = { showReportDialog = false },
        ) {
            Text(
                text = "若您是该角色的知识产权所有人（或官方版权方），认为该用户二创内容侵犯了您的权益，请直接发送邮件至 kaixuanapp@163.com 提交版权证明，我们将在3-5个工作日内进行下架审核。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun CharacterReadOnlyIdentityCard(
    name: String,
    author: String,
    avatarUri: String,
    categoryName: String,
    tagNames: List<String>,
    compact: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(StarRailSpacing.md),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 80.dp else 96.dp)
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
                        avatarUri = avatarUri,
                        contentDescription = name,
                        placeholderKind = StarRailIconKind.PROFILE,
                        placeholderSize = 36.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
//                        Surface(
//                            shape = RoundedCornerShape(6.dp),
//                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
//                        ) {
//                            Text(
//                                text = categoryName,
//                                color = MaterialTheme.colorScheme.secondary,
//                                style = MaterialTheme.typography.labelSmall,
//                                fontWeight = FontWeight.Bold,
//                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
//                            )
//                        }
                    }
                    Text(
                        text = "作者：$author",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            if (tagNames.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.character_catalog_tags_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                CharacterTagChips(tagNames = tagNames)
            }
        }
    }
}

@Composable
private fun CharacterReadOnlyDescriptionCard(
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(StarRailSpacing.md),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        ) {
            Text(
                text = "角色背景描述说明",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = StarRailSpacing.xs)
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CharacterReadOnlyTextCard(
    title: String,
    value: String,
    onCopyClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = StarRailSpacing.md, vertical = StarRailSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    onClick = onCopyClick,
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                ) {
                    Text(
                        text = "复制",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = StarRailSpacing.xs)
            )
        }
    }
}

@Composable
private fun CharacterReadOnlyVoiceSampleCard(
    isDownloading: Boolean,
    localPath: String?,
    onDownloadClick: () -> Unit,
    audioPlayer: AudioPlayer,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(audioPlayer) {
        onDispose {
            audioPlayer.release()
        }
    }

    LaunchedEffect(localPath) {
        isPlaying = false
        audioPlayer.stop()
        durationSeconds = localPath?.let { audioPlayer.getDuration(it) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = StarRailSpacing.md, vertical = StarRailSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "语音样本",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md)
            ) {
                if (localPath.isNullOrBlank()) {
                    Surface(
                        onClick = onDownloadClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp),
                        enabled = !isDownloading
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                StarRailIcon(
                                    kind = StarRailIconKind.PLAY,
                                    contentDescription = "下载并试听",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isDownloading) "正在准备音频预览..." else "点击下载并试听语音样本",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                } else {
                    Surface(
                        onClick = {
                            if (isPlaying) {
                                audioPlayer.stop()
                                isPlaying = false
                            } else {
                                audioPlayer.play(localPath) {
                                    isPlaying = false
                                }
                                isPlaying = true
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            StarRailIcon(
                                kind = if (isPlaying) StarRailIconKind.STOP else StarRailIconKind.PLAY,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isPlaying) "正在播放样本中..." else "点击播放样本音频",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    durationSeconds?.let { seconds ->
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${seconds}秒",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterReadOnlySliderCard(
    title: String,
    value: Double,
    valueRange: ClosedRange<Double>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = StarRailSpacing.md, vertical = StarRailSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${(value * 100).roundToInt() / 100.0}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = {},
                valueRange = valueRange.start.toFloat()..valueRange.endInclusive.toFloat(),
                enabled = false,
                colors = SliderDefaults.colors(
                    disabledThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    disabledActiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            )
        }
    }
}
