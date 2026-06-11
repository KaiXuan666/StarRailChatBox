package com.kaixuan.starrailchatbox.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SettingsItemClicked -> {
                // 处理不需要导航的设置项点击（目前大部分由 MainNavigationContainer 处理）
            }
            is SettingsAction.CopyToClipboard -> {
                _effects.trySend(SettingsEffect.ShowMessage(SettingsEffectMessage.SETTINGS_COPIED_SUCCESS))
            }
        }
    }
}
