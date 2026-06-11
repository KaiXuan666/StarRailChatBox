package com.kaixuan.starrailchatbox.ui.settings.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.OpenAiCompatibleProvider
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.DefaultModelConfig
import com.kaixuan.starrailchatbox.data.model.ImageGenerationModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.MultimodalModelConfig
import com.kaixuan.starrailchatbox.data.model.VoiceCloneModelConfig
import com.kaixuan.starrailchatbox.data.model.VoiceModelConfig
import com.kaixuan.starrailchatbox.data.settings.ApiSettingsDefaults
import com.kaixuan.starrailchatbox.data.settings.localApiSettingsDefaults
import com.kaixuan.starrailchatbox.ui.settings.SettingsEffectMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ApiSettingsViewModel(
    private val isMultimodal: Boolean = false,
    private val isVoice: Boolean = false,
    private val isImageGeneration: Boolean = false,
    private val aiRepository: AiRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val defaultApiSettings: ApiSettingsDefaults = localApiSettingsDefaults(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApiSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<ApiSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val config = when {
                isImageGeneration -> modelConfigRepository.getImageGeneration()
                isVoice -> modelConfigRepository.getVoice()
                isMultimodal -> modelConfigRepository.getMultimodal()
                else -> modelConfigRepository.getDefault()
            }

            val defaultHost = when {
                isImageGeneration -> defaultApiSettings.imageHost
                isVoice -> defaultApiSettings.voiceHost
                isMultimodal -> defaultApiSettings.multimodalHost
                else -> defaultApiSettings.apiHost
            }

            val defaultKey = when {
                isImageGeneration -> defaultApiSettings.imageKey
                isVoice -> defaultApiSettings.voiceKey
                isMultimodal -> defaultApiSettings.multimodalKey
                else -> defaultApiSettings.apiKey
            }

            val voiceCloneConfig = if (isVoice) modelConfigRepository.getVoiceClone() else null

            _uiState.update { state ->
                state.copy(
                    apiHost = config?.baseUrl ?: defaultHost,
                    apiKey = config?.apiKey ?: defaultKey,
                    selectedModel = config?.modelName.orEmpty(),
                    selectedCloneModel = voiceCloneConfig?.modelName.orEmpty(),
                    modelsList = listOfNotNull(
                        config?.modelName?.takeIf { it.isNotBlank() },
                        voiceCloneConfig?.modelName?.takeIf { it.isNotBlank() }
                    ).distinct()
                )
            }
        }
    }

    fun onAction(action: ApiSettingsAction) {
        when (action) {
            is ApiSettingsAction.ApiHostChanged -> {
                _uiState.update { it.copy(apiHost = action.host) }
            }
            is ApiSettingsAction.ApiKeyChanged -> {
                _uiState.update { it.copy(apiKey = action.key) }
            }
            ApiSettingsAction.ToggleApiKeyVisibility -> {
                _uiState.update { it.copy(showApiKey = !it.showApiKey) }
            }
            ApiSettingsAction.FetchModelsClicked -> fetchModels()
            is ApiSettingsAction.SelectModel -> {
                _uiState.update { state ->
                    if (action.isCloneModel) {
                        state.copy(selectedCloneModel = action.model)
                    } else {
                        state.copy(selectedModel = action.model)
                    }
                }
            }
            ApiSettingsAction.SaveSettingsClicked -> saveSettings()
            ApiSettingsAction.ClearSettingsClicked -> clearSettings()
            is ApiSettingsAction.CopyToClipboard -> {
                emitMessage(SettingsEffectMessage.SETTINGS_COPIED_SUCCESS)
            }
        }
    }

    private fun fetchModels() {
        val state = _uiState.value
        if (state.isFetchingModels) return

        if (!hasValidApiSettings(state.apiHost, state.apiKey)) {
            emitMessage(SettingsEffectMessage.SETTINGS_API_INVALID)
            return
        }

        _uiState.update { it.copy(isFetchingModels = true) }
        emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_START)

        viewModelScope.launch {
            when (
                val result = aiRepository.getModels(
                    apiHost = state.apiHost,
                    apiKey = state.apiKey,
                    providerId = OpenAiCompatibleProvider.Id,
                )
            ) {
                is ApiResult.Success -> handleModelsLoaded(result.value)
                is ApiResult.HttpError -> {
                    _uiState.update { it.copy(isFetchingModels = false) }
                    emitMessage(
                        if (result.statusCode == 401 || result.statusCode == 403) {
                            SettingsEffectMessage.SETTINGS_API_AUTH_FAILED
                        } else {
                            SettingsEffectMessage.SETTINGS_API_FETCH_FAILED
                        },
                    )
                }
                else -> {
                    _uiState.update { it.copy(isFetchingModels = false) }
                    emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_FAILED)
                }
            }
        }
    }

    private fun handleModelsLoaded(models: List<String>) {
        if (models.isEmpty()) {
            _uiState.update { it.copy(isFetchingModels = false, modelsList = emptyList()) }
            emitMessage(SettingsEffectMessage.SETTINGS_API_NO_MODELS)
            return
        }

        val sortedModels = when {
            isVoice -> models.sortedByDescending { it.contains("tts", ignoreCase = true) }
            isImageGeneration -> models.sortedByDescending {
                it.contains("image", ignoreCase = true) ||
                        it.contains("dall-e", ignoreCase = true) ||
                        it.contains("flux", ignoreCase = true)
            }
            else -> models
        }

        _uiState.update { state ->
            state.copy(
                isFetchingModels = false,
                modelsList = sortedModels,
                selectedModel = state.selectedModel.takeIf(sortedModels::contains)
                    ?: if (isVoice) {
                        sortedModels.firstOrNull { !it.contains("clone") && !it.contains("design") }
                            ?: sortedModels.first()
                    } else {
                        sortedModels.first()
                    },
                selectedCloneModel = state.selectedCloneModel.takeIf(sortedModels::contains) ?: "",
            )
        }
        emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_SUCCESS)
    }

    private fun saveSettings() {
        val state = _uiState.value
        if (state.isSaving) return

        if (!hasValidApiSettings(state.apiHost, state.apiKey)) {
            emitMessage(SettingsEffectMessage.SETTINGS_API_INVALID)
            return
        }

        if (state.selectedModel.isBlank()) {
            emitMessage(SettingsEffectMessage.SETTINGS_API_NO_MODELS)
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                if (isVoice) {
                    saveVoiceSettings(state)
                } else if (isImageGeneration) {
                    saveImageGenerationSettings(state)
                } else {
                    saveCommonSettings(state)
                }
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(ApiSettingsEffect.ApiSettingsSaved)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                t.printStackTrace()
                _uiState.update { it.copy(isSaving = false) }
                emitMessage(SettingsEffectMessage.SETTINGS_API_SAVE_FAILED)
            }
        }
    }

    private suspend fun saveVoiceSettings(state: ApiSettingsUiState) {
        if (state.selectedModel.isNotBlank()) {
            modelConfigRepository.saveVoice(
                ModelConfig(
                    id = VoiceModelConfig.Id,
                    provider = VoiceModelConfig.Provider,
                    name = VoiceModelConfig.Name,
                    baseUrl = state.apiHost.trim().trimEnd('/'),
                    apiKey = state.apiKey.trim(),
                    modelName = state.selectedModel,
                    contextWindow = VoiceModelConfig.ContextWindow,
                    maxOutputTokens = VoiceModelConfig.MaxOutputTokens,
                    supportVision = false,
                    supportToolCall = false,
                    supportReasoning = false,
                    temperature = VoiceModelConfig.Temperature,
                    topP = VoiceModelConfig.TopP,
                    enabled = true,
                ),
            )
        } else {
            modelConfigRepository.deleteConfig(VoiceModelConfig.Id)
        }

        if (state.selectedCloneModel.isNotBlank()) {
            modelConfigRepository.saveVoiceClone(
                ModelConfig(
                    id = VoiceCloneModelConfig.Id,
                    provider = VoiceCloneModelConfig.Provider,
                    name = VoiceCloneModelConfig.Name,
                    baseUrl = state.apiHost.trim().trimEnd('/'),
                    apiKey = state.apiKey.trim(),
                    modelName = state.selectedCloneModel,
                    contextWindow = VoiceCloneModelConfig.ContextWindow,
                    maxOutputTokens = VoiceCloneModelConfig.MaxOutputTokens,
                    supportVision = false,
                    supportToolCall = false,
                    supportReasoning = false,
                    temperature = VoiceCloneModelConfig.Temperature,
                    topP = VoiceCloneModelConfig.TopP,
                    enabled = true,
                ),
            )
        } else {
            modelConfigRepository.deleteConfig(VoiceCloneModelConfig.Id)
        }
    }

    private suspend fun saveImageGenerationSettings(state: ApiSettingsUiState) {
        modelConfigRepository.saveImageGeneration(
            ModelConfig(
                id = ImageGenerationModelConfig.Id,
                provider = ImageGenerationModelConfig.Provider,
                name = ImageGenerationModelConfig.Name,
                baseUrl = state.apiHost.trim().trimEnd('/'),
                apiKey = state.apiKey.trim(),
                modelName = state.selectedModel,
                contextWindow = ImageGenerationModelConfig.ContextWindow,
                maxOutputTokens = ImageGenerationModelConfig.MaxOutputTokens,
                supportVision = false,
                supportToolCall = false,
                supportReasoning = false,
                temperature = ImageGenerationModelConfig.Temperature,
                topP = ImageGenerationModelConfig.TopP,
                enabled = true,
            ),
        )
    }

    private suspend fun saveCommonSettings(state: ApiSettingsUiState) {
        val supportToolCall = aiRepository.testToolCallSupport(
            apiHost = state.apiHost,
            apiKey = state.apiKey,
            model = state.selectedModel,
            providerId = OpenAiCompatibleProvider.Id,
        )

        if (isMultimodal) {
            modelConfigRepository.saveMultimodal(
                ModelConfig(
                    id = MultimodalModelConfig.Id,
                    provider = MultimodalModelConfig.Provider,
                    name = MultimodalModelConfig.Name,
                    baseUrl = state.apiHost.trim().trimEnd('/'),
                    apiKey = state.apiKey.trim(),
                    modelName = state.selectedModel,
                    contextWindow = MultimodalModelConfig.ContextWindow,
                    maxOutputTokens = MultimodalModelConfig.MaxOutputTokens,
                    supportVision = true,
                    supportToolCall = supportToolCall,
                    supportReasoning = false,
                    temperature = MultimodalModelConfig.Temperature,
                    topP = MultimodalModelConfig.TopP,
                    enabled = true,
                ),
            )
        } else {
            modelConfigRepository.saveDefault(
                ModelConfig(
                    id = DefaultModelConfig.Id,
                    provider = DefaultModelConfig.Provider,
                    name = DefaultModelConfig.Name,
                    baseUrl = state.apiHost.trim().trimEnd('/'),
                    apiKey = state.apiKey.trim(),
                    modelName = state.selectedModel,
                    contextWindow = DefaultModelConfig.ContextWindow,
                    maxOutputTokens = DefaultModelConfig.MaxOutputTokens,
                    supportVision = false,
                    supportToolCall = supportToolCall,
                    supportReasoning = false,
                    temperature = DefaultModelConfig.Temperature,
                    topP = DefaultModelConfig.TopP,
                    enabled = true,
                ),
            )
        }
    }

    private fun clearSettings() {
        viewModelScope.launch {
            try {
                when {
                    isImageGeneration -> modelConfigRepository.deleteConfig(ImageGenerationModelConfig.Id)
                    isVoice -> {
                        modelConfigRepository.deleteConfig(VoiceModelConfig.Id)
                        modelConfigRepository.deleteConfig(VoiceCloneModelConfig.Id)
                    }
                    isMultimodal -> modelConfigRepository.deleteConfig(MultimodalModelConfig.Id)
                    else -> modelConfigRepository.deleteConfig(DefaultModelConfig.Id)
                }
                _effects.send(ApiSettingsEffect.NavigateBack)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun hasValidApiSettings(host: String, key: String): Boolean {
        val normalizedHost = host.trim()
        return key.isNotBlank() && normalizedHost.startsWith("https://")
    }

    private fun emitMessage(message: SettingsEffectMessage) {
        _effects.trySend(ApiSettingsEffect.ShowMessage(message))
    }
}
