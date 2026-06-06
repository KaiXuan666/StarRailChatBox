package com.kaixuan.starrailchatbox.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val StarRailLightColorScheme = lightColorScheme(
    primary = Color(0xFF4F6FFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7EEFF),
    onPrimaryContainer = Color(0xFF174F9F),
    secondary = Color(0xFF6257D9),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECE9FF),
    onSecondaryContainer = Color(0xFF30266F),
    tertiary = Color(0xFF008FA8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8F3FA),
    onTertiaryContainer = Color(0xFF00363F),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF171D38),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF202947),
    surfaceVariant = Color(0xFFEEF2FC),
    onSurfaceVariant = Color(0xFF626D8D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F7FE),
    surfaceContainer = Color(0xFFEFF3FC),
    surfaceContainerHigh = Color(0xFFE8EDF8),
    surfaceContainerHighest = Color(0xFFE1E7F3),
    outline = Color(0xFF7883A3),
    outlineVariant = Color(0xFFCBD4EA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val StarRailDarkColorScheme = darkColorScheme(
    primary = Color(0xFF58E7F2),
    onPrimary = Color(0xFF00363C),
    primaryContainer = Color(0xFF173F66),
    onPrimaryContainer = Color(0xFFE9F7FF),
    secondary = Color(0xFFB8A9FF),
    onSecondary = Color(0xFF2D2465),
    secondaryContainer = Color(0xFF443A80),
    onSecondaryContainer = Color(0xFFE7DEFF),
    tertiary = Color(0xFF49DCEB),
    onTertiary = Color(0xFF00373D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF9CF0FA),
    background = Color(0xFF020817),
    onBackground = Color(0xFFEEF3FF),
    surface = Color(0xFF08152E),
    onSurface = Color(0xFFEDF2FF),
    surfaceVariant = Color(0xFF111D38),
    onSurfaceVariant = Color(0xFFA9B5D1),
    surfaceContainerLowest = Color(0xFF020817),
    surfaceContainerLow = Color(0xFF071229),
    surfaceContainer = Color(0xFF0B1731),
    surfaceContainerHigh = Color(0xFF101D39),
    surfaceContainerHighest = Color(0xFF172440),
    outline = Color(0xFF7282A4),
    outlineVariant = Color(0xFF283B5C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Immutable
data class StarRailExtendedColors(
    val online: Color,
    val constellation: Color,
    val constellationMuted: Color,
    val sentBubbleBorder: Color,
    val receivedBubbleBorder: Color,
    val avatarRingStart: Color,
    val avatarRingEnd: Color,
    val successCheck: Color,
    val warmSparkle: Color,
    val backgroundGlow: Color,
)

private val LightExtendedColors = StarRailExtendedColors(
    online = Color(0xFF44C7E7),
    constellation = Color(0xFF8FA8E0),
    constellationMuted = Color(0xFFD7E0F8),
    sentBubbleBorder = Color(0xFF6B9DFF),
    receivedBubbleBorder = Color(0xFFCFD7EF),
    avatarRingStart = Color(0xFF4AC8FF),
    avatarRingEnd = Color(0xFF9B54FF),
    successCheck = Color(0xFF3BA8F4),
    warmSparkle = Color(0xFFF7C629),
    backgroundGlow = Color(0xFFDCE8FF),
)

private val DarkExtendedColors = StarRailExtendedColors(
    online = Color(0xFF38ECF4),
    constellation = Color(0xFF4C6FA0),
    constellationMuted = Color(0xFF17345A),
    sentBubbleBorder = Color(0xFF64A0FF),
    receivedBubbleBorder = Color(0xFF293B5C),
    avatarRingStart = Color(0xFF42F0FF),
    avatarRingEnd = Color(0xFF8668FF),
    successCheck = Color(0xFF43E7E9),
    warmSparkle = Color(0xFFFFC928),
    backgroundGlow = Color(0xFF0A335C),
)

private val LocalStarRailExtendedColors = staticCompositionLocalOf {
    LightExtendedColors
}

val MaterialTheme.starRailColors: StarRailExtendedColors
    @Composable get() = LocalStarRailExtendedColors.current

object StarRailSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
}

private val StarRailTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
)

private val StarRailShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun StarRailTheme(
    darkThemeOverride: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = darkThemeOverride ?: isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(
        LocalStarRailExtendedColors provides
            if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) {
                StarRailDarkColorScheme
            } else {
                StarRailLightColorScheme
            },
            typography = StarRailTypography,
            shapes = StarRailShapes,
            content = content,
        )
    }
}
