package com.kaixuan.starrailchatbox.ui.settings

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
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
        // 观察配置变化，更新主页面的配置状态标记
        modelConfigRepository.observeDefault().onEach { config ->
            _uiState.update { it.copy(isDefaultConfigured = config?.apiKey?.isNotBlank() == true) }
        }.launchIn(scope())

        modelConfigRepository.observeMultimodal().onEach { config ->
            _uiState.update { it.copy(isMultimodalConfigured = config?.apiKey?.isNotBlank() == true) }
        }.launchIn(scope())

        modelConfigRepository.observeVoice().onEach { config ->
            val voiceCloneConfig = modelConfigRepository.getVoiceClone()
            _uiState.update { it.copy(isVoiceConfigured = config?.apiKey?.isNotBlank() == true || voiceCloneConfig?.apiKey?.isNotBlank() == true) }
        }.launchIn(scope())

        modelConfigRepository.observeVoiceClone().onEach { config ->
            val voiceConfig = modelConfigRepository.getVoice()
            _uiState.update { it.copy(isVoiceConfigured = config?.apiKey?.isNotBlank() == true || voiceConfig?.apiKey?.isNotBlank() == true) }
        }.launchIn(scope())

        modelConfigRepository.observeImageGeneration().onEach { config ->
            _uiState.update { it.copy(isImageGenerationConfigured = config?.apiKey?.isNotBlank() == true) }
        }.launchIn(scope())

        scope().launch {
            val settings = modelConfigRepository.getDefault()
            val mmSettings = modelConfigRepository.getMultimodal()
            val voiceSettings = modelConfigRepository.getVoice()
            val voiceCloneSettings = modelConfigRepository.getVoiceClone()
            val imageGenSettings = modelConfigRepository.getImageGeneration()
            Napier.d { "settings=$settings" }
            Napier.d { "mmSettings=$mmSettings" }
            _uiState.update { state ->
                // 1. 提取 Host 解析逻辑
                fun resolveHost(dbHost: String?, currentHost: String, useDefaultHost: Boolean = false) =
                    dbHost?.takeIf(String::isNotBlank)
                        ?: (if (useDefaultHost) defaultApiSettings.apiHost.takeIf(String::isNotBlank) else null)
                        ?: currentHost

                // 2. 提取 Key 提取逻辑
                fun resolveKey(dbKey: String?, currentKey: String, useDefaultKey: Boolean = false) =
                    dbKey?.takeIf(String::isNotBlank)
                        ?: (if (useDefaultKey) defaultApiSettings.apiKey.takeIf(String::isNotBlank) else null)
                        ?: currentKey

                // 4. 提取 Model 列表过滤逻辑
                fun createModelList(vararg models: String?) =
                    models.mapNotNull { it?.takeIf(String::isNotBlank) }.distinct()

                val model = settings?.modelName.orEmpty()
                val mmModel = mmSettings?.modelName.orEmpty()
                val voiceModel = voiceSettings?.modelName.orEmpty()
                val voiceCloneModel = voiceCloneSettings?.modelName.orEmpty()
                val imageGenModel = imageGenSettings?.modelName.orEmpty()

                state.copy(
                    // 标准模型配置
                    apiHost = resolveHost(settings?.baseUrl, state.apiHost, useDefaultHost = true),
                    apiKey = resolveKey(settings?.apiKey, state.apiKey, useDefaultKey = true),
                    selectedModel = model,
                    modelsList = createModelList(model),

                    // 多模态模型配置
                    multimodalApiHost = resolveHost(mmSettings?.baseUrl, state.multimodalApiHost),
                    multimodalApiKey = resolveKey(mmSettings?.apiKey, state.multimodalApiKey),
                    multimodalSelectedModel = mmModel,
                    multimodalModelsList = createModelList(mmModel),

                    // 语音模型配置
                    voiceApiHost = resolveHost(voiceSettings?.baseUrl, state.voiceApiHost),
                    voiceApiKey = resolveKey(voiceSettings?.apiKey, state.voiceApiKey),
                    voiceSelectedModel = voiceModel,
                    voiceSelectedCloneModel = voiceCloneModel,
                    voiceModelsList = createModelList(voiceModel, voiceCloneModel),

                    // 图片生成配置
                    imageGenerationApiHost = resolveHost(imageGenSettings?.baseUrl, state.imageGenerationApiHost),
                    imageGenerationApiKey = resolveKey(imageGenSettings?.apiKey, state.imageGenerationApiKey),
                    imageGenerationSelectedModel = imageGenModel,
                    imageGenerationModelsList = createModelList(imageGenModel)
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
                        action.isImageGeneration -> state.copy(imageGenerationApiHost = action.host)
                        action.isVoice -> state.copy(voiceApiHost = action.host)
                        action.isMultimodal -> state.copy(multimodalApiHost = action.host)
                        else -> state.copy(apiHost = action.host)
                    }
                }
            }
            
            is SettingsAction.ApiKeyChanged -> {
                _uiState.update { state ->
                    when {
                        action.isImageGeneration -> state.copy(imageGenerationApiKey = action.key)
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

            SettingsAction.ToggleImageGenerationApiKeyVisibility -> {
                _uiState.update { it.copy(imageGenerationShowApiKey = !it.imageGenerationShowApiKey) }
            }
            
            SettingsAction.FetchModelsClicked -> {
                fetchModels(isMultimodal = false, isVoice = false, isImageGeneration = false)
            }
            
            SettingsAction.FetchMultimodalModelsClicked -> {
                fetchModels(isMultimodal = true, isVoice = false, isImageGeneration = false)
            }

            SettingsAction.FetchVoiceModelsClicked -> {
                fetchModels(isMultimodal = false, isVoice = true, isImageGeneration = false)
            }

            SettingsAction.FetchImageGenerationModelsClicked -> {
                fetchModels(isMultimodal = false, isVoice = false, isImageGeneration = true)
            }
            
            is SettingsAction.SelectModel -> {
                _uiState.update { state ->
                    when {
                        action.isImageGeneration -> state.copy(imageGenerationSelectedModel = action.model)
                        action.isVoiceClone -> state.copy(voiceSelectedCloneModel = action.model)
                        action.isVoice -> state.copy(voiceSelectedModel = action.model)
                        action.isMultimodal -> state.copy(multimodalSelectedModel = action.model)
                        else -> state.copy(selectedModel = action.model)
                    }
                }
            }
            
            SettingsAction.SaveApiSettingsClicked -> {
                saveApiSettings(isMultimodal = false, isVoice = false, isImageGeneration = false)
            }
            
            SettingsAction.SaveMultimodalApiSettingsClicked -> {
                saveApiSettings(isMultimodal = true, isVoice = false, isImageGeneration = false)
            }

            SettingsAction.SaveVoiceApiSettingsClicked -> {
                saveApiSettings(isMultimodal = false, isVoice = true, isImageGeneration = false)
            }

            SettingsAction.SaveImageGenerationApiSettingsClicked -> {
                saveApiSettings(isMultimodal = false, isVoice = false, isImageGeneration = true)
            }

            is SettingsAction.ClearApiSettingsClicked -> {
                clearApiSettings(isMultimodal = action.isMultimodal, isVoice = action.isVoice, isImageGeneration = action.isImageGeneration)
            }

            is SettingsAction.CopyToClipboard -> {
                emitMessage(SettingsEffectMessage.SETTINGS_COPIED_SUCCESS)
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
            SettingsItem.IMAGE_GENERATION_API_SETTINGS -> {
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

    private fun fetchModels(isMultimodal: Boolean, isVoice: Boolean = false, isImageGeneration: Boolean = false) {
        val state = _uiState.value
        val isFetching = when {
            isImageGeneration -> state.imageGenerationIsFetchingModels
            isVoice -> state.voiceIsFetchingModels
            isMultimodal -> state.multimodalIsFetchingModels
            else -> state.isFetchingModels
        }
        if (isFetching) return

        val apiHost = when {
            isImageGeneration -> state.imageGenerationApiHost
            isVoice -> state.voiceApiHost
            isMultimodal -> state.multimodalApiHost
            else -> state.apiHost
        }
        val apiKey = when {
            isImageGeneration -> state.imageGenerationApiKey
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
                isImageGeneration -> it.copy(imageGenerationIsFetchingModels = true)
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
                is ApiResult.Success -> handleModelsLoaded(result.value, isMultimodal, isVoice, isImageGeneration)
                is ApiResult.HttpError -> {
                    _uiState.update {
                        when {
                            isImageGeneration -> it.copy(imageGenerationIsFetchingModels = false)
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
                            isImageGeneration -> it.copy(imageGenerationIsFetchingModels = false)
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

    private fun handleModelsLoaded(models: List<String>, isMultimodal: Boolean, isVoice: Boolean = false, isImageGeneration: Boolean = false) {
        if (models.isEmpty()) {
            _uiState.update {
                when {
                    isImageGeneration -> it.copy(imageGenerationIsFetchingModels = false, imageGenerationModelsList = emptyList())
                    isVoice -> it.copy(voiceIsFetchingModels = false, voiceModelsList = emptyList())
                    isMultimodal -> it.copy(multimodalIsFetchingModels = false, multimodalModelsList = emptyList())
                    else -> it.copy(isFetchingModels = false, modelsList = emptyList())
                }
            }
            emitMessage(SettingsEffectMessage.SETTINGS_API_NO_MODELS)
            return
        }

        val sortedModels = if (isVoice) {
            models.sortedByDescending { it.contains("tts", ignoreCase = true) }
        } else if (isImageGeneration) {
            models.sortedByDescending { 
                it.contains("image", ignoreCase = true) || 
                it.contains("dall-e", ignoreCase = true) || 
                it.contains("flux", ignoreCase = true) 
            }
        } else {
            models
        }

        _uiState.update { state ->
            when {
                isImageGeneration -> {
                    state.copy(
                        imageGenerationIsFetchingModels = false,
                        imageGenerationModelsList = sortedModels,
                        imageGenerationSelectedModel = state.imageGenerationSelectedModel.takeIf(sortedModels::contains) ?: sortedModels.first(),
                    )
                }
                isVoice -> {
                    state.copy(
                        voiceIsFetchingModels = false,
                        voiceModelsList = sortedModels,
                        voiceSelectedModel = state.voiceSelectedModel.takeIf(sortedModels::contains)
                            ?: sortedModels.firstOrNull { !it.contains("clone") && !it.contains("design") }
                            ?: sortedModels.first(),
                        voiceSelectedCloneModel = state.voiceSelectedCloneModel.takeIf(sortedModels::contains) ?: "",
                    )
                }
                isMultimodal -> {
                    state.copy(
                        multimodalIsFetchingModels = false,
                        multimodalModelsList = sortedModels,
                        multimodalSelectedModel = state.multimodalSelectedModel.takeIf(sortedModels::contains) ?: sortedModels.first(),
                    )
                }
                else -> {
                    state.copy(
                        isFetchingModels = false,
                        modelsList = sortedModels,
                        selectedModel = state.selectedModel.takeIf(sortedModels::contains) ?: sortedModels.first(),
                    )
                }
            }
        }
        emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_SUCCESS)
    }

    private fun saveApiSettings(isMultimodal: Boolean, isVoice: Boolean = false, isImageGeneration: Boolean = false) {
        val state = _uiState.value
        val isSaving = when {
            isImageGeneration -> state.imageGenerationIsSaving
            isVoice -> state.voiceIsSaving
            isMultimodal -> state.multimodalIsSaving
            else -> state.isSaving
        }
        if (isSaving) return

        val apiHost = when {
            isImageGeneration -> state.imageGenerationApiHost
            isVoice -> state.voiceApiHost
            isMultimodal -> state.multimodalApiHost
            else -> state.apiHost
        }
        val apiKey = when {
            isImageGeneration -> state.imageGenerationApiKey
            isVoice -> state.voiceApiKey
            isMultimodal -> state.multimodalApiKey
            else -> state.apiKey
        }
        val selectedModel = when {
            isImageGeneration -> state.imageGenerationSelectedModel
            isVoice -> state.voiceSelectedModel
            isMultimodal -> state.multimodalSelectedModel
            else -> state.selectedModel
        }

        if (!hasValidApiSettings(apiHost, apiKey)) {
            emitMessage(SettingsEffectMessage.SETTINGS_API_INVALID)
            return
        }

        if (selectedModel.isBlank()) {
            emitMessage(SettingsEffectMessage.SETTINGS_API_NO_MODELS)
            return
        }

        _uiState.update {
            when {
                isImageGeneration -> it.copy(imageGenerationIsSaving = true)
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
                } else if (isImageGeneration) {
                    modelConfigRepository.saveImageGeneration(
                        ModelConfig(
                            id = ImageGenerationModelConfig.Id,
                            provider = ImageGenerationModelConfig.Provider,
                            name = ImageGenerationModelConfig.Name,
                            baseUrl = apiHost.trim().trimEnd('/'),
                            apiKey = apiKey.trim(),
                            modelName = selectedModel,
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
                        isImageGeneration -> it.copy(imageGenerationIsSaving = false)
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
                        isImageGeneration -> it.copy(imageGenerationIsSaving = false)
                        isVoice -> it.copy(voiceIsSaving = false)
                        isMultimodal -> it.copy(multimodalIsSaving = false)
                        else -> it.copy(isSaving = false)
                    }
                }
                emitMessage(SettingsEffectMessage.SETTINGS_API_SAVE_FAILED)
            }
        }
    }

    private fun clearApiSettings(isMultimodal: Boolean, isVoice: Boolean, isImageGeneration: Boolean) {
        scope().launch {
            try {
                when {
                    isImageGeneration -> {
                        modelConfigRepository.deleteConfig(ImageGenerationModelConfig.Id)
                        _uiState.update {
                            it.copy(
                                imageGenerationApiKey = "",
                                imageGenerationSelectedModel = "",
                                imageGenerationModelsList = emptyList()
                            )
                        }
                    }
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
