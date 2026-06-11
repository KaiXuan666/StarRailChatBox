package com.kaixuan.starrailchatbox.ui.settings.api

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.data.ai.OpenAiCompatibleProvider
import com.kaixuan.starrailchatbox.data.ai.image.ImageGenerationProviderIds

@Immutable
data class ApiSettingsUiState(
    val apiHost: String = "",
    val apiKey: String = "",
    val showApiKey: Boolean = false,
    val modelsList: List<String> = emptyList(),
    val selectedModel: String = "",
    val selectedCloneModel: String = "", // 仅用于语音模式
    val apiProviderId: String = OpenAiCompatibleProvider.Id,
    val imageProviderId: String = ImageGenerationProviderIds.OpenAiCompatible,
    val isFetchingModels: Boolean = false,
    val isSaving: Boolean = false,
    val showSuggestDefaultConfigDialog: Boolean = false,
)
