package com.kaixuan.starrailchatbox.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme



@Composable
fun StarRailPageLayout(
    title: String,
    contentPadding: PaddingValues,
    compact: Boolean,
    backContentDescription: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentSpacing: Dp = StarRailSpacing.xl,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                    StarRailSpacing.lg,
                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(contentSpacing),
    ) {
        StarRailPageHeader(
            title = title,
            compact = compact,
            backContentDescription = backContentDescription,
            onBackClick = onBackClick,
        )
        content()
    }
}

@Composable
fun StarRailPageHeader(
    title: String,
    compact: Boolean,
    backContentDescription: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null && backContentDescription != null) {
            Surface(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StarRailIcon(
                        kind = StarRailIconKind.CHEVRON_LEFT,
                        contentDescription = backContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = if (compact) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.headlineMedium
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun StarRailPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val gradient = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
        ),
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun PageActionsLightPreview() {
    PageActionsPreview(darkTheme = false)
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun PageActionsDarkPreview() {
    PageActionsPreview(darkTheme = true)
}

@Composable
private fun PageActionsPreview(darkTheme: Boolean) {
    StarRailTheme(darkThemeOverride = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(StarRailSpacing.md),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xl),
        ) {
            StarRailPageHeader(
                title = "API Settings",
                compact = true,
                backContentDescription = "Back",
                onBackClick = {},
            )
            StarRailPrimaryButton(
                text = "Save",
                onClick = {},
            )
        }
    }
}
