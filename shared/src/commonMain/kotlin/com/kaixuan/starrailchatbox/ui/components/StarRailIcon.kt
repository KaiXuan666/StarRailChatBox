package com.kaixuan.starrailchatbox.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private const val RetryStartAngle = 35f
private const val RetrySweepAngle = 285f
private const val RetryEndCos = 0.76604444f
private const val RetryEndSin = -0.64278764f
private const val RetryArrowLeftCos = 0.9612617f
private const val RetryArrowLeftSin = 0.27563736f
private const val RetryArrowRightCos = 0.104528464f
private const val RetryArrowRightSin = 0.9945219f

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
    API_PROVIDER,
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
    CLOSE,
    KEYBOARD,
    VOICE_WAVE,
    RETRY,
    DATABASE,
    DOWNLOAD,
    EXPORT,
    PLAY,
    STOP,
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

    Spacer(
        modifier = semanticsModifier.drawWithCache {
            val side = size.minDimension
            val strokeWidth = side * 0.085f
            val stroke = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            val path = Path()

            fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)

            onDrawBehind {
                when (kind) {
            StarRailIconKind.VOICE -> {
                drawLine(tint, point(0.20f, 0.30f), point(0.20f, 0.70f), strokeWidth, cap = StrokeCap.Round)
                drawLine(tint, point(0.35f, 0.18f), point(0.35f, 0.82f), strokeWidth, cap = StrokeCap.Round)
                drawLine(tint, point(0.50f, 0.10f), point(0.50f, 0.90f), strokeWidth, cap = StrokeCap.Round)
                drawLine(tint, point(0.65f, 0.20f), point(0.65f, 0.80f), strokeWidth, cap = StrokeCap.Round)
                drawLine(tint, point(0.80f, 0.34f), point(0.80f, 0.66f), strokeWidth, cap = StrokeCap.Round)
            }

            StarRailIconKind.VOICE_WAVE -> {
                // Dot
                drawCircle(
                    color = tint,
                    radius = side * 0.06f,
                    center = point(0.3f, 0.5f)
                )
                // First wave
                drawArc(
                    color = tint,
                    startAngle = -60f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = point(0.1f, 0.3f),
                    size = Size(side * 0.4f, side * 0.4f),
                    style = stroke
                )
                // Second wave
                drawArc(
                    color = tint,
                    startAngle = -60f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = point(-0.1f, 0.1f),
                    size = Size(side * 0.8f, side * 0.8f),
                    style = stroke
                )
            }

            StarRailIconKind.PROFILE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(0.2f, 0.14f),
                    size = Size(size.width * 0.58f, size.height * 0.7f),
                    cornerRadius = CornerRadius(
                        side * 0.06f,
                    ),
                    style = stroke,
                )
                drawLine(tint, point(0.34f, 0.38f), point(0.64f, 0.38f), strokeWidth)
                drawLine(tint, point(0.34f, 0.56f), point(0.58f, 0.56f), strokeWidth)
                drawLine(tint, point(0.72f, 0.74f), point(0.88f, 0.9f), strokeWidth)
            }

            StarRailIconKind.SETTINGS -> {
                drawCircle(tint, side * 0.29f, point(0.5f, 0.5f), style = stroke)
                drawCircle(tint, side * 0.1f, point(0.5f, 0.5f), style = stroke)
                drawLine(tint, point(0.5f, 0.08f), point(0.5f, 0.21f), strokeWidth)
                drawLine(tint, point(0.5f, 0.79f), point(0.5f, 0.92f), strokeWidth)
                drawLine(tint, point(0.14f, 0.29f), point(0.25f, 0.36f), strokeWidth)
                drawLine(tint, point(0.75f, 0.64f), point(0.86f, 0.71f), strokeWidth)
                drawLine(tint, point(0.14f, 0.71f), point(0.25f, 0.64f), strokeWidth)
                drawLine(tint, point(0.75f, 0.36f), point(0.86f, 0.29f), strokeWidth)
            }

            StarRailIconKind.HEART -> {
                path.reset()

                path.apply {
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
                drawCircle(tint, side * 0.035f, point(0.36f, 0.46f))
                drawCircle(tint, side * 0.035f, point(0.50f, 0.46f))
                drawCircle(tint, side * 0.035f, point(0.64f, 0.46f))
            }

            StarRailIconKind.SPARKLE -> drawSparkle(path, tint, point(0.5f, 0.5f), side * 0.42f)

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
                path.reset()

                path.apply {
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
                    cornerRadius = CornerRadius(side * 0.13f),
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
                drawCircle(tint, side * 0.045f, point(0.34f, 0.5f))
                drawCircle(tint, side * 0.045f, point(0.50f, 0.5f))
                drawCircle(tint, side * 0.045f, point(0.66f, 0.5f))
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
                path.reset()

                path.apply {
                    moveTo(size.width * 0.62f, size.height * 0.26f)
                    lineTo(size.width * 0.53f, size.height * 0.57f)
                    lineTo(size.width * 0.27f, size.height * 0.72f)
                    lineTo(size.width * 0.38f, size.height * 0.4f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }

            StarRailIconKind.API_PROVIDER -> {
                drawLine(tint, point(0.28f, 0.32f), point(0.72f, 0.32f), strokeWidth)
                drawLine(tint, point(0.28f, 0.32f), point(0.5f, 0.72f), strokeWidth)
                drawLine(tint, point(0.72f, 0.32f), point(0.5f, 0.72f), strokeWidth)
                drawCircle(tint, side * 0.12f, point(0.28f, 0.32f), style = stroke)
                drawCircle(tint, side * 0.12f, point(0.72f, 0.32f), style = stroke)
                drawCircle(tint, side * 0.12f, point(0.5f, 0.72f), style = stroke)
            }

            StarRailIconKind.CHECK -> {
                drawLine(tint, point(0.18f, 0.52f), point(0.42f, 0.76f), strokeWidth)
                drawLine(tint, point(0.42f, 0.76f), point(0.84f, 0.24f), strokeWidth)
            }

            StarRailIconKind.CUBE -> {
                path.reset()

                path.apply {
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
                // 极简更新设计：圆环中心带有一个向上的箭头，象征升级与更新，风格统一
                drawCircle(tint, side * 0.38f, point(0.5f, 0.5f), style = stroke)
                drawLine(tint, point(0.5f, 0.68f), point(0.5f, 0.32f), strokeWidth)
                drawLine(tint, point(0.36f, 0.46f), point(0.5f, 0.32f), strokeWidth)
                drawLine(tint, point(0.64f, 0.46f), point(0.5f, 0.32f), strokeWidth)
            }

            StarRailIconKind.BELL -> {
                path.reset()

                path.apply {
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
                path.reset()

                path.apply {
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
                path.reset()

                path.apply {
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
                path.reset()

                path.apply {
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
                path.reset()

                path.apply {
                    moveTo(size.width * 0.18f, size.height * 0.5f)
                    quadraticTo(size.width * 0.5f, size.height * 0.22f, size.width * 0.82f, size.height * 0.5f)
                    quadraticTo(size.width * 0.5f, size.height * 0.78f, size.width * 0.18f, size.height * 0.5f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawCircle(tint, side * 0.12f, point(0.5f, 0.5f))
            }

            StarRailIconKind.EYE_HIDDEN -> {
                path.reset()

                path.apply {
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
                path.reset()

                path.apply {
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
                    cornerRadius = CornerRadius(side * 0.05f),
                    style = stroke,
                )
                drawLine(tint, point(0.2f, 0.3f), point(0.8f, 0.3f), strokeWidth)
                drawLine(tint, point(0.38f, 0.18f), point(0.62f, 0.18f), strokeWidth)
                drawLine(tint, point(0.41f, 0.43f), point(0.41f, 0.72f), strokeWidth)
                drawLine(tint, point(0.59f, 0.43f), point(0.59f, 0.72f), strokeWidth)
            }

            StarRailIconKind.EDIT -> {
                path.reset()

                path.apply {
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
                path.reset()

                path.apply {
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
                    cornerRadius = CornerRadius(side * 0.08f),
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
                    cornerRadius = CornerRadius(side * 0.06f),
                    style = stroke,
                )
                drawCircle(tint, side * 0.06f, point(0.32f, 0.38f))
                path.reset()

                path.apply {
                    moveTo(size.width * 0.22f, size.height * 0.72f)
                    lineTo(size.width * 0.42f, size.height * 0.42f)
                    lineTo(size.width * 0.58f, size.height * 0.62f)
                    lineTo(size.width * 0.68f, size.height * 0.48f)
                    lineTo(size.width * 0.78f, size.height * 0.72f)
                }
                drawPath(path, tint, style = stroke)
            }

            StarRailIconKind.CLOSE -> {
                drawLine(tint, point(0.28f, 0.28f), point(0.72f, 0.72f), strokeWidth)
                drawLine(tint, point(0.72f, 0.28f), point(0.28f, 0.72f), strokeWidth)
            }

            StarRailIconKind.KEYBOARD -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(0.15f, 0.25f),
                    size = Size(size.width * 0.7f, size.height * 0.5f),
                    cornerRadius = CornerRadius(side * 0.05f),
                    style = stroke,
                )
                // Keys
                for (i in 0..3) {
                    for (j in 0..1) {
                        drawCircle(
                            tint,
                            side * 0.03f,
                            point(0.28f + i * 0.15f, 0.38f + j * 0.18f)
                        )
                    }
                }
                drawLine(tint, point(0.35f, 0.75f), point(0.65f, 0.75f), strokeWidth)
            }

            StarRailIconKind.RETRY -> {
                drawArc(
                    color = tint,
                    startAngle = RetryStartAngle,
                    sweepAngle = RetrySweepAngle,
                    useCenter = false,
                    topLeft = point(0.22f, 0.22f),
                    size = Size(side * 0.56f, side * 0.56f),
                    style = stroke,
                )

                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val radius = side * 0.28f
                val arrowLength = side * 0.16f
                val arrowTip = Offset(
                    x = center.x + RetryEndCos * radius,
                    y = center.y + RetryEndSin * radius,
                )

                path.reset()
                path.moveTo(arrowTip.x, arrowTip.y)
                path.lineTo(
                    arrowTip.x - RetryArrowLeftCos * arrowLength,
                    arrowTip.y - RetryArrowLeftSin * arrowLength,
                )
                path.moveTo(arrowTip.x, arrowTip.y)
                path.lineTo(
                    arrowTip.x - RetryArrowRightCos * arrowLength,
                    arrowTip.y - RetryArrowRightSin * arrowLength,
                )

                drawPath(path, tint, style = stroke)
            }

            StarRailIconKind.DATABASE -> {
                // Bottom disk
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = point(0.25f, 0.55f),
                    size = Size(size.width * 0.5f, size.height * 0.25f),
                    style = stroke
                )
                // Middle disk
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = point(0.25f, 0.35f),
                    size = Size(size.width * 0.5f, size.height * 0.25f),
                    style = stroke
                )
                // Top disk (full ellipse)
                drawOval(
                    color = tint,
                    topLeft = point(0.25f, 0.15f),
                    size = Size(size.width * 0.5f, size.height * 0.25f),
                    style = stroke
                )
                // Vertical lines
                drawLine(tint, point(0.25f, 0.275f), point(0.25f, 0.675f), strokeWidth)
                drawLine(tint, point(0.75f, 0.275f), point(0.75f, 0.675f), strokeWidth)
            }

            StarRailIconKind.DOWNLOAD -> {
                drawLine(tint, point(0.5f, 0.18f), point(0.5f, 0.66f), strokeWidth)
                drawLine(tint, point(0.5f, 0.66f), point(0.32f, 0.48f), strokeWidth)
                drawLine(tint, point(0.5f, 0.66f), point(0.68f, 0.48f), strokeWidth)
                drawLine(tint, point(0.24f, 0.82f), point(0.76f, 0.82f), strokeWidth)
            }

            StarRailIconKind.EXPORT -> {
                drawRoundRect(
                    color = tint,
                    topLeft = point(0.16f, 0.38f),
                    size = Size(size.width * 0.52f, size.height * 0.46f),
                    cornerRadius = CornerRadius(side * 0.07f),
                    style = stroke,
                )
                drawLine(tint, point(0.46f, 0.54f), point(0.82f, 0.18f), strokeWidth)
                drawLine(tint, point(0.58f, 0.18f), point(0.82f, 0.18f), strokeWidth)
                drawLine(tint, point(0.82f, 0.18f), point(0.82f, 0.42f), strokeWidth)
            }

            StarRailIconKind.PLAY -> {
                path.reset()
                path.moveTo(size.width * 0.35f, size.height * 0.28f)
                path.lineTo(size.width * 0.75f, size.height * 0.5f)
                path.lineTo(size.width * 0.35f, size.height * 0.72f)
                path.close()
                drawPath(path, tint)
            }

            StarRailIconKind.STOP -> {
                drawRect(
                    color = tint,
                    topLeft = point(0.3f, 0.3f),
                    size = Size(size.width * 0.4f, size.height * 0.4f)
                )
            }
                }
            }
        }
    )
}

private fun DrawScope.drawSparkle(
    path: Path,
    color: Color,
    center: Offset,
    radius: Float,
) {
    path.reset()

    path.apply {
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
