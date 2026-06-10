package com.kaixuan.starrailchatbox.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import com.kaixuan.starrailchatbox.platform.openUri
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.cancel
import starrailchatbox.shared.generated.resources.confirm
import starrailchatbox.shared.generated.resources.settings_about_desc
import starrailchatbox.shared.generated.resources.settings_about_title
import starrailchatbox.shared.generated.resources.settings_api_desc
import starrailchatbox.shared.generated.resources.settings_api_title
import starrailchatbox.shared.generated.resources.settings_api_configured
import starrailchatbox.shared.generated.resources.settings_api_not_configured
import starrailchatbox.shared.generated.resources.settings_multimodal_api_desc
import starrailchatbox.shared.generated.resources.settings_multimodal_api_title
import starrailchatbox.shared.generated.resources.settings_image_generation_api_desc
import starrailchatbox.shared.generated.resources.settings_image_generation_api_title
import starrailchatbox.shared.generated.resources.settings_voice_api_title
import starrailchatbox.shared.generated.resources.settings_voice_api_desc
import starrailchatbox.shared.generated.resources.settings_profile_desc
import starrailchatbox.shared.generated.resources.settings_profile_title
import starrailchatbox.shared.generated.resources.settings_privacy_desc
import starrailchatbox.shared.generated.resources.settings_privacy_title
import starrailchatbox.shared.generated.resources.settings_qq_group_prefix
import starrailchatbox.shared.generated.resources.settings_qq_group_number
import starrailchatbox.shared.generated.resources.settings_theme_desc
import starrailchatbox.shared.generated.resources.settings_theme_title
import starrailchatbox.shared.generated.resources.settings_title
import starrailchatbox.shared.generated.resources.settings_update_desc
import starrailchatbox.shared.generated.resources.settings_update_available
import starrailchatbox.shared.generated.resources.settings_update_title
import starrailchatbox.shared.generated.resources.theme_dark
import starrailchatbox.shared.generated.resources.theme_follow_system
import starrailchatbox.shared.generated.resources.theme_light
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.main.MainSettingsItem
import com.kaixuan.starrailchatbox.ui.main.MainUiState
import com.kaixuan.starrailchatbox.getPlatform

private data class SettingsItemUiData(
    val item: SettingsItem,
    val iconKind: StarRailIconKind,
    val titleRes: StringResource,
    val descRes: StringResource,
    val isConfigured: Boolean? = null,
    val getColors: @Composable () -> Pair<Color, Color>, // Returns (ContainerColor, IconColor)
    val getDescColor: @Composable () -> Color? = { null }
)

