package com.kaixuan.starrailchatbox.ui.settings.localmodel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.data.localmodel.ChatModelMode
import com.kaixuan.starrailchatbox.data.localmodel.InferenceBackend
import com.kaixuan.starrailchatbox.data.localmodel.LocalModelInstallState
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.cancel
import starrailchatbox.shared.generated.resources.confirm
import starrailchatbox.shared.generated.resources.local_model_backend_auto
import starrailchatbox.shared.generated.resources.local_model_backend_cpu
import starrailchatbox.shared.generated.resources.local_model_backend_cpu_fallback
import starrailchatbox.shared.generated.resources.local_model_backend_gpu
import starrailchatbox.shared.generated.resources.local_model_catalog_title
import starrailchatbox.shared.generated.resources.local_model_delete
import starrailchatbox.shared.generated.resources.local_model_delete_message
import starrailchatbox.shared.generated.resources.local_model_delete_title
import starrailchatbox.shared.generated.resources.local_model_download
import starrailchatbox.shared.generated.resources.local_model_empty
import starrailchatbox.shared.generated.resources.local_model_import
import starrailchatbox.shared.generated.resources.local_model_import_message
import starrailchatbox.shared.generated.resources.local_model_import_title
import starrailchatbox.shared.generated.resources.local_model_in_use
import starrailchatbox.shared.generated.resources.local_model_install
import starrailchatbox.shared.generated.resources.local_model_installed_models
import starrailchatbox.shared.generated.resources.local_model_mode_local
import starrailchatbox.shared.generated.resources.local_model_mode_online
import starrailchatbox.shared.generated.resources.local_model_mode_title
import starrailchatbox.shared.generated.resources.local_model_pause
import starrailchatbox.shared.generated.resources.local_model_platform_unsupported
import starrailchatbox.shared.generated.resources.local_model_qwen_detail
import starrailchatbox.shared.generated.resources.local_model_qwen_name
import starrailchatbox.shared.generated.resources.local_model_resume
import starrailchatbox.shared.generated.resources.local_model_source
import starrailchatbox.shared.generated.resources.local_model_use
import starrailchatbox.shared.generated.resources.local_model_verifying
import starrailchatbox.shared.generated.resources.navigation_back
import starrailchatbox.shared.generated.resources.settings_local_model_title

