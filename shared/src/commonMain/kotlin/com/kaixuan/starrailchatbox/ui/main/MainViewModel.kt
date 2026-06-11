package com.kaixuan.starrailchatbox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import com.kaixuan.starrailchatbox.data.update.UpdateRepository
import com.kaixuan.starrailchatbox.getPlatform
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsStore: AppSettingsStore,
    private val updateRepository: UpdateRepository
) : ViewModel() {
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

        // 启动时自动检查更新
        checkUpdate(isManual = false)
    }

    private fun checkUpdate(isManual: Boolean) {
        if (isManual) {
            _effects.trySend(MainEffect.ShowMessage(MainEffectMessage.CHECKING_FOR_UPDATE))
        }
        viewModelScope.launch {
            when (val result = updateRepository.checkUpdate(isManual)) {
                is ApiResult.Success -> {
                    val info = result.value
                    val currentVersionCode = getPlatform().versionCode
                    if (info.versionCode > currentVersionCode) {
                        _uiState.update {
                            it.copy(
                                showUpdateDialog = true,
                                updateInfo = UpdateInfo(
                                    version = info.versionName,
                                    description = info.updateLog,
                                    downloadUrl = info.downloadUrl,
                                    isForceUpdate = info.forceUpdate
                                )
                            )
                        }
                    } else if (isManual) {
                        _effects.trySend(MainEffect.ShowMessage(MainEffectMessage.ALREADY_LATEST_VERSION))
                    }
                }
                else -> {
                    if (isManual) {
                        _effects.trySend(MainEffect.ShowMessage(MainEffectMessage.UPDATE_CHECK_FAILED))
                    }
                }
            }
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.NavigationSelected,
            is MainAction.NavigateTo,
            MainAction.PopBackStack,
            -> Unit

            is MainAction.SettingsItemClicked -> {
                when (action.item) {
                    MainSettingsItem.PROFILE,
                    MainSettingsItem.API_SETTINGS,
                    MainSettingsItem.MULTIMODAL_API_SETTINGS,
                    MainSettingsItem.IMAGE_GENERATION_API_SETTINGS,
                    MainSettingsItem.VOICE_API_SETTINGS,
                    MainSettingsItem.ABOUT_US,
                    MainSettingsItem.PRIVACY_SECURITY,
                    -> Unit
                    MainSettingsItem.THEME_STYLE -> {
                        _uiState.update { it.copy(showThemeDialog = true) }
                    }
                    MainSettingsItem.CHECK_UPDATE -> {
                        checkUpdate(isManual = true)
                    }
                    else -> {
                        // 其它非核心主逻辑（更新检查、通知等）由 SettingsViewModel 处理
                    }
                }
            }

            MainAction.UpdateDialogDismiss -> {
                val isForceUpdate = _uiState.value.updateInfo?.isForceUpdate == true
                if (!isForceUpdate) {
                    _uiState.update { it.copy(showUpdateDialog = false) }
                }
            }

            MainAction.UpdateDialogConfirm -> {
                val isForceUpdate = _uiState.value.updateInfo?.isForceUpdate == true
                if (!isForceUpdate) {
                    _uiState.update { it.copy(showUpdateDialog = false) }
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
