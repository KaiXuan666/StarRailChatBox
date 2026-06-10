package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.attachments_title
import starrailchatbox.shared.generated.resources.close
import starrailchatbox.shared.generated.resources.open_file

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageAttachments(
    attachments: List<MessageAttachment>,
    onOpenAttachment: (MessageAttachment) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return

    FlowRow(
        modifier = modifier.padding(StarRailSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
    ) {
        attachments.forEach { attachment ->
            val isImage = attachment.mimeType.startsWith("image/")
            val isSingle = attachments.size == 1
            Box(
                modifier = Modifier
                    .then(
                        if (isSingle && isImage) Modifier.widthIn(max = 240.dp).aspectRatio(16f / 9f)
                        else Modifier.size(if (compact) 80.dp else 100.dp)
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { onOpenAttachment(attachment) },
                contentAlignment = Alignment.Center
            ) {
                if (isImage) {
                    AsyncImage(
                        model = attachment.uri,
                        contentDescription = attachment.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(StarRailSpacing.xs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        StarRailIcon(
                            kind = getIconForMimeType(attachment.mimeType),
                            contentDescription = attachment.name,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (compact) 32.dp else 40.dp)
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
        }
    }
}

@Composable
fun AttachmentsDialog(
    attachments: List<MessageAttachment>,
    onDismissRequest: () -> Unit,
    onOpenAttachment: (MessageAttachment) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.attachments_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                attachments.forEach { attachment ->
                        Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onOpenAttachment(attachment) }
                            .padding(StarRailSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            StarRailIcon(
                                kind = getIconForMimeType(attachment.mimeType),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attachment.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = attachment.mimeType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onOpenAttachment(attachment) }) {
                            Text(stringResource(Res.string.open_file))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text(stringResource(Res.string.close))
            }
        }
    )
}

@Composable
fun FullScreenImagePreview(
    uri: String,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
            onClick = onDismiss
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                if (onDownload != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(StarRailSpacing.lg)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onDownload() },
                        contentAlignment = Alignment.Center
                    ) {
                        StarRailIcon(
                            kind = StarRailIconKind.DOWNLOAD,
                            contentDescription = "下载",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
