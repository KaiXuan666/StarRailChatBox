package com.kaixuan.starrailchatbox.ui.settings

sealed interface SettingsEffect {
    data class ShowMessage(val message: SettingsEffectMessage) : SettingsEffect
    data object ApiSettingsSaved : SettingsEffect
    data object NavigateBack : SettingsEffect
}

enum class SettingsEffectMessage {
    SETTINGS_API_NOT_READY,
    SETTINGS_UPDATE_CHECK,
    SETTINGS_NOTICE_NOT_READY,
    SETTINGS_ABOUT_INFO,
    SETTINGS_PRIVACY_INFO,
    SETTINGS_API_SAVED,
    SETTINGS_API_FETCH_START,
    SETTINGS_API_FETCH_SUCCESS,
    SETTINGS_API_INVALID,
    SETTINGS_API_AUTH_FAILED,
    SETTINGS_API_FETCH_FAILED,
    SETTINGS_API_NO_MODELS,
    SETTINGS_API_SAVE_FAILED,
}
