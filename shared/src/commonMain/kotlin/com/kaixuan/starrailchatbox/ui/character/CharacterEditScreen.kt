package com.kaixuan.starrailchatbox.ui.character

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.importer.ImportedCharacterDraft
import com.kaixuan.starrailchatbox.data.character.importer.ImportWarning
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.character.CharacterAction.CharacterAvatarChanged
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.components.StarRailPrimaryButton
import com.kaixuan.starrailchatbox.ui.main.MainAction
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.character_edit_avatar
import starrailchatbox.shared.generated.resources.character_edit_author
import starrailchatbox.shared.generated.resources.character_edit_author_hint
import starrailchatbox.shared.generated.resources.character_edit_change_avatar
import starrailchatbox.shared.generated.resources.character_edit_delete
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_action
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_message
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_title
import starrailchatbox.shared.generated.resources.character_edit_name
import starrailchatbox.shared.generated.resources.character_edit_opening_message
import starrailchatbox.shared.generated.resources.character_edit_save
import starrailchatbox.shared.generated.resources.character_edit_restore_default
import starrailchatbox.shared.generated.resources.character_edit_system_prompt
import starrailchatbox.shared.generated.resources.character_edit_system_prompt_hint
import starrailchatbox.shared.generated.resources.character_edit_temperature
import starrailchatbox.shared.generated.resources.character_edit_temperature_hint
import starrailchatbox.shared.generated.resources.character_edit_title
import starrailchatbox.shared.generated.resources.character_edit_top_p
import starrailchatbox.shared.generated.resources.character_edit_top_p_hint
import starrailchatbox.shared.generated.resources.character_edit_prompt_gen_btn
import starrailchatbox.shared.generated.resources.character_edit_prompt_gen_title
import starrailchatbox.shared.generated.resources.character_edit_prompt_gen_default_input
import starrailchatbox.shared.generated.resources.character_edit_prompt_gen_generating
import starrailchatbox.shared.generated.resources.character_edit_avatar_gen_btn
import starrailchatbox.shared.generated.resources.character_edit_avatar_gen_title
import starrailchatbox.shared.generated.resources.character_edit_avatar_gen_default_input
import starrailchatbox.shared.generated.resources.character_edit_avatar_gen_generating
import starrailchatbox.shared.generated.resources.character_edit_avatar_gen_failed
import starrailchatbox.shared.generated.resources.character_edit_avatar_gen_config_required
import starrailchatbox.shared.generated.resources.character_edit_importing
import starrailchatbox.shared.generated.resources.cancel
import starrailchatbox.shared.generated.resources.confirm
import starrailchatbox.shared.generated.resources.navigation_back
import starrailchatbox.shared.generated.resources.settings_saving
import com.kaixuan.starrailchatbox.platform.rememberAudioPlayer
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.nameWithoutExtension
import starrailchatbox.shared.generated.resources.character_edit_voice_clear
import starrailchatbox.shared.generated.resources.character_edit_voice_sample
import starrailchatbox.shared.generated.resources.character_edit_voice_sample_hint
import starrailchatbox.shared.generated.resources.character_edit_voice_select
import kotlin.math.roundToInt


/**
 * 角色编辑界面，用于创建新角色或修改现有角色的详细信息（名称、提示词、头像、语音等）。
 */
