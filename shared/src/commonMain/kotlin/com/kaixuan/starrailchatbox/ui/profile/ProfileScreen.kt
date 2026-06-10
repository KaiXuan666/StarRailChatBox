@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.kaixuan.starrailchatbox.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.main.MainAction
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.global_settings_avatar_section
import starrailchatbox.shared.generated.resources.global_settings_data_footer
import starrailchatbox.shared.generated.resources.global_settings_data_management
import starrailchatbox.shared.generated.resources.global_settings_enable_web_search
import starrailchatbox.shared.generated.resources.global_settings_enable_web_search_desc
import starrailchatbox.shared.generated.resources.global_settings_export_data
import starrailchatbox.shared.generated.resources.global_settings_general_section
import starrailchatbox.shared.generated.resources.global_settings_import_data
import starrailchatbox.shared.generated.resources.global_settings_messages_unit
import starrailchatbox.shared.generated.resources.global_settings_save_multimodal_token
import starrailchatbox.shared.generated.resources.global_settings_save_multimodal_token_desc
import starrailchatbox.shared.generated.resources.global_settings_summary_threshold
import starrailchatbox.shared.generated.resources.global_settings_summary_threshold_desc
import starrailchatbox.shared.generated.resources.navigation_back
import starrailchatbox.shared.generated.resources.profile_avatar_title
import starrailchatbox.shared.generated.resources.profile_restore_default
import starrailchatbox.shared.generated.resources.settings_profile_title

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.starRailColors
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.Image,
    ) { image ->
        if (image != null) {
            onAction(ProfileAction.AvatarChanged(image.path ?: ""))
        }
    }

    val exportLauncher = rememberDirectoryPickerLauncher { directory ->
        if (directory != null) {
            onAction(ProfileAction.ExportData(directory))
        }
    }

    val importLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(),
    ) { file ->
        if (file != null) {
            onAction(ProfileAction.ImportData(file))
        }
    }

    BackHandler {
        onAction(ProfileAction.BackClicked)
    }

    StarRailPageLayout(
        title = stringResource(Res.string.settings_profile_title),
        contentPadding = contentPadding,
        compact = compact,
        backContentDescription = stringResource(Res.string.navigation_back),
        onBackClick = { onAction(ProfileAction.BackClicked) },
        modifier = modifier,
    ) {
        // User Avatar Section
        SettingsSection(
            title = stringResource(Res.string.global_settings_avatar_section)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Avatar with orbit-like decoration
                Box(contentAlignment = Alignment.Center) {
                    // Outer Orbit
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val strokeWidth = 1.dp.toPx()
                        val color = colors.constellation.copy(alpha = 0.2f)
                        drawCircle(
                            color = color,
                            radius = size.minDimension / 2,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    
                    // Avatar Container
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .border(2.dp, colors.constellation.copy(alpha = 0.4f), CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarImage(
                            avatarUri = state.customAvatarUri.orEmpty(),
                            contentDescription = null,
                            placeholderKind = StarRailIconKind.SPARKLE,
                            placeholderSize = 60.dp,
                            modifier = Modifier.fillMaxSize(),
                            isUser = true,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // Change Avatar Button
                    SecondaryActionButton(
                        onClick = { imagePicker.launch() },
                        icon = StarRailIconKind.GALLERY,
                        text = stringResource(Res.string.profile_avatar_title),
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Restore Default Button
                    SecondaryActionButton(
                        onClick = { onAction(ProfileAction.RestoreDefaultAvatar) },
                        icon = StarRailIconKind.UPDATE,
                        text = stringResource(Res.string.profile_restore_default),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // General Settings Section
        SettingsSection(
            title = stringResource(Res.string.global_settings_general_section)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Threshold
//                SettingsItemRow(
//                    title = stringResource(Res.string.global_settings_summary_threshold),
//                    description = stringResource(Res.string.global_settings_summary_threshold_desc),
//                ) {
//                    ThresholdDropdown(
//                        value = state.summaryThreshold,
//                        onValueChange = { onAction(ProfileAction.SummaryThresholdChanged(it)) }
//                    )
//                }

                // Save Multimodal Token
                SettingsItemRow(
                    title = stringResource(Res.string.global_settings_save_multimodal_token),
                    description = stringResource(Res.string.global_settings_save_multimodal_token_desc),
                ) {
                    Switch(
                        checked = state.saveMultimodalToken,
                        onCheckedChange = { onAction(ProfileAction.SaveMultimodalTokenChanged(it)) },
                        colors = switchColors()
                    )
                }

                // Web Search
//                SettingsItemRow(
//                    title = stringResource(Res.string.global_settings_enable_web_search),
//                    description = stringResource(Res.string.global_settings_enable_web_search_desc),
//                ) {
//                    Switch(
//                        checked = state.enableWebSearch,
//                        onCheckedChange = { onAction(ProfileAction.EnableWebSearchChanged(it)) },
//                        colors = switchColors()
//                    )
//                }
            }
        }

        // Data Management Section
        SettingsSection(
            title = stringResource(Res.string.global_settings_data_management)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Export Data Button (Gradient)
                PrimaryGradientButton(
                    text = stringResource(Res.string.global_settings_export_data),
                    icon = StarRailIconKind.ARROW_UP,
                    onClick = { exportLauncher.launch() }
                )

                // Import Data Button (Outlined)
                OutlinedActionButton(
                    text = stringResource(Res.string.global_settings_import_data),
                    icon = StarRailIconKind.FILE,
                    onClick = { importLauncher.launch() }
                )
            }
        }

        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarRailIcon(
                kind = StarRailIconKind.SHIELD,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.global_settings_data_footer),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            SectionHeader(title = title, modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DecorativeLine(modifier = Modifier.weight(1f), isStart = true)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        DecorativeLine(modifier = Modifier.weight(1f), isStart = false)
    }
}

@Composable
private fun DecorativeLine(modifier: Modifier = Modifier, isStart: Boolean) {
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    Canvas(modifier = modifier.height(12.dp)) {
        val centerY = size.height / 2
        val diamondSize = 6.dp.toPx()
        
        // Draw line
        drawLine(
            color = lineColor,
            start = Offset(if (isStart) 0f else diamondSize, centerY),
            end = Offset(if (isStart) size.width - diamondSize else size.width, centerY),
            strokeWidth = 1.dp.toPx()
        )
        
        // Draw diamond at the end/start
        val diamondX = if (isStart) size.width - diamondSize / 2 else diamondSize / 2
        val path = Path().apply {
            moveTo(diamondX, centerY - diamondSize / 2)
            lineTo(diamondX + diamondSize / 2, centerY)
            lineTo(diamondX, centerY + diamondSize / 2)
            lineTo(diamondX - diamondSize / 2, centerY)
            close()
        }
        drawPath(path, color = lineColor)
    }
}

@Composable
private fun SettingsItemRow(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        content()
    }
}

@Composable
private fun ThresholdDropdown(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    // Simple mock dropdown for now as requested "just interface"
    Surface(
        onClick = { onValueChange(value) }, // Just for reactivity demonstration if needed
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.global_settings_messages_unit, value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            StarRailIcon(
                kind = StarRailIconKind.CHEVRON_RIGHT, // Chevron down would be better if available
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
)

@Composable
private fun SecondaryActionButton(
    onClick: () -> Unit,
    icon: StarRailIconKind,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarRailIcon(
                kind = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PrimaryGradientButton(
    text: String,
    icon: StarRailIconKind,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF6B8EFF),
            Color(0xFF4A67EB)
        )
    )
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRailIcon(
                    kind = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OutlinedActionButton(
    text: String,
    icon: StarRailIconKind,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRailIcon(
                    kind = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ProfileScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        ProfileScreen(
            state = ProfileUiState(
                isLoaded = true
            ),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ProfileScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        ProfileScreen(
            state = ProfileUiState(
                isLoaded = true
            ),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onAction = {}
        )
    }
}
