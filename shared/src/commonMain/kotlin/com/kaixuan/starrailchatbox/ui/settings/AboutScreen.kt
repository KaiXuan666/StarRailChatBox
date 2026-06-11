package com.kaixuan.starrailchatbox.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.main.MainAction
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.about_alipay
import starrailchatbox.shared.generated.resources.about_alipay_qr
import starrailchatbox.shared.generated.resources.about_alipay_tip
import starrailchatbox.shared.generated.resources.about_app_name
import starrailchatbox.shared.generated.resources.about_description
import starrailchatbox.shared.generated.resources.about_disclaimer_content
import starrailchatbox.shared.generated.resources.about_disclaimer_title
import starrailchatbox.shared.generated.resources.about_donate_footer
import starrailchatbox.shared.generated.resources.about_donate_subtitle
import starrailchatbox.shared.generated.resources.about_donate_title
import starrailchatbox.shared.generated.resources.about_wechat_pay
import starrailchatbox.shared.generated.resources.about_wechat_qr
import starrailchatbox.shared.generated.resources.about_wechat_pay_tip
import starrailchatbox.shared.generated.resources.navigation_back
import starrailchatbox.shared.generated.resources.settings_about_title
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 关于我们界面，展示应用描述、免责声明和捐赠信息。
 */
@Composable
fun AboutScreen(
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.starRailColors

    BackHandler {
        onMainAction(MainAction.PopBackStack)
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // High fidelity decorative Compass background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val mainColor = colors.constellation.copy(alpha = 0.08f)
            val subColor = colors.constellationMuted.copy(alpha = 0.04f)

            drawCompassDecor(
                center = Offset(size.width * 0.5f, size.height * 0.45f),
                radius = size.width * 0.42f,
                mainColor = mainColor,
                subColor = subColor
            )
        }

        StarRailPageLayout(
            title = stringResource(Res.string.settings_about_title),
            contentPadding = contentPadding,
            compact = compact,
            backContentDescription = stringResource(Res.string.navigation_back),
            onBackClick = { onMainAction(MainAction.PopBackStack) },
            contentSpacing = StarRailSpacing.lg,
        ) {
            // App Name with decorations
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                DecorationLine(isLeft = true)
                Text(
                    text = stringResource(Res.string.about_app_name),
                    style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                DecorationLine(isLeft = false)
            }

            // Description Card
            AboutCard {
                Text(
                    text = stringResource(Res.string.about_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            }

            // Disclaimer Section
            SectionTitle(title = stringResource(Res.string.about_disclaimer_title))
            AboutCard {
                Text(
                    text = stringResource(Res.string.about_disclaimer_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }

            // Donate Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
            ) {
                SectionTitle(title = stringResource(Res.string.about_donate_title))
                Text(
                    text = stringResource(Res.string.about_donate_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Donate Cards
            DonateCard(
                title = stringResource(Res.string.about_alipay),
                tip = stringResource(Res.string.about_alipay_tip),
                iconKind = StarRailIconKind.GLOBE, // Placeholder for Alipay icon
                primaryColor = Color(0xFF108EE9),
                qrResource = Res.drawable.about_alipay_qr
            )

            DonateCard(
                title = stringResource(Res.string.about_wechat_pay),
                tip = stringResource(Res.string.about_wechat_pay_tip),
                iconKind = StarRailIconKind.CHECK, // Placeholder for WeChat icon
                primaryColor = Color(0xFF07C160),
                qrResource = Res.drawable.about_wechat_qr
            )

            // Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                StarRailIcon(
                    kind = StarRailIconKind.SHIELD,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.about_donate_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun DecorationLine(isLeft: Boolean) {
    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isLeft) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        if (isLeft) listOf(Color.Transparent, color) else listOf(color, Color.Transparent)
                    )
                )
        )
        if (isLeft) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        StarRailIcon(
            kind = StarRailIconKind.SPARKLE,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        StarRailIcon(
            kind = StarRailIconKind.SPARKLE,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.padding(StarRailSpacing.md)) {
            content()
        }
    }
}

@Composable
private fun DonateCard(
    title: String,
    tip: String,
    iconKind: StarRailIconKind,
    primaryColor: Color,
    qrResource: DrawableResource,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StarRailSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StarRailIcon(
                    kind = iconKind,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // QR Code Image
            Surface(
                modifier = Modifier.size(180.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Image(
                    painter = painterResource(qrResource),
                    contentDescription = "QR Code",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
            }

            Text(
                text = tip,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCompassDecor(
    center: Offset,
    radius: Float,
    mainColor: Color,
    subColor: Color
) {
    val thinStroke = Stroke(width = 1.dp.toPx())

    drawCircle(
        color = mainColor,
        radius = radius,
        center = center,
        style = thinStroke
    )

    drawCircle(
        color = subColor,
        radius = radius * 0.8f,
        center = center,
        style = thinStroke
    )

    drawCircle(
        color = mainColor,
        radius = radius * 0.3f,
        center = center,
        style = thinStroke
    )

    drawLine(
        color = subColor,
        start = Offset(center.x - radius * 1.1f, center.y),
        end = Offset(center.x + radius * 1.1f, center.y),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = subColor,
        start = Offset(center.x, center.y - radius * 1.1f),
        end = Offset(center.x, center.y + radius * 1.1f),
        strokeWidth = 1.dp.toPx()
    )

    val path = Path().apply {
        for (i in 0 until 4) {
            val angle = i * (PI / 2).toFloat()
            val outerX = center.x + cos(angle) * radius * 0.75f
            val outerY = center.y + sin(angle) * radius * 0.75f

            val rightAngle = angle + (PI / 8).toFloat()
            val leftAngle = angle - (PI / 8).toFloat()
            val innerRX = center.x + cos(rightAngle) * radius * 0.15f
            val innerRY = center.y + sin(rightAngle) * radius * 0.15f
            val innerLX = center.x + cos(leftAngle) * radius * 0.15f
            val innerLY = center.y + sin(leftAngle) * radius * 0.15f

            moveTo(outerX, outerY)
            lineTo(innerRX, innerRY)
            lineTo(center.x, center.y)
            lineTo(innerLX, innerLY)
            close()
        }
    }
    drawPath(path, mainColor)

    val pathDiag = Path().apply {
        for (i in 0 until 4) {
            val angle = (i * (PI / 2) + (PI / 4)).toFloat()
            val outerX = center.x + cos(angle) * radius * 0.5f
            val outerY = center.y + sin(angle) * radius * 0.5f

            val rightAngle = angle + (PI / 8).toFloat()
            val leftAngle = angle - (PI / 8).toFloat()
            val innerRX = center.x + cos(rightAngle) * radius * 0.12f
            val innerRY = center.y + sin(rightAngle) * radius * 0.12f
            val innerLX = center.x + cos(leftAngle) * radius * 0.12f
            val innerLY = center.y + sin(leftAngle) * radius * 0.12f

            moveTo(outerX, outerY)
            lineTo(innerRX, innerRY)
            lineTo(center.x, center.y)
            lineTo(innerLX, innerLY)
            close()
        }
    }
    drawPath(pathDiag, subColor)
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AboutScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        AboutScreen(
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AboutScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        AboutScreen(
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {}
        )
    }
}
