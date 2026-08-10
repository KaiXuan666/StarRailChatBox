package com.kaixuan.starrailchatbox.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.localmodel.LocalModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsOverviewViewModel(
    private val modelConfigRepository: ModelConfigRepository,
    private val localModelRepository: LocalModelRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsOverviewUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val onlineState = combine(
                modelConfigRepository.observeDefault(),
                modelConfigRepository.observeMultimodal(),
                modelConfigRepository.observeVoice(),
                modelConfigRepository.observeVoiceClone(),
                modelConfigRepository.observeImageGeneration(),
            ) { default, multimodal, voice, voiceClone, imageGeneration ->
                SettingsOverviewUiState(
                    isDefaultConfigured = default?.apiKey?.isNotBlank() == true,
                    isMultimodalConfigured = multimodal?.apiKey?.isNotBlank() == true,
                    isVoiceConfigured = voice?.apiKey?.isNotBlank() == true ||
                        voiceClone?.apiKey?.isNotBlank() == true,
                    isImageGenerationConfigured = imageGeneration?.apiKey?.isNotBlank() == true,
                )
            }
            combine(onlineState, localModelRepository.observeModels()) { state, localModels ->
                state.copy(isLocalModelConfigured = localModels.isNotEmpty())
            }.collect(_uiState)
        }
    }
}