@Composable
fun SettingsScreen(
    mainState: MainUiState,
    settingsState: SettingsUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onSettingsAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUpdate = mainState.updateInfo != null
    val items = listOf(
        SettingsItemUiData(
            item = SettingsItem.PROFILE,
            iconKind = StarRailIconKind.PERSON,
            titleRes = Res.string.settings_profile_title,
            descRes = Res.string.settings_profile_desc,
            getColors = {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.primary
            }
        ),
        SettingsItemUiData(
            item = SettingsItem.API_SETTINGS,
            iconKind = StarRailIconKind.CUBE,
            titleRes = Res.string.settings_api_title,
            descRes = Res.string.settings_api_desc,
            isConfigured = settingsState.isDefaultConfigured,
            getColors = {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.secondary
            }
        ),
        SettingsItemUiData(
            item = SettingsItem.MULTIMODAL_API_SETTINGS,
            iconKind = StarRailIconKind.SPARKLE,
            titleRes = Res.string.settings_multimodal_api_title,
            descRes = Res.string.settings_multimodal_api_desc,
            isConfigured = settingsState.isMultimodalConfigured,
            getColors = {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.tertiary
            }
        ),
        SettingsItemUiData(
            item = SettingsItem.IMAGE_GENERATION_API_SETTINGS,
            iconKind = StarRailIconKind.GALLERY,
            titleRes = Res.string.settings_image_generation_api_title,
            descRes = Res.string.settings_image_generation_api_desc,
            isConfigured = settingsState.isImageGenerationConfigured,
            getColors = {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.secondary
            }
        ),
        SettingsItemUiData(
            item = SettingsItem.VOICE_API_SETTINGS,
            iconKind = StarRailIconKind.VOICE,
            titleRes = Res.string.settings_voice_api_title,
            descRes = Res.string.settings_voice_api_desc,
            isConfigured = settingsState.isVoiceConfigured,
            getColors = {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.primary
            }
        ),
        SettingsItemUiData(
            item = SettingsItem.CHECK_UPDATE,
            iconKind = StarRailIconKind.UPDATE,
            titleRes = Res.string.settings_update_title,
            descRes = if (hasUpdate) Res.string.settings_update_available else Res.string.settings_update_desc,
            getColors = {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.secondary
            },
            getDescColor = {
                if (hasUpdate) MaterialTheme.colorScheme.primary else null
            }
        ),
        SettingsItemUiData(
            item = SettingsItem.THEME_STYLE,
            iconKind = StarRailIconKind.PALETTE,
            titleRes = Res.string.settings_theme_title,
            descRes = Res.string.settings_theme_desc,
            getColors = {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.tertiary
            }
        ),
        SettingsItemUiData(
            item = SettingsItem.ABOUT_US,
            iconKind = StarRailIconKind.INFO,
            titleRes = Res.string.settings_about_title,
            descRes = Res.string.settings_about_desc,
            getColors = {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.primary
            }
        ),
        SettingsItemUiData(
            item = SettingsItem.PRIVACY_SECURITY,
            iconKind = StarRailIconKind.SHIELD,
            titleRes = Res.string.settings_privacy_title,
            descRes = Res.string.settings_privacy_desc,
            getColors = {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) to MaterialTheme.colorScheme.secondary
            }
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + StarRailSpacing.lg,
                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.lg
            ),
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.lg)
    ) {
        // Top Large Title
        Text(
            text = stringResource(Res.string.settings_title),
            color = MaterialTheme.colorScheme.onBackground,
            style = if (compact) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.headlineLarge
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        // Settings Items Container Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                items.forEachIndexed { index, itemData ->
                    SettingsItemRow(
                        data = itemData,
                        onClick = {
                            if (itemData.item == SettingsItem.PROFILE || 
                                itemData.item == SettingsItem.API_SETTINGS || 
                                itemData.item == SettingsItem.MULTIMODAL_API_SETTINGS || 
                                itemData.item == SettingsItem.IMAGE_GENERATION_API_SETTINGS ||
                                itemData.item == SettingsItem.VOICE_API_SETTINGS || 
                                itemData.item == SettingsItem.CHECK_UPDATE ||
                                itemData.item == SettingsItem.THEME_STYLE ||
                                itemData.item == SettingsItem.ABOUT_US ||
                                itemData.item == SettingsItem.PRIVACY_SECURITY) {
                                onMainAction(MainAction.SettingsItemClicked(MainSettingsItem.valueOf(itemData.item.name)))
                            } else {
                                onSettingsAction(SettingsAction.SettingsItemClicked(itemData.item))
                            }
                        },
                        compact = compact
                    )
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = StarRailSpacing.md),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }

        // Footer: QQ Group & Powered by StarRailChatBox
        val clipboardManager = LocalClipboardManager.current
        val qqGroupNumber = stringResource(Res.string.settings_qq_group_number)
        val qqGroupText = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                append(stringResource(Res.string.settings_qq_group_prefix))
            }
            pushStringAnnotation(tag = "COPY", annotation = qqGroupNumber)
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(qqGroupNumber)
            }
            pop()
        }

        val footerText = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                append("Powered by ")
            }
            pushStringAnnotation(
                tag = "URL",
                annotation = "https://github.com/KaiXuan666/StarRailChatBox"
            )
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("StarRailChatBox")
            }
            pop()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = StarRailSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ClickableText(
                    text = qqGroupText,
                    style = MaterialTheme.typography.bodySmall,
                    onClick = { offset ->
                        qqGroupText.getStringAnnotations(tag = "COPY", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                clipboardManager.setText(AnnotatedString(annotation.item))
                                onSettingsAction(SettingsAction.CopyToClipboard(annotation.item))
                            }
                    }
                )

                ClickableText(
                    text = footerText,
                    style = MaterialTheme.typography.bodySmall,
                    onClick = { offset ->
                        footerText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                openUri(annotation.item)
                            }
                    }
                )
            }
        }
    }

    // Theme Selection Dialog
    if (mainState.showThemeDialog) {
        ThemeStyleDialog(
            currentThemeOverride = mainState.darkThemeOverride,
            onMainAction = onMainAction
        )
    }
}

