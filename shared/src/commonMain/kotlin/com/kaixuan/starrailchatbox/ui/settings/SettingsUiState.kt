package com.kaixuan.starrailchatbox.ui.settings

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsOverviewUiState(
    val isDefaultConfigured: Boolean = false,
    val isMultimodalConfigured: Boolean = false,
    val isVoiceConfigured: Boolean = false,
    val isImageGenerationConfigured: Boolean = false,
)

/**
 * 设置模块状态（仅保留配置状态标记）
 */
@Immutable
data class SettingsUiState(
    // 配置状态（真实持久化状态）
    val isDefaultConfigured: Boolean = false,
    val isMultimodalConfigured: Boolean = false,
    val isVoiceConfigured: Boolean = false,
    val isImageGenerationConfigured: Boolean = false,
)
