package com.kaixuan.starrailchatbox.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.settings_api_fetching
import starrailchatbox.shared.generated.resources.settings_api_host
import starrailchatbox.shared.generated.resources.settings_api_key
import starrailchatbox.shared.generated.resources.settings_api_model_selected
import starrailchatbox.shared.generated.resources.settings_api_empty_models
import starrailchatbox.shared.generated.resources.settings_api_title
import starrailchatbox.shared.generated.resources.settings_get
import starrailchatbox.shared.generated.resources.settings_model_list
import starrailchatbox.shared.generated.resources.settings_save_config
import starrailchatbox.shared.generated.resources.settings_saving
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.main.MainAction
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ApiSettingsScreen(
    state: SettingsUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onSettingsAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.starRailColors
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + StarRailSpacing.xs,
                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                bottom = contentPadding.calculateBottomPadding() + StarRailSpacing.lg
            )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.lg)
        ) {
            // --- Custom Header (Back arrow + Title + Tech Divider) ---
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back navigation button
                    Surface(
                        onClick = { onMainAction(MainAction.PopBackStack) },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            StarRailIcon(
                                kind = StarRailIconKind.CHEVRON_LEFT,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(if (compact) 28.dp else 32.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = stringResource(Res.string.settings_api_title),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = if (compact) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.headlineSmall
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                // Decorative scifi line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    colors.constellation.copy(alpha = 0.65f),
                                    colors.constellationMuted.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // --- API Host Section ---
            Column(
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
            ) {
                Text(
                    text = stringResource(Res.string.settings_api_host),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                ApiInputField(
                    value = state.apiHost,
                    onValueChange = { onSettingsAction(SettingsAction.ApiHostChanged(it)) },
                    placeholder = "https://api.openai.com/v1",
                    leadingIcon = StarRailIconKind.COMPASS,
                    compact = compact
                )
            }

            // --- API Key Section ---
            Column(
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
            ) {
                Text(
                    text = stringResource(Res.string.settings_api_key),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                ApiInputField(
                    value = state.apiKey,
                    onValueChange = { onSettingsAction(SettingsAction.ApiKeyChanged(it)) },
                    placeholder = "sk-",
                    leadingIcon = StarRailIconKind.KEY,
                    isPasswordField = true,
                    passwordVisible = state.showApiKey,
                    onPasswordToggle = { onSettingsAction(SettingsAction.ToggleApiKeyVisibility) },
                    compact = compact
                )
            }

            // --- Model List Section ---
            Column(
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.settings_model_list),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    
                    // Fetch models button
                    val fetchInteractionSource = remember { MutableInteractionSource() }
                    val isFetchPressed by fetchInteractionSource.collectIsPressedAsState()
                    val fetchScale by animateFloatAsState(if (isFetchPressed) 0.92f else 1f)
                    
                    Surface(
                        onClick = { onSettingsAction(SettingsAction.FetchModelsClicked) },
                        enabled = !state.isFetchingModels,
                        interactionSource = fetchInteractionSource,
                        modifier = Modifier.scale(fetchScale),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (state.isFetchingModels) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 1.5.dp
                                )
                            }
                            Text(
                                text = stringResource(Res.string.settings_get),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Show loading placeholder if loading but empty list, or overlay animation
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
                    ) {
                        state.modelsList.forEach { model ->
                            ModelCardItem(
                                model = model,
                                isSelected = state.selectedModel == model,
                                onClick = { onSettingsAction(SettingsAction.SelectModel(model)) },
                                compact = compact
                            )
                        }
                        if (!state.isFetchingModels && state.modelsList.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.settings_api_empty_models),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(StarRailSpacing.md),
                            )
                        }
                    }
                    
                    // Loading Overlay banner
                    if (state.isFetchingModels && state.modelsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(Res.string.settings_api_fetching),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // --- Save Configuration Button ---
            val saveInteractionSource = remember { MutableInteractionSource() }
            val isSavePressed by saveInteractionSource.collectIsPressedAsState()
            val saveScale by animateFloatAsState(if (isSavePressed) 0.95f else 1f)
            
            val saveGradient = Brush.horizontalGradient(
                listOf(
                    Color(0xFF5D7AFF), // StarRail primary signature blue
                    Color(0xFF385EFF)
                )
            )
            
            Surface(
                onClick = {
                    onSettingsAction(SettingsAction.SaveApiSettingsClicked)
                },
                enabled = !state.isSaving,
                interactionSource = saveInteractionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 46.dp else 52.dp)
                    .scale(saveScale),
                shape = RoundedCornerShape(50),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(saveGradient)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(
                            text = stringResource(
                                if (state.isSaving) {
                                    Res.string.settings_saving
                                } else {
                                    Res.string.settings_save_config
                                },
                            ),
                            color = Color.White,
                            style = if (compact) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: StarRailIconKind,
    modifier: Modifier = Modifier,
    isPasswordField: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    compact: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarRailIcon(
                kind = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (compact) 18.dp else 22.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                }
                
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    visualTransformation = if (isPasswordField && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    }
                )
            }
            
            if (isPasswordField) {
                Spacer(modifier = Modifier.width(8.dp))
                val eyeIcon = if (passwordVisible) StarRailIconKind.EYE_VISIBLE else StarRailIconKind.EYE_HIDDEN
                
                Surface(
                    onClick = onPasswordToggle,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        StarRailIcon(
                            kind = eyeIcon,
                            contentDescription = "Show/Hide Key",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCardItem(
    model: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val outlineBrushColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    
    val bgGradient = if (isSelected) {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f)
            )
        )
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, outlineBrushColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .padding(horizontal = 14.dp, vertical = if (compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 32.dp else 38.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    StarRailIcon(
                        kind = StarRailIconKind.CUBE,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = model,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_api_model_selected),
                            color = MaterialTheme.colorScheme.primary,
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .size(if (compact) 18.dp else 22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
                    .border(
                        if (isSelected) 2.dp else 1.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    StarRailIcon(
                        kind = StarRailIconKind.CHECK,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (compact) 10.dp else 12.dp)
                    )
                }
            }
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
private fun ApiSettingsScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        ApiSettingsScreen(
            state = SettingsUiState(
                apiHost = "https://api.example.com/v1",
                apiKey = "sk-1234567890",
                selectedModel = "gpt-4o-mini"
            ),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onSettingsAction = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ApiSettingsScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        ApiSettingsScreen(
            state = SettingsUiState(
                apiHost = "https://api.example.com/v1",
                apiKey = "sk-1234567890",
                selectedModel = "gpt-4o-mini"
            ),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onSettingsAction = {}
        )
    }
}
