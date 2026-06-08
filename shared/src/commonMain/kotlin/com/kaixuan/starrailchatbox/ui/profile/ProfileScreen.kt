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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import starrailchatbox.shared.generated.resources.profile_unsaved_changes_title
import starrailchatbox.shared.generated.resources.profile_unsaved_changes_message
import starrailchatbox.shared.generated.resources.profile_unsaved_changes_save
import starrailchatbox.shared.generated.resources.profile_unsaved_changes_discard
import starrailchatbox.shared.generated.resources.profile_unsaved_changes_cancel
import starrailchatbox.shared.generated.resources.settings_saving
import starrailchatbox.shared.generated.resources.navigation_back
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.components.StarRailPrimaryButton
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.AvatarImage
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
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
    val imagePicker = rememberImagePicker { image ->
        if (image != null) {
            onAction(ProfileAction.AvatarChanged(image.uri))
        }
    }

    BackHandler {
        onAction(ProfileAction.BackClicked)
    }

    if (state.isDiscardDialogOpen) {
        StarRailDialog(
            title = stringResource(Res.string.profile_unsaved_changes_title),
            dismissText = stringResource(Res.string.profile_unsaved_changes_cancel),
            confirmText = stringResource(Res.string.profile_unsaved_changes_save),
            neutralText = stringResource(Res.string.profile_unsaved_changes_discard),
            onDismissRequest = { onAction(ProfileAction.CancelDiscard) },
            onConfirm = { onAction(ProfileAction.SaveClicked) },
            onNeutral = { onAction(ProfileAction.ConfirmDiscard) },
        ) {
            Text(
                text = stringResource(Res.string.profile_unsaved_changes_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    StarRailPageLayout(
        title = stringResource(Res.string.settings_profile_title),
        contentPadding = contentPadding,
        compact = compact,
        backContentDescription = stringResource(Res.string.navigation_back),
        onBackClick = { onAction(ProfileAction.BackClicked) },
        modifier = modifier,
    ) {
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
                    AvatarImage(
                        avatarUri = state.customAvatarUri.orEmpty(),
                        contentDescription = null,
                        placeholderKind = StarRailIconKind.SPARKLE,
                        placeholderSize = 64.dp,
                        modifier = Modifier.fillMaxSize(),
                        isUser = true,
                    )
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

                    if (state.customAvatarUri != null) {
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

        StarRailPrimaryButton(
            text = if (state.isSaving) {
                stringResource(Res.string.settings_saving)
            } else {
                stringResource(Res.string.profile_save_btn)
            },
            onClick = { onAction(ProfileAction.SaveClicked) },
            enabled = !state.isSaving,
        )
    }
}
