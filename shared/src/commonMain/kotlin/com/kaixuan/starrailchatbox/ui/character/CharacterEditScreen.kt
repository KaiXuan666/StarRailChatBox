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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.platform.rememberImagePicker
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.components.StarRailPrimaryButton
import com.kaixuan.starrailchatbox.ui.main.MainAction
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.character_edit_avatar
import starrailchatbox.shared.generated.resources.character_edit_change_avatar
import starrailchatbox.shared.generated.resources.character_edit_delete
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_action
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_message
import starrailchatbox.shared.generated.resources.character_edit_delete_confirm_title
import starrailchatbox.shared.generated.resources.character_edit_name
import starrailchatbox.shared.generated.resources.character_edit_opening_message
import starrailchatbox.shared.generated.resources.character_edit_save
import starrailchatbox.shared.generated.resources.character_edit_system_prompt
import starrailchatbox.shared.generated.resources.character_edit_temperature
import starrailchatbox.shared.generated.resources.character_edit_temperature_hint
import starrailchatbox.shared.generated.resources.character_edit_title
import starrailchatbox.shared.generated.resources.character_edit_top_p
import starrailchatbox.shared.generated.resources.character_edit_top_p_hint
import starrailchatbox.shared.generated.resources.character_edit_prompt_gen_btn
import starrailchatbox.shared.generated.resources.character_edit_prompt_gen_title
import starrailchatbox.shared.generated.resources.character_edit_prompt_gen_default_input
import starrailchatbox.shared.generated.resources.character_edit_prompt_gen_generating
import starrailchatbox.shared.generated.resources.cancel
import starrailchatbox.shared.generated.resources.confirm
import starrailchatbox.shared.generated.resources.navigation_back
import starrailchatbox.shared.generated.resources.settings_saving
import kotlin.math.roundToInt

private const val MaxCharacterNameLength = 40
private const val MaxPromptLength = 5000
private const val MaxOpeningMessageLength = 200

@Composable
fun CharacterEditScreen(
    characterId: String?,
    state: CharactersUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onAction: (CharacterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(characterId) {
        onAction(CharacterAction.CharacterEditOpened(characterId))
    }

    BackHandler {
        onMainAction(MainAction.PopBackStack)
    }

    val editState = state.characterEdit
    var deleteDialogVisible by remember { mutableStateOf(false) }
    val imagePicker = rememberImagePicker { image ->
        if (image != null) {
            onAction(CharacterAction.CharacterAvatarChanged(CharacterAvatarSource(image.uri)))
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
        CharacterIdentityCard(
            state = editState,
            compact = compact,
            onPickAvatar = imagePicker,
            onNameChanged = { name ->
                onAction(CharacterAction.CharacterNameChanged(name.take(MaxCharacterNameLength)))
            },
        )

        val defaultPromptRequestText = stringResource(
            Res.string.character_edit_prompt_gen_default_input,
            editState.name
        )
        CharacterTextCard(
            title = stringResource(Res.string.character_edit_system_prompt),
            value = editState.prompt,
            maxLength = MaxPromptLength,
            minLines = 5,
            onValueChange = { prompt ->
                onAction(CharacterAction.CharacterPromptChanged(prompt.take(MaxPromptLength)))
            },
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
            maxLength = MaxOpeningMessageLength,
            minLines = 2,
            onValueChange = { openingMessage ->
                onAction(CharacterAction.CharacterOpeningMessageChanged(openingMessage.take(MaxOpeningMessageLength)))
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
                enabled = !editState.isSaving,
            )
        }

        if (editState.characterId != null) {
            Surface(
                onClick = { deleteDialogVisible = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.65f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.character_edit_delete),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
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

    if (deleteDialogVisible) {
        StarRailDialog(
            title = stringResource(Res.string.character_edit_delete_confirm_title),
            dismissText = stringResource(Res.string.cancel),
            confirmText = stringResource(Res.string.character_edit_delete_confirm_action),
            destructive = true,
            onDismissRequest = { deleteDialogVisible = false },
            onConfirm = {
                deleteDialogVisible = false
                editState.characterId?.let { onAction(CharacterAction.CharacterDeleteClicked(it)) }
            },
        ) {
            Text(
                text = stringResource(
                    Res.string.character_edit_delete_confirm_message,
                    editState.name,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CharacterIdentityCard(
    state: CharacterEditUiState,
    compact: Boolean,
    onPickAvatar: () -> Unit,
    onNameChanged: (String) -> Unit,
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
                    Surface(
                        onClick = onPickAvatar,
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
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
                LabeledTextField(
                    label = stringResource(Res.string.character_edit_name),
                    value = state.name,
                    onValueChange = onNameChanged,
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
    maxLength: Int,
    minLines: Int,
    onValueChange: (String) -> Unit,
    actionButton: @Composable (() -> Unit)? = null,
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
                supportingText = "${value.length}/$maxLength",
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
            characterId = characterEditPreviewCharacter.id,
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
            characterId = characterEditPreviewCharacter.id,
            state = characterEditPreviewState,
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

private val characterEditPreviewState = CharactersUiState(
    characters = listOf(characterEditPreviewCharacter),
    selectedCharacterId = characterEditPreviewCharacter.id,
    characterEdit = characterEditPreviewCharacter.run {
        CharacterEditUiState(
            characterId = id,
            name = name,
            prompt = prompt,
            openingMessage = openingMessage,
            avatarUri = avatarUri,
            temperature = temperature,
            topP = topP,
        )
    },
    isLoadingCharacters = false,
)
