@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.kaixuan.starrailchatbox.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.settings_profile_title
import starrailchatbox.shared.generated.resources.profile_avatar_title
import starrailchatbox.shared.generated.resources.profile_nickname_title
import starrailchatbox.shared.generated.resources.profile_save_btn
import starrailchatbox.shared.generated.resources.profile_restore_default
import starrailchatbox.shared.generated.resources.settings_saving
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.platform.rememberImagePicker
import kotlin.io.encoding.Base64

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
    val imagePicker = rememberImagePicker { bytes ->
        if (bytes != null) {
            onAction(ProfileAction.AvatarChanged(bytes))
        }
    }

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
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xl)
    ) {
        // 1. Back Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { onMainAction(MainAction.PopBackStack) },
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StarRailIcon(
                        kind = StarRailIconKind.CHEVRON_LEFT,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = stringResource(Res.string.settings_profile_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = if (compact) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // 2. Avatar Display & Action Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Outer glowing circle enclosing the avatar
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .border(2.dp, colors.constellation.copy(alpha = 0.55f), CircleShape)
                        .padding(8.dp)
                        .border(1.dp, colors.constellationMuted.copy(alpha = 0.25f), CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.customAvatarBase64 != null) {
                        val bitmap = remember(state.customAvatarBase64) {
                            try {
                                val bytes = Base64.decode(state.customAvatarBase64)
                                bytes.decodeToImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            StarRailIcon(
                                kind = StarRailIconKind.SPARKLE,
                                contentDescription = null,
                                tint = colors.warmSparkle,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    } else {
                        // Default fallback avatar is StarRailIconKind.SPARKLE
                        StarRailIcon(
                            kind = StarRailIconKind.SPARKLE,
                            contentDescription = null,
                            tint = colors.warmSparkle,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Avatar action row (Choose avatar + restore default)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { imagePicker() },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StarRailIcon(
                                kind = StarRailIconKind.PERSON,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(Res.string.profile_avatar_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (state.customAvatarBase64 != null) {
                        Text(
                            text = stringResource(Res.string.profile_restore_default),
                            modifier = Modifier.clickable {
                                onAction(ProfileAction.RestoreDefaultAvatar)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. Nickname Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.profile_nickname_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                BasicTextField(
                    value = state.nickname,
                    onValueChange = { onAction(ProfileAction.NicknameChanged(it)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            innerTextField()
                        }
                    }
                )
            }
        }

        // 4. Save Button
        val saveGrad = Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
            )
        )
        Surface(
            onClick = { onAction(ProfileAction.SaveClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(50),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(saveGrad)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.isSaving) stringResource(Res.string.settings_saving) else stringResource(Res.string.profile_save_btn),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
