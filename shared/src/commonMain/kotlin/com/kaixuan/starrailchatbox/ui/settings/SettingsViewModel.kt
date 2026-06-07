package com.kaixuan.starrailchatbox.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.api.OpenAiRepository
import com.kaixuan.starrailchatbox.data.model.DefaultModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
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
    private val openAiRepository: OpenAiRepository,
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
            _uiState.update { state ->
                val selectedModel = settings?.modelName.orEmpty()
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
                )
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SettingsItemClicked -> handleSettingsItemClick(action.item)
            
            is SettingsAction.ApiHostChanged -> {
                _uiState.update { it.copy(apiHost = action.host) }
            }
            
            is SettingsAction.ApiKeyChanged -> {
                _uiState.update { it.copy(apiKey = action.key) }
            }
            
            SettingsAction.ToggleApiKeyVisibility -> {
                _uiState.update { it.copy(showApiKey = !it.showApiKey) }
            }
            
            SettingsAction.FetchModelsClicked -> {
                fetchModels()
            }
            
            is SettingsAction.SelectModel -> {
                _uiState.update { it.copy(selectedModel = action.model) }
            }
            
            SettingsAction.SaveApiSettingsClicked -> {
                saveApiSettings()
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
            SettingsItem.CHECK_UPDATE -> emitMessage(SettingsEffectMessage.SETTINGS_UPDATE_CHECK)
            SettingsItem.MESSAGE_NOTIFICATION -> emitMessage(SettingsEffectMessage.SETTINGS_NOTICE_NOT_READY)
            SettingsItem.THEME_STYLE -> {
                // 由框架层 MainAction 转发主题弹窗显示
            }
            SettingsItem.ABOUT_US -> emitMessage(SettingsEffectMessage.SETTINGS_ABOUT_INFO)
            SettingsItem.PRIVACY_SECURITY -> emitMessage(SettingsEffectMessage.SETTINGS_PRIVACY_INFO)
        }
    }

    private fun fetchModels() {
        val state = _uiState.value
        if (state.isFetchingModels) return
        if (!state.hasValidApiSettings()) {
            emitMessage(SettingsEffectMessage.SETTINGS_API_INVALID)
            return
        }

        _uiState.update { it.copy(isFetchingModels = true) }
        emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_START)

        scope().launch {
            when (
                val result = openAiRepository.getModels(
                    apiHost = state.apiHost,
                    apiKey = state.apiKey,
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
                is ApiResult.NetworkError,
                is ApiResult.UnexpectedError,
                -> {
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

        _uiState.update { state ->
            state.copy(
                isFetchingModels = false,
                modelsList = models,
                selectedModel = state.selectedModel.takeIf(models::contains) ?: models.first(),
            )
        }
        emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_SUCCESS)
    }

    private fun saveApiSettings() {
        val state = _uiState.value
        if (state.isSaving) return
        if (!state.hasValidApiSettings() || state.selectedModel.isBlank()) {
            emitMessage(SettingsEffectMessage.SETTINGS_API_INVALID)
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        scope().launch {
            try {
                val supportToolCall = openAiRepository.testToolCallSupport(
                    apiHost = state.apiHost,
                    apiKey = state.apiKey,
                    model = state.selectedModel,
                )

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
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(SettingsEffect.ApiSettingsSaved)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                t.printStackTrace()
                _uiState.update { it.copy(isSaving = false) }
                emitMessage(SettingsEffectMessage.SETTINGS_API_SAVE_FAILED)
            }
        }
    }

    private fun SettingsUiState.hasValidApiSettings(): Boolean {
        val normalizedHost = apiHost.trim()
        return apiKey.isNotBlank() &&
            normalizedHost.startsWith("https://")
    }

    private fun scope(): CoroutineScope = coroutineScope ?: viewModelScope

    private fun emitMessage(message: SettingsEffectMessage) {
        _effects.trySend(SettingsEffect.ShowMessage(message))
    }
}
