package com.kaixuan.starrailchatbox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import com.kaixuan.starrailchatbox.ui.navigation.Route
import kotlinx.coroutines.launch

class MainViewModel(private val settingsStore: AppSettingsStore) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        settingsStore.darkThemeOverride
            .onEach { theme ->
                _uiState.update { it.copy(darkThemeOverride = theme) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.NavigationSelected -> {
                _uiState.update {
                    it.copy(backStack = listOf(action.route))
                }
            }

            is MainAction.NavigateTo -> {
                _uiState.update { state ->
                    if (state.backStack.lastOrNull() == action.route) {
                        state
                    } else {
                        state.copy(backStack = state.backStack + action.route)
                    }
                }
            }

            MainAction.PopBackStack -> {
                _uiState.update { state ->
                    if (state.backStack.size > 1) {
                        state.copy(backStack = state.backStack.dropLast(1))
                    } else {
                        state
                    }
                }
            }

            is MainAction.SettingsItemClicked -> {
                when (action.item) {
                    MainSettingsItem.PROFILE -> {
                        _uiState.update { state ->
                            state.copy(backStack = state.backStack + Route.Profile)
                        }
                    }
                    MainSettingsItem.API_SETTINGS -> {
                        _uiState.update { state ->
                            state.copy(backStack = state.backStack + Route.ApiSettings)
                        }
                    }
                    MainSettingsItem.MULTIMODAL_API_SETTINGS -> {
                        _uiState.update { state ->
                            state.copy(backStack = state.backStack + Route.MultimodalApiSettings)
                        }
                    }
                    MainSettingsItem.IMAGE_GENERATION_API_SETTINGS -> {
                        _uiState.update { state ->
                            state.copy(backStack = state.backStack + Route.ImageGenerationApiSettings)
                        }
                    }
                    MainSettingsItem.VOICE_API_SETTINGS -> {
                        _uiState.update { state ->
                            state.copy(backStack = state.backStack + Route.VoiceApiSettings)
                        }
                    }
                    MainSettingsItem.THEME_STYLE -> {
                        _uiState.update { it.copy(showThemeDialog = true) }
                    }
                    MainSettingsItem.ABOUT_US -> {
                        _uiState.update { state ->
                            state.copy(backStack = state.backStack + Route.About)
                        }
                    }
                    else -> {
                        // 其它非核心主逻辑（更新检查、通知等）由 SettingsViewModel 处理
                    }
                }
            }

            is MainAction.ThemeDialogConfirm -> {
                viewModelScope.launch {
                    settingsStore.setDarkThemeOverride(action.themeOverride)
                }
                _uiState.update { state ->
                    state.copy(showThemeDialog = false)
                }
                _effects.trySend(MainEffect.ShowMessage(MainEffectMessage.THEME_CHANGED))
            }

            MainAction.ThemeDialogDismiss -> {
                _uiState.update { it.copy(showThemeDialog = false) }
            }

            is MainAction.ShowMessage -> {
                _effects.trySend(MainEffect.ShowMessage(action.message))
            }
        }
    }
}
