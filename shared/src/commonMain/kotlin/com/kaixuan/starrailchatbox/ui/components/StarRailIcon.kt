package com.kaixuan.starrailchatbox.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class StarRailIconKind {
    VOICE,
    PROFILE,
    SETTINGS,
    HEART,
    CHAT,
    SPARKLE,
    MOON,
    ADD,
    SMILE,
    SEND,
    MICROPHONE,
    CONVERSATION,
    PERSON,
    COMPASS,
    CHECK,
    CUBE,
    UPDATE,
    BELL,
    PALETTE,
    INFO,
    SHIELD,
    CHEVRON_RIGHT,
    GLOBE,
    KEY,
    EYE_VISIBLE,
    EYE_HIDDEN,
    CHEVRON_LEFT,
    ARROW_UP,
    DELETE,
    EDIT,
    FILE,
    CAMERA,
    GALLERY,
}

@Composable
fun StarRailIcon(
    kind: StarRailIconKind,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val semanticsModifier = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics {
            this.contentDescription = contentDescription
        }
    }

    Canvas(semanticsModifier) {
        val side = min(size.width, size.height)
        val strokeWidth = side * 0.085f
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)

        when (kind) {
            StarRailIconKind.VOICE -> {
                listOf(
                    0.2f to 0.3f,
                    0.35f to 0.18f,
                    0.5f to 0.1f,
                    0.65f to 0.2f,
                    0.8f to 0.34f,
                ).forEach { (x, top) ->
                    drawLine(
                        color = tint,
                        start = point(x, top),
                        end = point(x, 1f - top),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            StarRailIconKind.PROFILE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(0.2f, 0.14f),
                    size = Size(size.width * 0.58f, size.height * 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        side * 0.06f,
                    ),
                    style = stroke,
                )
                drawLine(tint, point(0.34f, 0.38f), point(0.64f, 0.38f), strokeWidth)
                drawLine(tint, point(0.34f, 0.56f), point(0.58f, 0.56f), strokeWidth)
                drawLine(tint, point(0.72f, 0.74f), point(0.88f, 0.9f), strokeWidth)
            }

            StarRailIconKind.SETTINGS -> {
                drawCircle(
                    color = tint,
                    radius = side * 0.18f,
                    center = point(0.5f, 0.5f),
                    style = stroke,
                )
                repeat(8) { index ->
                    val angle = index * (kotlin.math.PI / 4).toFloat()
                    val inner = Offset(
                        point(0.5f, 0.5f).x + cos(angle) * side * 0.3f,
                        point(0.5f, 0.5f).y + sin(angle) * side * 0.3f,
                    )
                    val outer = Offset(
                        point(0.5f, 0.5f).x + cos(angle) * side * 0.43f,
                        point(0.5f, 0.5f).y + sin(angle) * side * 0.43f,
                    )
                    drawLine(tint, inner, outer, strokeWidth, StrokeCap.Round)
                }
            }

            StarRailIconKind.HEART -> {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.84f)
                    cubicTo(
                        size.width * 0.36f,
                        size.height * 0.7f,
                        size.width * 0.12f,
                        size.height * 0.55f,
                        size.width * 0.18f,
                        size.height * 0.3f,
                    )
                    cubicTo(
                        size.width * 0.23f,
                        size.height * 0.1f,
                        size.width * 0.43f,
                        size.height * 0.14f,
                        size.width * 0.5f,
                        size.height * 0.28f,
                    )
                    cubicTo(
                        size.width * 0.57f,
                        size.height * 0.14f,
                        size.width * 0.77f,
                        size.height * 0.1f,
                        size.width * 0.82f,
                        size.height * 0.3f,
                    )
                    cubicTo(
                        size.width * 0.88f,
                        size.height * 0.55f,
                        size.width * 0.64f,
                        size.height * 0.7f,
                        size.width * 0.5f,
                        size.height * 0.84f,
                    )
                }
                drawPath(path, tint, style = stroke)
            }

            StarRailIconKind.CHAT -> {
                drawCircle(tint, side * 0.37f, point(0.5f, 0.46f), style = stroke)
                drawLine(tint, point(0.63f, 0.75f), point(0.76f, 0.86f), strokeWidth)
                listOf(0.36f, 0.5f, 0.64f).forEach {
                    drawCircle(tint, side * 0.035f, point(it, 0.46f))
                }
            }

            StarRailIconKind.SPARKLE -> drawSparkle(tint, point(0.5f, 0.5f), side * 0.42f)

            StarRailIconKind.MOON -> {
                drawArc(
                    color = tint,
                    startAngle = 70f,
                    sweepAngle = 250f,
                    useCenter = false,
                    topLeft = point(0.18f, 0.12f),
                    size = Size(side * 0.7f, side * 0.76f),
                    style = stroke,
                )
                drawArc(
                    color = tint,
                    startAngle = 105f,
                    sweepAngle = 190f,
                    useCenter = false,
                    topLeft = point(0.38f, 0.12f),
                    size = Size(side * 0.48f, side * 0.64f),
                    style = stroke,
                )
            }

            StarRailIconKind.ADD -> {
                drawLine(tint, point(0.5f, 0.18f), point(0.5f, 0.82f), strokeWidth)
                drawLine(tint, point(0.18f, 0.5f), point(0.82f, 0.5f), strokeWidth)
            }

            StarRailIconKind.SMILE -> {
                drawCircle(tint, side * 0.4f, point(0.5f, 0.5f), style = stroke)
                drawCircle(tint, side * 0.045f, point(0.36f, 0.42f))
                drawCircle(tint, side * 0.045f, point(0.64f, 0.42f))
                drawArc(
                    color = tint,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = point(0.3f, 0.43f),
                    size = Size(side * 0.4f, side * 0.32f),
                    style = stroke,
                )
            }

            StarRailIconKind.SEND -> {
                val path = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.48f)
                    lineTo(size.width * 0.88f, size.height * 0.15f)
                    lineTo(size.width * 0.66f, size.height * 0.87f)
                    lineTo(size.width * 0.46f, size.height * 0.61f)
                    close()
                }
                drawPath(path, tint)
                drawLine(tint, point(0.46f, 0.61f), point(0.66f, 0.44f), strokeWidth * 0.65f)
            }

            StarRailIconKind.MICROPHONE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(0.37f, 0.12f),
                    size = Size(size.width * 0.26f, size.height * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(side * 0.13f),
                    style = stroke,
                )
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = point(0.24f, 0.32f),
                    size = Size(side * 0.52f, side * 0.42f),
                    style = stroke,
                )
                drawLine(tint, point(0.5f, 0.74f), point(0.5f, 0.9f), strokeWidth)
                drawLine(tint, point(0.34f, 0.9f), point(0.66f, 0.9f), strokeWidth)
            }

            StarRailIconKind.CONVERSATION -> {
                drawCircle(tint, side * 0.38f, point(0.5f, 0.5f), style = stroke)
                listOf(0.34f, 0.5f, 0.66f).forEach {
                    drawCircle(tint, side * 0.045f, point(it, 0.5f))
                }
            }

            StarRailIconKind.PERSON -> {
                drawCircle(tint, side * 0.18f, point(0.5f, 0.3f), style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = point(0.18f, 0.48f),
                    size = Size(side * 0.64f, side * 0.48f),
                    style = stroke,
                )
            }

            StarRailIconKind.COMPASS -> {
                drawCircle(tint, side * 0.39f, point(0.5f, 0.5f), style = stroke)
                val path = Path().apply {
                    moveTo(size.width * 0.62f, size.height * 0.26f)
                    lineTo(size.width * 0.53f, size.height * 0.57f)
                    lineTo(size.width * 0.27f, size.height * 0.72f)
                    lineTo(size.width * 0.38f, size.height * 0.4f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }

            StarRailIconKind.CHECK -> {
                drawLine(tint, point(0.18f, 0.52f), point(0.42f, 0.76f), strokeWidth)
                drawLine(tint, point(0.42f, 0.76f), point(0.84f, 0.24f), strokeWidth)
            }

            StarRailIconKind.CUBE -> {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.18f)
                    lineTo(size.width * 0.82f, size.height * 0.34f)
                    lineTo(size.width * 0.82f, size.height * 0.66f)
                    lineTo(size.width * 0.5f, size.height * 0.82f)
                    lineTo(size.width * 0.18f, size.height * 0.66f)
                    lineTo(size.width * 0.18f, size.height * 0.34f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, point(0.5f, 0.5f), point(0.5f, 0.82f), strokeWidth)
                drawLine(tint, point(0.5f, 0.5f), point(0.18f, 0.34f), strokeWidth)
                drawLine(tint, point(0.5f, 0.5f), point(0.82f, 0.34f), strokeWidth)
            }

            StarRailIconKind.UPDATE -> {
                drawArc(
                    color = tint,
                    startAngle = -30f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = point(0.2f, 0.2f),
                    size = Size(side * 0.6f, side * 0.6f),
                    style = stroke,
                )
                val path = Path().apply {
                    moveTo(size.width * 0.7f, size.height * 0.14f)
                    lineTo(size.width * 0.82f, size.height * 0.34f)
                    lineTo(size.width * 0.58f, size.height * 0.38f)
                }
                drawPath(path, tint, style = stroke)
            }

            StarRailIconKind.BELL -> {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.14f)
                    cubicTo(
                        size.width * 0.46f, size.height * 0.14f,
                        size.width * 0.46f, size.height * 0.22f,
                        size.width * 0.5f, size.height * 0.22f
                    )
                    lineTo(size.width * 0.5f, size.height * 0.26f)
                    cubicTo(
                        size.width * 0.34f, size.height * 0.3f,
                        size.width * 0.26f, size.height * 0.5f,
                        size.width * 0.22f, size.height * 0.72f
                    )
                    lineTo(size.width * 0.78f, size.height * 0.72f)
                    cubicTo(
                        size.width * 0.74f, size.height * 0.5f,
                        size.width * 0.66f, size.height * 0.3f,
                        size.width * 0.5f, size.height * 0.26f
                    )
                }
                drawPath(path, tint, style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = point(0.42f, 0.72f),
                    size = Size(side * 0.16f, side * 0.12f),
                    style = stroke
                )
                drawLine(tint, point(0.16f, 0.72f), point(0.84f, 0.72f), strokeWidth)
            }

            StarRailIconKind.PALETTE -> {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.16f)
                    cubicTo(
                        size.width * 0.85f, size.height * 0.16f,
                        size.width * 0.9f, size.height * 0.65f,
                        size.width * 0.75f, size.height * 0.82f
                    )
                    cubicTo(
                        size.width * 0.65f, size.height * 0.94f,
                        size.width * 0.35f, size.height * 0.94f,
                        size.width * 0.2f, size.height * 0.78f
                    )
                    cubicTo(
                        size.width * 0.05f, size.height * 0.62f,
                        size.width * 0.15f, size.height * 0.16f,
                        size.width * 0.5f, size.height * 0.16f
                    )
                }
                drawPath(path, tint, style = stroke)
                drawCircle(tint, side * 0.06f, point(0.35f, 0.72f), style = stroke)
                drawCircle(tint, side * 0.045f, point(0.45f, 0.34f))
                drawCircle(tint, side * 0.045f, point(0.68f, 0.38f))
                drawCircle(tint, side * 0.045f, point(0.68f, 0.62f))
                drawCircle(tint, side * 0.045f, point(0.52f, 0.54f))
            }

            StarRailIconKind.INFO -> {
                drawCircle(tint, side * 0.38f, point(0.5f, 0.5f), style = stroke)
                drawCircle(tint, side * 0.04f, point(0.5f, 0.34f))
                drawLine(tint, point(0.5f, 0.46f), point(0.5f, 0.7f), strokeWidth)
            }

            StarRailIconKind.SHIELD -> {
                val path = Path().apply {
                    moveTo(size.width * 0.24f, size.height * 0.24f)
                    lineTo(size.width * 0.76f, size.height * 0.24f)
                    lineTo(size.width * 0.76f, size.height * 0.54f)
                    cubicTo(
                        size.width * 0.76f, size.height * 0.72f,
                        size.width * 0.62f, size.height * 0.84f,
                        size.width * 0.5f, size.height * 0.88f
                    )
                    cubicTo(
                        size.width * 0.38f, size.height * 0.84f,
                        size.width * 0.24f, size.height * 0.72f,
                        size.width * 0.24f, size.height * 0.54f
                    )
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, point(0.42f, 0.5f), point(0.58f, 0.5f), strokeWidth)
                drawLine(tint, point(0.5f, 0.42f), point(0.5f, 0.58f), strokeWidth)
            }

            StarRailIconKind.CHEVRON_RIGHT -> {
                val path = Path().apply {
                    moveTo(size.width * 0.42f, size.height * 0.32f)
                    lineTo(size.width * 0.6f, size.height * 0.5f)
                    lineTo(size.width * 0.42f, size.height * 0.68f)
                }
                drawPath(path, tint, style = stroke)
            }

            StarRailIconKind.GLOBE -> {
                drawCircle(tint, side * 0.38f, point(0.5f, 0.5f), style = stroke)
                drawLine(tint, point(0.12f, 0.5f), point(0.88f, 0.5f), strokeWidth)
                drawLine(tint, point(0.5f, 0.12f), point(0.5f, 0.88f), strokeWidth)
                drawOval(
                    color = tint,
                    topLeft = point(0.32f, 0.12f),
                    size = Size(side * 0.36f, side * 0.76f),
                    style = stroke
                )
            }

            StarRailIconKind.KEY -> {
                drawCircle(tint, side * 0.15f, point(0.35f, 0.35f), style = stroke)
                drawLine(tint, point(0.45f, 0.45f), point(0.8f, 0.8f), strokeWidth)
                drawLine(tint, point(0.65f, 0.65f), point(0.73f, 0.57f), strokeWidth)
                drawLine(tint, point(0.73f, 0.73f), point(0.81f, 0.65f), strokeWidth)
            }

            StarRailIconKind.EYE_VISIBLE -> {
                val path = Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.5f)
                    quadraticTo(size.width * 0.5f, size.height * 0.22f, size.width * 0.82f, size.height * 0.5f)
                    quadraticTo(size.width * 0.5f, size.height * 0.78f, size.width * 0.18f, size.height * 0.5f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawCircle(tint, side * 0.12f, point(0.5f, 0.5f))
            }

            StarRailIconKind.EYE_HIDDEN -> {
                val path = Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.5f)
                    quadraticTo(size.width * 0.5f, size.height * 0.22f, size.width * 0.82f, size.height * 0.5f)
                    quadraticTo(size.width * 0.5f, size.height * 0.78f, size.width * 0.18f, size.height * 0.5f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawCircle(tint, side * 0.12f, point(0.5f, 0.5f))
                drawLine(tint, point(0.25f, 0.25f), point(0.75f, 0.75f), strokeWidth)
            }

            StarRailIconKind.CHEVRON_LEFT -> {
                val path = Path().apply {
                    moveTo(size.width * 0.58f, size.height * 0.32f)
                    lineTo(size.width * 0.4f, size.height * 0.5f)
                    lineTo(size.width * 0.58f, size.height * 0.68f)
                }
                drawPath(path, tint, style = stroke)
            }

            StarRailIconKind.ARROW_UP -> {
                drawLine(tint, point(0.5f, 0.22f), point(0.5f, 0.78f), strokeWidth)
                drawLine(tint, point(0.5f, 0.22f), point(0.26f, 0.46f), strokeWidth)
                drawLine(tint, point(0.5f, 0.22f), point(0.74f, 0.46f), strokeWidth)
            }

            StarRailIconKind.DELETE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(0.27f, 0.3f),
                    size = Size(size.width * 0.46f, size.height * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(side * 0.05f),
                    style = stroke,
                )
                drawLine(tint, point(0.2f, 0.3f), point(0.8f, 0.3f), strokeWidth)
                drawLine(tint, point(0.38f, 0.18f), point(0.62f, 0.18f), strokeWidth)
                drawLine(tint, point(0.41f, 0.43f), point(0.41f, 0.72f), strokeWidth)
                drawLine(tint, point(0.59f, 0.43f), point(0.59f, 0.72f), strokeWidth)
            }

            StarRailIconKind.EDIT -> {
                val path = Path().apply {
                    moveTo(size.width * 0.73f, size.height * 0.15f)
                    lineTo(size.width * 0.85f, size.height * 0.27f)
                    lineTo(size.width * 0.42f, size.height * 0.7f)
                    lineTo(size.width * 0.22f, size.height * 0.78f)
                    lineTo(size.width * 0.3f, size.height * 0.58f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, point(0.63f, 0.25f), point(0.75f, 0.37f), strokeWidth)
            }

            StarRailIconKind.FILE -> {
                val path = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.15f)
                    lineTo(size.width * 0.6f, size.height * 0.15f)
                    lineTo(size.width * 0.75f, size.height * 0.3f)
                    lineTo(size.width * 0.75f, size.height * 0.85f)
                    lineTo(size.width * 0.25f, size.height * 0.85f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, point(0.6f, 0.15f), point(0.6f, 0.3f), strokeWidth)
                drawLine(tint, point(0.6f, 0.3f), point(0.75f, 0.3f), strokeWidth)
            }

            StarRailIconKind.CAMERA -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(0.18f, 0.28f),
                    size = Size(size.width * 0.64f, size.height * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(side * 0.08f),
                    style = stroke,
                )
                drawCircle(tint, side * 0.14f, point(0.5f, 0.54f), style = stroke)
                drawRect(
                    color = tint,
                    topLeft = point(0.38f, 0.2f),
                    size = Size(size.width * 0.24f, size.height * 0.08f),
                    style = stroke,
                )
            }

            StarRailIconKind.GALLERY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(0.18f, 0.22f),
                    size = Size(size.width * 0.64f, size.height * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(side * 0.06f),
                    style = stroke,
                )
                drawCircle(tint, side * 0.06f, point(0.32f, 0.38f))
                val path = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.72f)
                    lineTo(size.width * 0.42f, size.height * 0.42f)
                    lineTo(size.width * 0.58f, size.height * 0.62f)
                    lineTo(size.width * 0.68f, size.height * 0.48f)
                    lineTo(size.width * 0.78f, size.height * 0.72f)
                }
                drawPath(path, tint, style = stroke)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkle(
    color: Color,
    center: Offset,
    radius: Float,
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.2f, center.y - radius * 0.2f)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + radius * 0.2f, center.y + radius * 0.2f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.2f, center.y + radius * 0.2f)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - radius * 0.2f, center.y - radius * 0.2f)
        close()
    }
    drawPath(path, color)
}
