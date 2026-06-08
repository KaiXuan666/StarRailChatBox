package com.kaixuan.starrailchatbox.ui.settings

import androidx.compose.runtime.Immutable

/**
 * 设置与 API 配置模块状态
 */
@Immutable
data class SettingsUiState(
    val apiHost: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val showApiKey: Boolean = false,
    val modelsList: List<String> = emptyList(),
    val selectedModel: String = "",
    val isFetchingModels: Boolean = false,
    val isSaving: Boolean = false,

    // 多模态 API 设置
    val multimodalApiHost: String = "https://api.openai.com/v1",
    val multimodalApiKey: String = "",
    val multimodalShowApiKey: Boolean = false,
    val multimodalModelsList: List<String> = emptyList(),
    val multimodalSelectedModel: String = "",
    val multimodalIsFetchingModels: Boolean = false,
    val multimodalIsSaving: Boolean = false,
)