@Composable
fun CharacterEditScreen(
    state: CharacterEditUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onAction: (CharacterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onMainAction(MainAction.PopBackStack)
    }

    val editState = state
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.Image,
    ) { image ->
        if (image != null) {
            onAction(
                CharacterAvatarChanged(
                    CharacterAvatarSource(
                        uri = image.path ?: "",
                        name = image.name,
                        extension = image.extension,
                    )
                )
            )
        }
    }

    StarRailPageLayout(
        title = stringResource(Res.string.character_edit_title),
        contentPadding = contentPadding,
        compact = compact,
        backContentDescription = stringResource(Res.string.navigation_back),
        onBackClick = { onMainAction(MainAction.PopBackStack) },
        modifier = modifier,
        contentSpacing = StarRailSpacing.md,
    ) {
        if (editState.isImporting) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                    )
                    Text(
                        text = stringResource(Res.string.character_edit_importing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (editState.importDraft != null) {
            CharacterImportBanner(
                draft = editState.importDraft,
                onDismiss = { onAction(CharacterAction.CharacterImportCancelled) },
                onClearWarnings = { onAction(CharacterAction.CharacterImportWarningDismissed) }
            )
        }

        if (editState.importError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.INFO,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = editState.importError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        onClick = { onAction(CharacterAction.CharacterImportCancelled) },
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        StarRailIcon(
                            kind = StarRailIconKind.CLOSE,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).padding(4.dp)
                        )
                    }
                }
            }
        }

        CharacterIdentityCard(
            state = editState,
            compact = compact,
            onPickAvatar = { imagePicker.launch() },
            onNameChanged = { name ->
                onAction(CharacterAction.CharacterNameChanged(name))
            },
            onAction = onAction,
        )

        val defaultPromptRequestText = stringResource(
            Res.string.character_edit_prompt_gen_default_input,
            editState.name
        )
        CharacterTextCard(
            title = stringResource(Res.string.character_edit_system_prompt),
            value = editState.prompt,
            minLines = 5,
            onValueChange = { prompt ->
                onAction(CharacterAction.CharacterPromptChanged(prompt))
            },
            hint = if (editState.characterId != null) {
                stringResource(Res.string.character_edit_system_prompt_hint)
            } else null,
            actionButton = {
                Surface(
                    onClick = {
                        onAction(CharacterAction.CharacterPromptGenClicked(defaultPromptRequestText))
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (editState.isGeneratingPrompt) {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (editState.isGeneratingPrompt) {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        }
                    ),
                    enabled = !editState.isGeneratingPrompt,
                ) {
                    Text(
                        text = stringResource(
                            if (editState.isGeneratingPrompt) {
                                Res.string.character_edit_prompt_gen_generating
                            } else {
                                Res.string.character_edit_prompt_gen_btn
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (editState.isGeneratingPrompt) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        )

        CharacterTextCard(
            title = stringResource(Res.string.character_edit_opening_message),
            value = editState.openingMessage,
            minLines = 2,
            onValueChange = { openingMessage ->
                onAction(CharacterAction.CharacterOpeningMessageChanged(openingMessage))
            },
        )

        CharacterSliderCard(
            title = stringResource(Res.string.character_edit_temperature),
            value = editState.temperature,
            valueRange = 0.0..2.0,
            hint = stringResource(Res.string.character_edit_temperature_hint),
            onValueChange = { onAction(CharacterAction.CharacterTemperatureChanged(it)) },
        )

        CharacterSliderCard(
            title = stringResource(Res.string.character_edit_top_p),
            value = editState.topP,
            valueRange = 0.0..1.0,
            hint = stringResource(Res.string.character_edit_top_p_hint),
            onValueChange = { onAction(CharacterAction.CharacterTopPChanged(it)) },
        )

        CharacterVoiceSampleCard(
            state = editState,
            onAction = onAction
        )

        if (editState.characterId?.startsWith("builtin:") == true) {
            Surface(
                onClick = { onAction(CharacterAction.CharacterRestoreDefaultClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StarRailIcon(
                            kind = StarRailIconKind.SPARKLE,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(Res.string.character_edit_restore_default),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = { onMainAction(MainAction.PopBackStack) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.cancel),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            StarRailPrimaryButton(
                text = stringResource(
                    if (editState.isSaving) {
                        Res.string.settings_saving
                    } else {
                        Res.string.character_edit_save
                    },
                ),
                onClick = { onAction(CharacterAction.CharacterSaveClicked) },
                modifier = Modifier.weight(1f),
                enabled = !editState.isSaving && editState.name.isNotBlank(),
            )
        }

        if (editState.isPromptGenDialogOpen) {
            StarRailDialog(
                title = stringResource(Res.string.character_edit_prompt_gen_title),
                dismissText = stringResource(Res.string.cancel),
                confirmText = stringResource(Res.string.confirm),
                onDismissRequest = { onAction(CharacterAction.CharacterPromptGenCancelClicked) },
                onConfirm = { onAction(CharacterAction.CharacterPromptGenConfirmClicked) },
            ) {
                LabeledTextField(
                    value = editState.promptGenInputText,
                    onValueChange = { text ->
                        onAction(CharacterAction.CharacterPromptGenInputChanged(text))
                    },
                    minLines = 4,
                )
            }
        }

        if (editState.isAvatarGenDialogOpen) {
            StarRailDialog(
                title = stringResource(Res.string.character_edit_avatar_gen_title),
                dismissText = stringResource(Res.string.cancel),
                confirmText = stringResource(Res.string.confirm),
                onDismissRequest = { onAction(CharacterAction.CharacterAvatarGenCancelClicked) },
                onConfirm = { onAction(CharacterAction.CharacterAvatarGenConfirmClicked) },
            ) {
                LabeledTextField(
                    value = editState.avatarGenInputText,
                    onValueChange = { text ->
                        onAction(CharacterAction.CharacterAvatarGenInputChanged(text))
                    },
                    minLines = 4,
                )
            }
        }
    }
}

@Composable
private fun CharacterImportBanner(
    draft: ImportedCharacterDraft,
    onDismiss: () -> Unit,
    onClearWarnings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StarRailIcon(
                    kind = StarRailIconKind.FILE,
                    tint = MaterialTheme.colorScheme.secondary,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "角色卡已导入",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "来源版本: ${draft.sourceVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.CLOSE,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).padding(4.dp)
                    )
                }
            }

            if (draft.warnings.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StarRailIcon(
                                kind = StarRailIconKind.INFO,
                                tint = MaterialTheme.starRailColors.warmSparkle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "导入警告",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.starRailColors.warmSparkle
                            )
                        }
                        draft.warnings.forEach { warning ->
                            val text = when (warning) {
                                ImportWarning.UNSUPPORTED_V3_FIELDS -> "部分 V3 专属字段暂不支持，已忽略。"
                                ImportWarning.UNSUPPORTED_ALTERNATE_GREETINGS -> "备选开场白未导入。"
                                ImportWarning.UNSUPPORTED_CHARACTER_BOOK -> "世界书（Lorebook）未导入。"
                                ImportWarning.UNSUPPORTED_TAGS -> "标签未导入。"
                                ImportWarning.UNSUPPORTED_CREATOR_NOTES -> "作者备注未导入。"
                                ImportWarning.OTHER_EXTENSIONS_IGNORED -> "其他扩展字段已忽略。"
                            }
                            Text(
                                text = "• $text",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = "我知道了",
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { onClearWarnings() }
                                .padding(top = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterIdentityCard(
    state: CharacterEditUiState,
    compact: Boolean,
    onPickAvatar: () -> Unit,
    onNameChanged: (String) -> Unit,
    onAction: (CharacterAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = StarRailSpacing.md, vertical = StarRailSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                CharacterEditAvatar(
                    avatarUri = state.avatarUri,
                    contentDescription = stringResource(Res.string.character_edit_avatar),
                    size = if (compact) 96.dp else 112.dp,
                )
                StarRailIcon(
                    kind = StarRailIconKind.SPARKLE,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = 1.dp, y = (-1).dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.character_edit_avatar),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val defaultAvatarPrompt = stringResource(
                        Res.string.character_edit_avatar_gen_default_input,
                        state.name
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = {
                                onAction(CharacterAction.CharacterAvatarGenClicked(defaultAvatarPrompt))
                            },
                            shape = MaterialTheme.shapes.extraLarge,
                            color = if (state.isGeneratingAvatar) {
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (state.isGeneratingAvatar) {
                                    MaterialTheme.colorScheme.outlineVariant
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                }
                            ),
                            enabled = !state.isGeneratingAvatar
                        ) {
                            Text(
                                text = if (state.isGeneratingAvatar) {
                                    stringResource(Res.string.character_edit_avatar_gen_generating)
                                } else {
                                    stringResource(Res.string.character_edit_avatar_gen_btn)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (state.isGeneratingAvatar) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Surface(
                            onClick = onPickAvatar,
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                            enabled = !state.isGeneratingAvatar
                        ) {
                            Text(
                                text = stringResource(Res.string.character_edit_change_avatar),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                LabeledTextField(
                    label = stringResource(Res.string.character_edit_name),
                    value = state.name,
                    onValueChange = onNameChanged,
                    singleLine = true,
                )
                LabeledTextField(
                    label = stringResource(Res.string.character_edit_author),
                    value = state.author,
                    onValueChange = {
                        onAction(CharacterAction.CharacterAuthorChanged(it))
                    },
                    supportingText = stringResource(Res.string.character_edit_author_hint),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun CharacterEditAvatar(
    avatarUri: String,
    contentDescription: String,
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
            .padding(4.dp)
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

@Composable
private fun CharacterTextCard(
    title: String,
    value: String,
    minLines: Int,
    onValueChange: (String) -> Unit,
    actionButton: @Composable (() -> Unit)? = null,
    hint: String? = null,
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
                if (actionButton != null) {
                    actionButton()
                }
            }
            LabeledTextField(
                value = value,
                onValueChange = onValueChange,
                minLines = minLines,
                supportingText = "${value.length}",
            )
            if (hint != null) {
                Text(
                    text = hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CharacterVoiceSampleCard(
    state: CharacterEditUiState,
    onAction: (CharacterAction) -> Unit,
) {
    val audioPicker = rememberFilePickerLauncher(
        type = FileKitType.File(
            extensions = listOf("mp3", "wav")
        )
    ) { picked ->
        if (picked != null) {
            onAction(
                CharacterAction.CharacterVoiceSampleChanged(
                    uri = picked.path ?: "",
                    name = picked.name,
                    extension = picked.extension,
                )
            )
        }
    }

    val audioPlayer = rememberAudioPlayer()
    var isPlaying by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(audioPlayer) {
        onDispose {
            audioPlayer.release()
        }
    }

    LaunchedEffect(state.voiceSampleUri) {
        isPlaying = false
        audioPlayer.stop()
        durationSeconds = state.voiceSampleUri?.let { audioPlayer.getDuration(it) }
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
                    text = stringResource(Res.string.character_edit_voice_sample),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!state.voiceSampleUri.isNullOrEmpty()) {
                        Surface(
                            onClick = { onAction(CharacterAction.CharacterVoiceSampleChanged(null)) },
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = stringResource(Res.string.character_edit_voice_clear),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Surface(
                        onClick = { audioPicker.launch() },
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                    ) {
                        Text(
                            text = stringResource(Res.string.character_edit_voice_select),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (!state.voiceSampleUri.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md)
                ) {
                    Surface(
                        onClick = {
                            if (isPlaying) {
                                audioPlayer.stop()
                                isPlaying = false
                            } else {
                                state.voiceSampleUri?.let { uri ->
                                    audioPlayer.play(uri) {
                                        isPlaying = false
                                    }
                                    isPlaying = true
                                }
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

                    Column {
                        Text(
                            text = state.voiceSampleUri.substringAfterLast('/'),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.basicMarquee(),
                            maxLines = 1,
                        )
                        Text(
                            text = durationSeconds?.let { 
                                val mins = it / 60
                                val secs = it % 60
                                "$mins:${secs.toString().padStart(2, '0')}"
                            } ?: "--:--",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Text(
                text = stringResource(Res.string.character_edit_voice_sample_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun CharacterSliderCard(
    title: String,
    value: Double,
    valueRange: ClosedFloatingPointRange<Double>,
    hint: String,
    onValueChange: (Double) -> Unit,
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
                    text = value.sliderLabel(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toDouble()) },
                valueRange = valueRange.start.toFloat()..valueRange.endInclusive.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            )
            Text(
                text = hint,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    supportingText: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
    ) {
        if (label != null) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .let { 
                        if (singleLine) it.height(48.dp) else it
                    }
                    .clip(if (singleLine) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp)),
                singleLine = singleLine,
                minLines = minLines,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = if (singleLine) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    disabledBorderColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                )
            )
            if (supportingText != null && !singleLine) {
                Text(
                    text = supportingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp)
                )
            }
        }
        if (supportingText != null && singleLine) {
            Text(
                text = supportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun Double.sliderLabel(): String {
    val rounded = (this * 100).roundToInt() / 100.0
    val tenths = (rounded * 10).roundToInt()
    return if ((rounded * 10).roundToInt() / 10.0 == rounded) {
        (tenths / 10.0).toString()
    } else {
        rounded.toString()
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterEditScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        CharacterEditScreen(
            state = characterEditPreviewState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterEditScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        CharacterEditScreen(
            state = characterEditPreviewState,
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterEditScreenImportingLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        CharacterEditScreen(
            state = CharacterEditUiState(isImporting = true),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun CharacterEditScreenImportingDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        CharacterEditScreen(
            state = CharacterEditUiState(isImporting = true),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {},
        )
    }
}

private val characterEditPreviewCharacter = Character(
    id = "builtin:三月七",
    name = "三月七",
    prompt = "你是三月七，热情开朗、元气满满的少女。",
    openingMessage = "今天想聊点什么呢？",
    avatarUri = "",
    temperature = 0.85,
    topP = 0.9,
)

private val characterEditPreviewState = characterEditPreviewCharacter.run {
    CharacterEditUiState(
        characterId = id,
        name = name,
        prompt = prompt,
        openingMessage = openingMessage,
        avatarUri = avatarUri,
        temperature = temperature,
        topP = topP,
    )
}
