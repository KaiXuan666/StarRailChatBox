package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.release_to_cancel
import starrailchatbox.shared.generated.resources.release_to_send

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceMessageBubble(
    durationMs: Long,
    compact: Boolean,
    isSent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val durationSec = (durationMs / 1000).coerceAtLeast(1)
    // 根据时长调整气泡宽度，模仿语音气泡效果
    val minWidth = if (compact) 80.dp else 100.dp
    val maxWidth = if (compact) 160.dp else 200.dp
    val bubbleWidth = (minWidth + (maxWidth - minWidth) * (durationSec.toFloat() / 60f).coerceAtMost(1f))

    val backgroundColor = if (isSent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (isSent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isSent) {
        MaterialTheme.starRailColors.sentBubbleBorder
    } else {
        MaterialTheme.starRailColors.receivedBubbleBorder
    }

    // 播放时的透明度微动画（0.4f -> 1.0f 呼吸渐变效果）
    val infiniteTransition = rememberInfiniteTransition()
    val animatedAlpha by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
        modifier = modifier
            .width(bubbleWidth)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else StarRailSpacing.md,
                vertical = if (compact) 8.dp else StarRailSpacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
        ) {
            if (isSent) {
                Text(
                    text = "${durationSec}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
                Spacer(Modifier.width(StarRailSpacing.xs))
                StarRailIcon(
                    kind = StarRailIconKind.VOICE_WAVE,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(if (compact) 18.dp else 22.dp)
                        .graphicsLayer { 
                            rotationY = 180f 
                            alpha = animatedAlpha
                        }
                )
            } else {
                StarRailIcon(
                    kind = StarRailIconKind.VOICE_WAVE,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(if (compact) 18.dp else 22.dp)
                        .graphicsLayer {
                            alpha = animatedAlpha
                        }
                )
                Spacer(Modifier.width(StarRailSpacing.xs))
                Text(
                    text = "${durationSec}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun RecordingOverlay(
    isCancelTargeted: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isCancelTargeted) {
        Brush.verticalGradient(
            listOf(
                Color(0xFFCC4141).copy(alpha = 0.9f),
                Color(0xFFCC4141).copy(alpha = 0.5f),
                Color.Transparent
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF418FCC).copy(alpha = 0.9f),
                Color(0xFF418FCC).copy(alpha = 0.5f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 160.dp)
        ) {
            Text(
                text = stringResource(
                    if (isCancelTargeted) Res.string.release_to_cancel else Res.string.release_to_send
                ),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(32.dp))
            // Waveform (Simulated)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(30) { index ->
                    // 模拟波形高度
                    val height = when (index % 5) {
                        0 -> 12.dp
                        1 -> 24.dp
                        2 -> 38.dp
                        3 -> 28.dp
                        else -> 16.dp
                    }
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height)
                            .background(
                                if (isCancelTargeted) Color(0xFFFF9999) else Color.White,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
