package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.attach_camera
import starrailchatbox.shared.generated.resources.attach_file
import starrailchatbox.shared.generated.resources.attach_gallery

@Composable
fun AttachmentPanel(
    compact: Boolean,
    onAction: (ComposerAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (compact) StarRailSpacing.md else StarRailSpacing.lg,
                end = if (compact) StarRailSpacing.md else StarRailSpacing.lg,
                bottom = if (compact) StarRailSpacing.md else StarRailSpacing.lg,
                top = StarRailSpacing.xxs,
            )
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.lg else StarRailSpacing.xl
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AttachmentItem(
            icon = StarRailIconKind.FILE,
            label = stringResource(Res.string.attach_file),
            compact = compact,
            onClick = { onAction(ComposerAction.PICK_FILE) }
        )
        AttachmentItem(
            icon = StarRailIconKind.CAMERA,
            label = stringResource(Res.string.attach_camera),
            compact = compact,
            onClick = { onAction(ComposerAction.TAKE_PHOTO) }
        )
        AttachmentItem(
            icon = StarRailIconKind.GALLERY,
            label = stringResource(Res.string.attach_gallery),
            compact = compact,
            onClick = { onAction(ComposerAction.PICK_IMAGE) }
        )
    }
}

@Composable
fun AttachmentItem(
    icon: StarRailIconKind,
    label: String,
    compact: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(StarRailSpacing.xs)
    ) {
        Surface(
            modifier = Modifier.size(if (compact) 52.dp else 60.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                StarRailIcon(
                    kind = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(if (compact) 26.dp else 30.dp)
                )
            }
        }
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SelectedAttachmentsArea(
    attachments: List<SelectedAttachment>,
    compact: Boolean,
    onAddClicked: () -> Unit,
    onRemoveClicked: (SelectedAttachment) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                top = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                bottom = StarRailSpacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        attachments.forEach { attachment ->
            AttachmentPreviewItem(
                attachment = attachment,
                compact = compact,
                onRemove = { onRemoveClicked(attachment) }
            )
        }
    }
}

@Composable
fun AttachmentPreviewItem(
    attachment: SelectedAttachment,
    compact: Boolean,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(if (compact) 72.dp else 84.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        when (attachment) {
            is SelectedAttachment.Image -> {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            is SelectedAttachment.File -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(StarRailSpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.FILE,
                        contentDescription = attachment.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (compact) 28.dp else 32.dp)
                    )
                    Text(
                        text = attachment.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
            is SelectedAttachment.Voice -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(StarRailSpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.MICROPHONE,
                        contentDescription = attachment.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (compact) 28.dp else 32.dp)
                    )
                    Text(
                        text = attachment.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }

        Surface(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(if (compact) 18.dp else 20.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ) {
            Box(contentAlignment = Alignment.Center) {
                StarRailIcon(
                    kind = StarRailIconKind.CLOSE,
                    contentDescription = "移除",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(if (compact) 12.dp else 14.dp)
                )
            }
        }
    }
}