@Composable
fun LocalModelSettingsScreen(
    state: LocalModelSettingsUiState,
    contentPadding: PaddingValues,
    compact: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onOpenSource: () -> Unit,
    onAction: (LocalModelSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    StarRailPageLayout(
        title = stringResource(Res.string.settings_local_model_title),
        contentPadding = contentPadding,
        compact = compact,
        backContentDescription = stringResource(Res.string.navigation_back),
        onBackClick = onBack,
        modifier = modifier,
    ) {
        if (!state.isRuntimeSupported) {
            Text(
                stringResource(Res.string.local_model_platform_unsupported),
                color = MaterialTheme.colorScheme.error,
            )
        }
        SectionCard {
            Text(
                stringResource(Res.string.local_model_mode_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)) {
                ModeButton(
                    selected = state.mode == ChatModelMode.ONLINE,
                    text = stringResource(Res.string.local_model_mode_online),
                    onClick = { onAction(LocalModelSettingsAction.ModeSelected(ChatModelMode.ONLINE)) },
                    modifier = Modifier.weight(1f),
                )
                ModeButton(
                    selected = state.mode == ChatModelMode.LOCAL,
                    text = stringResource(Res.string.local_model_mode_local),
                    onClick = { onAction(LocalModelSettingsAction.ModeSelected(ChatModelMode.LOCAL)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                when (state.runtimeStatus.backend) {
                    InferenceBackend.GPU -> stringResource(Res.string.local_model_backend_gpu)
                    InferenceBackend.CPU -> stringResource(
                        if (state.runtimeStatus.fallbackReason == null) {
                            Res.string.local_model_backend_cpu
                        } else {
                            Res.string.local_model_backend_cpu_fallback
                        },
                    )
                    null -> stringResource(Res.string.local_model_backend_auto)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionCard {
            Text(
                stringResource(Res.string.local_model_catalog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(Res.string.local_model_qwen_name), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.local_model_qwen_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onOpenSource) {
                Text(stringResource(Res.string.local_model_source))
            }
            if (state.installState == LocalModelInstallState.DOWNLOADING ||
                state.installState == LocalModelInstallState.VERIFYING
            ) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (state.installState == LocalModelInstallState.VERIFYING) {
                        stringResource(Res.string.local_model_verifying)
                    } else {
                        "${formatBytes(state.downloadedBytes)} / ${formatBytes(state.totalBytes)} · ${formatBytes(state.bytesPerSecond)}/s"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)) {
                Button(
                    onClick = {
                        if (state.installState == LocalModelInstallState.DOWNLOADING) {
                            onAction(LocalModelSettingsAction.CancelDownload)
                        } else {
                            onAction(LocalModelSettingsAction.DownloadCatalogModel)
                        }
                    },
                    enabled = state.isRuntimeSupported && state.installState != LocalModelInstallState.VERIFYING,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when (state.installState) {
                            LocalModelInstallState.DOWNLOADING -> stringResource(Res.string.local_model_pause)
                            LocalModelInstallState.FAILED -> stringResource(Res.string.local_model_resume)
                            else -> stringResource(Res.string.local_model_download)
                        },
                    )
                }
                OutlinedButton(
                    onClick = onImport,
                    enabled = state.isRuntimeSupported,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.local_model_import)) }
            }
        }
        SectionCard {
            Text(
                stringResource(Res.string.local_model_installed_models),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.installedModels.isEmpty()) {
                Text(
                    stringResource(Res.string.local_model_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.installedModels.forEach { model ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        Modifier.padding(StarRailSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
                    ) {
                        Text(model.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${formatBytes(model.sizeBytes)} · ${model.license}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.sm)) {
                            Button(
                                onClick = { onAction(LocalModelSettingsAction.SelectModel(model.id)) },
                                enabled = state.selectedModelId != model.id || state.mode != ChatModelMode.LOCAL,
                            ) {
                                Text(
                                    stringResource(
                                        if (state.selectedModelId == model.id && state.mode == ChatModelMode.LOCAL) {
                                            Res.string.local_model_in_use
                                        } else {
                                            Res.string.local_model_use
                                        },
                                    ),
                                )
                            }
                            OutlinedButton(
                                onClick = { onAction(LocalModelSettingsAction.RequestDelete(model.id)) },
                                enabled = !state.runtimeStatus.isBusy,
                            ) { Text(stringResource(Res.string.local_model_delete)) }
                        }
                    }
                }
            }
        }
    }

    state.stagedImport?.let {
        StarRailDialog(
            title = stringResource(Res.string.local_model_import_title),
            confirmText = stringResource(Res.string.local_model_install),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = { onAction(LocalModelSettingsAction.ConfirmImport) },
            onDismissRequest = { onAction(LocalModelSettingsAction.DismissImport) },
        ) { Text(stringResource(Res.string.local_model_import_message)) }
    }
    state.pendingDelete?.let { model ->
        StarRailDialog(
            title = stringResource(Res.string.local_model_delete_title),
            confirmText = stringResource(Res.string.confirm),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = { onAction(LocalModelSettingsAction.ConfirmDelete) },
            onDismissRequest = { onAction(LocalModelSettingsAction.DismissDelete) },
        ) { Text(stringResource(Res.string.local_model_delete_message, model.name)) }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(StarRailSpacing.md),
            verticalArrangement = Arrangement.spacedBy(StarRailSpacing.sm),
            content = content,
        )
    }
}

@Composable
private fun ModeButton(selected: Boolean, text: String, onClick: () -> Unit, modifier: Modifier) {
    if (selected) Button(onClick = onClick, modifier = modifier) { Text(text) }
    else OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
}

private fun formatBytes(value: Long): String = when {
    value >= 1_073_741_824L -> "${(value / 107_374_182.4).toInt() / 10.0} GB"
    value >= 1_048_576L -> "${value / 1_048_576L} MB"
    value >= 1_024L -> "${value / 1_024L} KB"
    else -> "$value B"
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LocalModelSettingsLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        LocalModelSettingsScreen(LocalModelSettingsUiState(), PaddingValues(), true, {}, {}, {}, {})
    }
}

@Preview(widthDp = 720, heightDp = 900)
@Composable
private fun LocalModelSettingsDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        LocalModelSettingsScreen(LocalModelSettingsUiState(), PaddingValues(), false, {}, {}, {}, {})
    }
}

@Preview(widthDp = 600, heightDp = 900)
@Composable
private fun LocalModelSettingsMediumPreview() {
    StarRailTheme(darkThemeOverride = false) {
        LocalModelSettingsScreen(LocalModelSettingsUiState(), PaddingValues(), false, {}, {}, {}, {})
    }
}