@Composable
private fun SettingsItemRow(
    data: SettingsItemUiData,
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val (containerColor, iconColor) = data.getColors()
    val title = stringResource(data.titleRes)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = StarRailSpacing.md,
                vertical = if (compact) StarRailSpacing.sm else StarRailSpacing.md
            ),
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container
        Box(
            modifier = Modifier
                .size(if (compact) 38.dp else 42.dp)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            StarRailIcon(
                kind = data.iconKind,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(if (compact) 18.dp else 22.dp)
            )
        }

        // Texts (Title & Description)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = if (compact) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                },
                maxLines = 1
            )
            val description = if (data.item == SettingsItem.CHECK_UPDATE && data.descRes == Res.string.settings_update_desc) {
                stringResource(data.descRes, getPlatform().versionName)
            } else {
                stringResource(data.descRes)
            }
            Text(
                text = description,
                color = data.getDescColor() ?: MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }

        // Status Text and Icon
        if (data.isConfigured != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val statusText = if (data.isConfigured) {
                    stringResource(Res.string.settings_api_configured)
                } else {
                    stringResource(Res.string.settings_api_not_configured)
                }
                val statusColor = if (data.isConfigured) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
                val statusIcon = if (data.isConfigured) {
                    StarRailIconKind.CHECK
                } else {
                    StarRailIconKind.INFO
                }

                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                StarRailIcon(
                    kind = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                )
            }
        }

        // Right Chevron Arrow
        StarRailIcon(
            kind = StarRailIconKind.CHEVRON_RIGHT,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(if (compact) 18.dp else 22.dp)
        )
    }
}

@Composable
private fun ThemeStyleDialog(
    currentThemeOverride: Boolean?,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTheme by remember(currentThemeOverride) {
        mutableStateOf(currentThemeOverride)
    }

    val options = listOf(
        Triple(null, Res.string.theme_follow_system, "System"),
        Triple(false, Res.string.theme_light, "Light"),
        Triple(true, Res.string.theme_dark, "Dark")
    )

    StarRailDialog(
        title = stringResource(Res.string.settings_theme_title),
        dismissText = stringResource(Res.string.cancel),
        confirmText = stringResource(Res.string.confirm),
        onDismissRequest = { onMainAction(MainAction.ThemeDialogDismiss) },
        onConfirm = { onMainAction(MainAction.ThemeDialogConfirm(selectedTheme)) },
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { (value, labelRes, _) ->
                val isSelected = selectedTheme == value
                val itemBgBrush = if (isSelected) {
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
                val itemBorderColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                }

                Surface(
                    onClick = { selectedTheme = value },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(
                        alpha = if (isSelected) 0.5f else 0.2f,
                    ),
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, itemBorderColor),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(itemBgBrush)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .border(
                                    2.dp,
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    },
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                StarRailIcon(
                                    kind = StarRailIconKind.SPARKLE,
                                    contentDescription = null,
                                    tint = MaterialTheme.starRailColors.warmSparkle,
                                    modifier = Modifier.size(10.dp),
                                )
                            }
                        }
                        Text(
                            text = stringResource(labelRes),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun SettingsScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        SettingsScreen(
            mainState = MainUiState(),
            settingsState = SettingsUiState(),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onSettingsAction = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun SettingsScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        SettingsScreen(
            mainState = MainUiState(darkThemeOverride = true),
            settingsState = SettingsUiState(),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onSettingsAction = {}
        )
    }
}
