package com.kaixuan.starrailchatbox.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.OpenAiCompatibleProvider
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.DefaultModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.MultimodalModelConfig
import com.kaixuan.starrailchatbox.data.model.VoiceCloneModelConfig
import com.kaixuan.starrailchatbox.data.model.VoiceModelConfig
import com.kaixuan.starrailchatbox.data.settings.ApiSettingsDefaults
import com.kaixuan.starrailchatbox.data.settings.localApiSettingsDefaults
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val aiRepository: AiRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val defaultApiSettings: ApiSettingsDefaults = localApiSettingsDefaults(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        scope().launch {
            val settings = modelConfigRepository.getDefault()
            val mmSettings = modelConfigRepository.getMultimodal()
            val voiceSettings = modelConfigRepository.getVoice()
            val voiceCloneSettings = modelConfigRepository.getVoiceClone()
            _uiState.update { state ->
                val selectedModel = settings?.modelName.orEmpty()
                val mmSelectedModel = mmSettings?.modelName.orEmpty()
                val voiceSelectedModel = voiceSettings?.modelName.orEmpty()
                val voiceSelectedCloneModel = voiceCloneSettings?.modelName.orEmpty()

                state.copy(
                    apiHost = settings?.baseUrl
                        ?.takeIf(String::isNotBlank)
                        ?: defaultApiSettings.apiHost.takeIf(String::isNotBlank)
                        ?: state.apiHost,
                    apiKey = settings?.apiKey
                        ?.takeIf(String::isNotBlank)
                        ?: defaultApiSettings.apiKey,
                    selectedModel = selectedModel,
                    modelsList = listOfNotNull(selectedModel.takeIf(String::isNotBlank)),

                    multimodalApiHost = mmSettings?.baseUrl
                        ?.takeIf(String::isNotBlank)
                        ?: defaultApiSettings.apiHost.takeIf(String::isNotBlank)
                        ?: state.multimodalApiHost,
                    multimodalApiKey = mmSettings?.apiKey
                        ?.takeIf(String::isNotBlank)
                        ?: defaultApiSettings.apiKey,
                    multimodalSelectedModel = mmSelectedModel,
                    multimodalModelsList = listOfNotNull(mmSelectedModel.takeIf(String::isNotBlank)),

                    voiceApiHost = voiceSettings?.baseUrl
                        ?.takeIf(String::isNotBlank)
                        ?: defaultApiSettings.apiHost.takeIf(String::isNotBlank)
                        ?: state.voiceApiHost,
                    voiceApiKey = voiceSettings?.apiKey
                        ?.takeIf(String::isNotBlank)
                        ?: defaultApiSettings.apiKey,
                    voiceSelectedModel = voiceSelectedModel,
                    voiceSelectedCloneModel = voiceSelectedCloneModel,
                    voiceModelsList = listOfNotNull(
                        voiceSelectedModel.takeIf(String::isNotBlank),
                        voiceSelectedCloneModel.takeIf(String::isNotBlank)
                    ).distinct(),
                )
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SettingsItemClicked -> handleSettingsItemClick(action.item)
            
            is SettingsAction.ApiHostChanged -> {
                _uiState.update { state ->
                    when {
                        action.isVoice -> state.copy(voiceApiHost = action.host)
                        action.isMultimodal -> state.copy(multimodalApiHost = action.host)
                        else -> state.copy(apiHost = action.host)
                    }
                }
            }
            
            is SettingsAction.ApiKeyChanged -> {
                _uiState.update { state ->
                    when {
                        action.isVoice -> state.copy(voiceApiKey = action.key)
                        action.isMultimodal -> state.copy(multimodalApiKey = action.key)
                        else -> state.copy(apiKey = action.key)
                    }
                }
            }
            
            SettingsAction.ToggleApiKeyVisibility -> {
                _uiState.update { it.copy(showApiKey = !it.showApiKey) }
            }
            
            SettingsAction.ToggleMultimodalApiKeyVisibility -> {
                _uiState.update { it.copy(multimodalShowApiKey = !it.multimodalShowApiKey) }
            }

            SettingsAction.ToggleVoiceApiKeyVisibility -> {
                _uiState.update { it.copy(voiceShowApiKey = !it.voiceShowApiKey) }
            }
            
            SettingsAction.FetchModelsClicked -> {
                fetchModels(isMultimodal = false, isVoice = false)
            }
            
            SettingsAction.FetchMultimodalModelsClicked -> {
                fetchModels(isMultimodal = true, isVoice = false)
            }

            SettingsAction.FetchVoiceModelsClicked -> {
                fetchModels(isMultimodal = false, isVoice = true)
            }
            
            is SettingsAction.SelectModel -> {
                _uiState.update { state ->
                    when {
                        action.isVoiceClone -> state.copy(voiceSelectedCloneModel = action.model)
                        action.isVoice -> state.copy(voiceSelectedModel = action.model)
                        action.isMultimodal -> state.copy(multimodalSelectedModel = action.model)
                        else -> state.copy(selectedModel = action.model)
                    }
                }
            }
            
            SettingsAction.SaveApiSettingsClicked -> {
                saveApiSettings(isMultimodal = false, isVoice = false)
            }
            
            SettingsAction.SaveMultimodalApiSettingsClicked -> {
                saveApiSettings(isMultimodal = true, isVoice = false)
            }

            SettingsAction.SaveVoiceApiSettingsClicked -> {
                saveApiSettings(isMultimodal = false, isVoice = true)
            }

            is SettingsAction.ClearApiSettingsClicked -> {
                clearApiSettings(isMultimodal = action.isMultimodal, isVoice = action.isVoice)
            }
        }
    }

    private fun handleSettingsItemClick(item: SettingsItem) {
        when (item) {
            SettingsItem.PROFILE -> {
                // 由框架层 MainAction 转发导航压栈
            }
            SettingsItem.API_SETTINGS -> {
                // 由框架层 MainAction 转发导航压栈
            }
            SettingsItem.MULTIMODAL_API_SETTINGS -> {
                // 由框架层 MainAction 转发导航压栈
            }
            SettingsItem.VOICE_API_SETTINGS -> {
                // 由框架层 MainAction 转发导航压栈
            }
            SettingsItem.CHECK_UPDATE -> emitMessage(SettingsEffectMessage.SETTINGS_UPDATE_CHECK)
            SettingsItem.MESSAGE_NOTIFICATION -> emitMessage(SettingsEffectMessage.SETTINGS_NOTICE_NOT_READY)
            SettingsItem.THEME_STYLE -> {
                // 由框架层 MainAction 转发主题弹窗显示
            }
            SettingsItem.ABOUT_US -> emitMessage(SettingsEffectMessage.SETTINGS_ABOUT_INFO)
            SettingsItem.PRIVACY_SECURITY -> emitMessage(SettingsEffectMessage.SETTINGS_PRIVACY_INFO)
        }
    }

    private fun fetchModels(isMultimodal: Boolean, isVoice: Boolean = false) {
        val state = _uiState.value
        val isFetching = when {
            isVoice -> state.voiceIsFetchingModels
            isMultimodal -> state.multimodalIsFetchingModels
            else -> state.isFetchingModels
        }
        if (isFetching) return

        val apiHost = when {
            isVoice -> state.voiceApiHost
            isMultimodal -> state.multimodalApiHost
            else -> state.apiHost
        }
        val apiKey = when {
            isVoice -> state.voiceApiKey
            isMultimodal -> state.multimodalApiKey
            else -> state.apiKey
        }

        if (!hasValidApiSettings(apiHost, apiKey)) {
            emitMessage(SettingsEffectMessage.SETTINGS_API_INVALID)
            return
        }

        _uiState.update {
            when {
                isVoice -> it.copy(voiceIsFetchingModels = true)
                isMultimodal -> it.copy(multimodalIsFetchingModels = true)
                else -> it.copy(isFetchingModels = true)
            }
        }
        emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_START)

        scope().launch {
            when (
                val result = aiRepository.getModels(
                    apiHost = apiHost,
                    apiKey = apiKey,
                    providerId = OpenAiCompatibleProvider.Id,
                )
            ) {
                is ApiResult.Success -> handleModelsLoaded(result.value, isMultimodal, isVoice)
                is ApiResult.HttpError -> {
                    _uiState.update {
                        when {
                            isVoice -> it.copy(voiceIsFetchingModels = false)
                            isMultimodal -> it.copy(multimodalIsFetchingModels = false)
                            else -> it.copy(isFetchingModels = false)
                        }
                    }
                    emitMessage(
                        if (result.statusCode == 401 || result.statusCode == 403) {
                            SettingsEffectMessage.SETTINGS_API_AUTH_FAILED
                        } else {
                            SettingsEffectMessage.SETTINGS_API_FETCH_FAILED
                        },
                    )
                }
                is ApiResult.NetworkError,
                is ApiResult.UnexpectedError,
                -> {
                    _uiState.update {
                        when {
                            isVoice -> it.copy(voiceIsFetchingModels = false)
                            isMultimodal -> it.copy(multimodalIsFetchingModels = false)
                            else -> it.copy(isFetchingModels = false)
                        }
                    }
                    emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_FAILED)
                }
            }
        }
    }

    private fun handleModelsLoaded(models: List<String>, isMultimodal: Boolean, isVoice: Boolean = false) {
        if (models.isEmpty()) {
            _uiState.update {
                when {
                    isVoice -> it.copy(voiceIsFetchingModels = false, voiceModelsList = emptyList())
                    isMultimodal -> it.copy(multimodalIsFetchingModels = false, multimodalModelsList = emptyList())
                    else -> it.copy(isFetchingModels = false, modelsList = emptyList())
                }
            }
            emitMessage(SettingsEffectMessage.SETTINGS_API_NO_MODELS)
            return
        }

        _uiState.update { state ->
            when {
                isVoice -> {
                    state.copy(
                        voiceIsFetchingModels = false,
                        voiceModelsList = models,
                        voiceSelectedModel = state.voiceSelectedModel.takeIf(models::contains) 
                            ?: models.firstOrNull { !it.contains("clone") && !it.contains("design") } 
                            ?: models.first(),
                        voiceSelectedCloneModel = state.voiceSelectedCloneModel.takeIf(models::contains) ?: "",
                    )
                }
                isMultimodal -> {
                    state.copy(
                        multimodalIsFetchingModels = false,
                        multimodalModelsList = models,
                        multimodalSelectedModel = state.multimodalSelectedModel.takeIf(models::contains) ?: models.first(),
                    )
                }
                else -> {
                    state.copy(
                        isFetchingModels = false,
                        modelsList = models,
                        selectedModel = state.selectedModel.takeIf(models::contains) ?: models.first(),
                    )
                }
            }
        }
        emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_SUCCESS)
    }

    private fun saveApiSettings(isMultimodal: Boolean, isVoice: Boolean = false) {
        val state = _uiState.value
        val isSaving = when {
            isVoice -> state.voiceIsSaving
            isMultimodal -> state.multimodalIsSaving
            else -> state.isSaving
        }
        if (isSaving) return

        val apiHost = when {
            isVoice -> state.voiceApiHost
            isMultimodal -> state.multimodalApiHost
            else -> state.apiHost
        }
        val apiKey = when {
            isVoice -> state.voiceApiKey
            isMultimodal -> state.multimodalApiKey
            else -> state.apiKey
        }
        val selectedModel = when {
            isVoice -> state.voiceSelectedModel
            isMultimodal -> state.multimodalSelectedModel
            else -> state.selectedModel
        }

        if (!hasValidApiSettings(apiHost, apiKey) || selectedModel.isBlank()) {
            emitMessage(SettingsEffectMessage.SETTINGS_API_INVALID)
            return
        }

        _uiState.update {
            when {
                isVoice -> it.copy(voiceIsSaving = true)
                isMultimodal -> it.copy(multimodalIsSaving = true)
                else -> it.copy(isSaving = true)
            }
        }
        scope().launch {
            try {
                if (isVoice) {
                    val voiceSelectedModel = state.voiceSelectedModel
                    val voiceSelectedCloneModel = state.voiceSelectedCloneModel

                    if (voiceSelectedModel.isNotBlank()) {
                        modelConfigRepository.saveVoice(
                            ModelConfig(
                                id = VoiceModelConfig.Id,
                                provider = VoiceModelConfig.Provider,
                                name = VoiceModelConfig.Name,
                                baseUrl = apiHost.trim().trimEnd('/'),
                                apiKey = apiKey.trim(),
                                modelName = voiceSelectedModel,
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

                    if (voiceSelectedCloneModel.isNotBlank()) {
                        modelConfigRepository.saveVoiceClone(
                            ModelConfig(
                                id = VoiceCloneModelConfig.Id,
                                provider = VoiceCloneModelConfig.Provider,
                                name = VoiceCloneModelConfig.Name,
                                baseUrl = apiHost.trim().trimEnd('/'),
                                apiKey = apiKey.trim(),
                                modelName = voiceSelectedCloneModel,
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
                } else {
                    val supportToolCall = aiRepository.testToolCallSupport(
                        apiHost = apiHost,
                        apiKey = apiKey,
                        model = selectedModel,
                        providerId = OpenAiCompatibleProvider.Id,
                    )

                    if (isMultimodal) {
                        modelConfigRepository.saveMultimodal(
                            ModelConfig(
                                id = MultimodalModelConfig.Id,
                                provider = MultimodalModelConfig.Provider,
                                name = MultimodalModelConfig.Name,
                                baseUrl = apiHost.trim().trimEnd('/'),
                                apiKey = apiKey.trim(),
                                modelName = selectedModel,
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
                                baseUrl = apiHost.trim().trimEnd('/'),
                                apiKey = apiKey.trim(),
                                modelName = selectedModel,
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
                _uiState.update {
                    when {
                        isVoice -> it.copy(voiceIsSaving = false)
                        isMultimodal -> it.copy(multimodalIsSaving = false)
                        else -> it.copy(isSaving = false)
                    }
                }
                _effects.send(SettingsEffect.ApiSettingsSaved)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                t.printStackTrace()
                _uiState.update {
                    when {
                        isVoice -> it.copy(voiceIsSaving = false)
                        isMultimodal -> it.copy(multimodalIsSaving = false)
                        else -> it.copy(isSaving = false)
                    }
                }
                emitMessage(SettingsEffectMessage.SETTINGS_API_SAVE_FAILED)
            }
        }
    }

    private fun clearApiSettings(isMultimodal: Boolean, isVoice: Boolean) {
        scope().launch {
            try {
                when {
                    isVoice -> {
                        modelConfigRepository.deleteConfig(VoiceModelConfig.Id)
                        modelConfigRepository.deleteConfig(VoiceCloneModelConfig.Id)
                        _uiState.update { 
                            it.copy(
                                voiceApiKey = "",
                                voiceSelectedModel = "",
                                voiceSelectedCloneModel = "",
                                voiceModelsList = emptyList()
                            )
                        }
                    }
                    isMultimodal -> {
                        modelConfigRepository.deleteConfig(MultimodalModelConfig.Id)
                        _uiState.update {
                            it.copy(
                                multimodalApiKey = "",
                                multimodalSelectedModel = "",
                                multimodalModelsList = emptyList()
                            )
                        }
                    }
                    else -> {
                        modelConfigRepository.deleteConfig(DefaultModelConfig.Id)
                        _uiState.update {
                            it.copy(
                                apiKey = "",
                                selectedModel = "",
                                modelsList = emptyList()
                            )
                        }
                    }
                }
                _effects.send(SettingsEffect.NavigateBack)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun hasValidApiSettings(host: String, key: String): Boolean {
        val normalizedHost = host.trim()
        return key.isNotBlank() && normalizedHost.startsWith("https://")
    }

    private fun scope(): CoroutineScope = coroutineScope ?: viewModelScope

    private fun emitMessage(message: SettingsEffectMessage) {
        _effects.trySend(SettingsEffect.ShowMessage(message))
    }
}
