package com.kaixuan.starrailchatbox.ui.settings.api

import androidx.compose.runtime.Immutable

@Immutable
data class ApiSettingsUiState(
    val apiHost: String = "",
    val apiKey: String = "",
    val showApiKey: Boolean = false,
    val modelsList: List<String> = emptyList(),
    val selectedModel: String = "",
    val selectedCloneModel: String = "", // 仅用于语音模式
    val isFetchingModels: Boolean = false,
    val isSaving: Boolean = false,
)
