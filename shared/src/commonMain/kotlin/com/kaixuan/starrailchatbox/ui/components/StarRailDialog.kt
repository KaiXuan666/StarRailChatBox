package com.kaixuan.starrailchatbox.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors

@Composable
fun StarRailDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    onDismissRequest: () -> Unit = {},
    onDismissButton: (() -> Unit)? = null,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    destructive: Boolean = false,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.starRailColors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StarRailIcon(
                            kind = StarRailIconKind.SPARKLE,
                            contentDescription = null,
                            tint = colors.warmSparkle,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        colors.constellation.copy(alpha = 0.45f),
                                        colors.constellationMuted.copy(alpha = 0.1f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                }

                content()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (neutralText != null && onNeutral != null) {
                        DialogActionButton(
                            text = neutralText,
                            onClick = onNeutral,
                            primary = false,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    if (dismissText != null) {
                        DialogActionButton(
                            text = dismissText,
                            onClick = onDismissButton ?: onDismissRequest,
                            primary = false,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    DialogActionButton(
                        text = confirmText,
                        onClick = onConfirm,
                        primary = true,
                        destructive = destructive,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogActionButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    destructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f)
    val gradient = when {
        destructive -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.error.copy(alpha = 0.82f),
            ),
        )
        primary -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
            ),
        )
        else -> null
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .width(96.dp)
            .height(38.dp)
            .scale(scale),
        shape = RoundedCornerShape(50),
        color = if (gradient == null) {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
        } else {
            Color.Transparent
        },
        border = if (gradient == null) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
        } else {
            null
        },
    ) {
        Box(
            modifier = if (gradient == null) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxSize().background(gradient)
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = when {
                    destructive -> MaterialTheme.colorScheme.onError
                    primary -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun StarRailDialogLightPreview() {
    StarRailDialogPreview(darkTheme = false)
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun StarRailDialogDarkPreview() {
    StarRailDialogPreview(darkTheme = true)
}

@Composable
private fun StarRailDialogPreview(darkTheme: Boolean) {
    StarRailTheme(darkThemeOverride = darkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            StarRailDialog(
                title = "删除对话？",
                dismissText = "取消",
                confirmText = "删除",
                onDismissRequest = {},
                onConfirm = {},
                destructive = true,
            ) {
                Text(
                    text = "确定删除这段对话吗？此操作无法撤销。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
