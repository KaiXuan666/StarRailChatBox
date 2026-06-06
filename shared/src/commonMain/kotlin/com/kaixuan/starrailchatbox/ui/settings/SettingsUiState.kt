package com.kaixuan.starrailchatbox.ui.settings

import androidx.compose.runtime.Immutable

/**
 * 设置与 API 配置模块状态
 */
@Immutable
data class SettingsUiState(
    val apiHost: String = "https://api.example.com/v1",
    val apiKey: String = "",
    val showApiKey: Boolean = false,
    val modelsList: List<String> = listOf("gpt-4o-mini", "gpt-4.1", "deepseek-chat", "qwen-plus"),
    val selectedModel: String = "gpt-4o-mini",
    val isFetchingModels: Boolean = false,
)
