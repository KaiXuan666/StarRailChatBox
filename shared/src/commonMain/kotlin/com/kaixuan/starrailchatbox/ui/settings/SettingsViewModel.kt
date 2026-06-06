package com.kaixuan.starrailchatbox.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

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
                _effects.trySend(SettingsEffect.ShowMessage(SettingsEffectMessage.SETTINGS_API_SAVED))
            }
        }
    }

    private fun handleSettingsItemClick(item: SettingsItem) {
        when (item) {
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
        if (_uiState.value.isFetchingModels) return
        _uiState.update { it.copy(isFetchingModels = true) }
        emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_START)
        viewModelScope.launch {
            delay(1500)
            _uiState.update { state ->
                val newModels = listOf("gpt-4o-mini", "gpt-4.1", "deepseek-chat", "qwen-plus", "claude-3.5-sonnet")
                val nextSelection = if (state.selectedModel in newModels) state.selectedModel else newModels.first()
                state.copy(
                    isFetchingModels = false,
                    modelsList = newModels,
                    selectedModel = nextSelection
                )
            }
            emitMessage(SettingsEffectMessage.SETTINGS_API_FETCH_SUCCESS)
        }
    }

    private fun emitMessage(message: SettingsEffectMessage) {
        _effects.trySend(SettingsEffect.ShowMessage(message))
    }
}
