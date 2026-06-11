package com.kaixuan.starrailchatbox.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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
import starrailchatbox.shared.generated.resources.settings_multimodal_api_title
import starrailchatbox.shared.generated.resources.settings_image_generation_api_title
import starrailchatbox.shared.generated.resources.settings_voice_api_title
import starrailchatbox.shared.generated.resources.settings_multimodal_api_tip
import starrailchatbox.shared.generated.resources.settings_get
import starrailchatbox.shared.generated.resources.settings_model_list
import starrailchatbox.shared.generated.resources.settings_save_config
import starrailchatbox.shared.generated.resources.settings_saving
import starrailchatbox.shared.generated.resources.settings_voice_generation_models
import starrailchatbox.shared.generated.resources.settings_voice_clone_models
import starrailchatbox.shared.generated.resources.settings_voice_clone_tip
import starrailchatbox.shared.generated.resources.settings_voice_clone_none
import starrailchatbox.shared.generated.resources.settings_clear_config
import starrailchatbox.shared.generated.resources.navigation_back
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.components.StarRailPrimaryButton
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.StarRailSecondaryButton
import com.kaixuan.starrailchatbox.ui.main.MainAction
import com.kaixuan.starrailchatbox.ui.settings.api.ApiSettingsAction
import com.kaixuan.starrailchatbox.ui.settings.api.ApiSettingsUiState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ApiSettingsScreen(
    state: ApiSettingsUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onApiAction: (ApiSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    isMultimodal: Boolean = false,
    isVoice: Boolean = false,
    isImageGeneration: Boolean = false,
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
            title = when {
                isImageGeneration -> stringResource(Res.string.settings_image_generation_api_title)
                isVoice -> stringResource(Res.string.settings_voice_api_title)
                isMultimodal -> stringResource(Res.string.settings_multimodal_api_title)
                else -> stringResource(Res.string.settings_api_title)
            },
            contentPadding = contentPadding,
            compact = compact,
            backContentDescription = stringResource(Res.string.navigation_back),
            onBackClick = { onMainAction(MainAction.PopBackStack) },
            contentSpacing = StarRailSpacing.lg,
        ) {
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
                    onValueChange = { onApiAction(ApiSettingsAction.ApiHostChanged(it)) },
                    placeholder = if (isVoice) "https://api.xiaomimimo.com/v1" else "https://api.openai.com/v1",
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
                    onValueChange = { onApiAction(ApiSettingsAction.ApiKeyChanged(it)) },
                    placeholder = "sk-",
                    leadingIcon = StarRailIconKind.KEY,
                    isPasswordField = true,
                    passwordVisible = state.showApiKey,
                    onPasswordToggle = { onApiAction(ApiSettingsAction.ToggleApiKeyVisibility) },
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
                        onClick = { onApiAction(ApiSettingsAction.FetchModelsClicked) },
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

                if (isMultimodal && !state.isFetchingModels && state.modelsList.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.settings_multimodal_api_tip),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                
                // Show loading placeholder if loading but empty list, or overlay animation
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)
                    ) {
                        if (isVoice && state.modelsList.isNotEmpty()) {
                            StarRailDropdown(
                                label = stringResource(Res.string.settings_voice_generation_models),
                                options = state.modelsList,
                                selectedOption = state.selectedModel,
                                onOptionSelected = { onApiAction(ApiSettingsAction.SelectModel(it)) },
                                compact = compact,
                                isFetching = state.isFetchingModels,
                                onFetchRequest = { onApiAction(ApiSettingsAction.FetchModelsClicked) }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val noneText = stringResource(Res.string.settings_voice_clone_none)
                            StarRailDropdown(
                                label = stringResource(Res.string.settings_voice_clone_models),
                                options = listOf(noneText) + state.modelsList,
                                selectedOption = state.selectedCloneModel.ifEmpty { noneText },
                                onOptionSelected = {
                                    val model = if (it == noneText) "" else it
                                    onApiAction(ApiSettingsAction.SelectModel(model, isCloneModel = true))
                                },
                                compact = compact,
                                placeholder = noneText,
                                isFetching = state.isFetchingModels,
                                onFetchRequest = { onApiAction(ApiSettingsAction.FetchModelsClicked) }
                            )
                            
                            Text(
                                text = stringResource(Res.string.settings_voice_clone_tip),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                            )
                        } else if (state.modelsList.isNotEmpty()) {
                            StarRailDropdown(
                                options = state.modelsList,
                                selectedOption = state.selectedModel,
                                onOptionSelected = { onApiAction(ApiSettingsAction.SelectModel(it)) },
                                compact = compact,
                                placeholder = stringResource(Res.string.settings_api_model_selected),
                                iconKind = if (isImageGeneration) StarRailIconKind.GALLERY else StarRailIconKind.CUBE,
                                isFetching = state.isFetchingModels,
                                onFetchRequest = { onApiAction(ApiSettingsAction.FetchModelsClicked) }
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
            
            StarRailPrimaryButton(
                text = stringResource(
                    if (state.isSaving) {
                        Res.string.settings_saving
                    } else {
                        Res.string.settings_save_config
                    },
                ),
                onClick = { onApiAction(ApiSettingsAction.SaveSettingsClicked) },
                enabled = !state.isSaving,
            )

            StarRailSecondaryButton(
                text = stringResource(Res.string.settings_clear_config),
                onClick = { onApiAction(ApiSettingsAction.ClearSettingsClicked) },
                modifier = Modifier.fillMaxWidth()
            )
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
private fun StarRailDropdown(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    label: String? = null,
    placeholder: String = "",
    iconKind: StarRailIconKind = StarRailIconKind.CUBE,
    isFetching: Boolean = false,
    onFetchRequest: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var wasFetching by remember { mutableStateOf(false) }

    LaunchedEffect(isFetching) {
        if (wasFetching && !isFetching) {
            wasFetching = false
            if (options.isNotEmpty()) {
                expanded = true
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs)
    ) {
        if (label != null) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = {
                    if (options.isEmpty() && !isFetching && onFetchRequest != null) {
                        onFetchRequest()
                        wasFetching = true
                    } else {
                        expanded = !expanded
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            StarRailIcon(
                                kind = iconKind,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (compact) 18.dp else 22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (isFetching && options.isEmpty()) stringResource(Res.string.settings_api_fetching)
                                   else selectedOption.ifEmpty { placeholder },
                            color = if (selectedOption.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Box(modifier = Modifier.rotate(if (expanded) -90f else 90f)) {
                        StarRailIcon(
                            kind = StarRailIconKind.CHEVRON_RIGHT,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (option == selectedOption) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        trailingIcon = if (option == selectedOption) {
                            {
                                StarRailIcon(
                                    kind = StarRailIconKind.CHECK,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
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
            state = ApiSettingsUiState(
                apiHost = "https://api.example.com/v1",
                apiKey = "sk-1234567890",
                selectedModel = "gpt-4o-mini"
            ),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onApiAction = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ApiSettingsScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        ApiSettingsScreen(
            state = ApiSettingsUiState(
                apiHost = "https://api.example.com/v1",
                apiKey = "sk-1234567890",
                selectedModel = "gpt-4o-mini"
            ),
            contentPadding = PaddingValues(0.dp),
            compact = true,
            onMainAction = {},
            onApiAction = {}
        )
    }
}
