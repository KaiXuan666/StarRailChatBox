package com.kaixuan.starrailchatbox.ui.settings

sealed interface SettingsEffect {
    data class ShowMessage(val message: SettingsEffectMessage) : SettingsEffect
}

enum class SettingsEffectMessage {
    SETTINGS_API_NOT_READY,
    SETTINGS_UPDATE_CHECK,
    SETTINGS_NOTICE_NOT_READY,
    SETTINGS_ABOUT_INFO,
    SETTINGS_PRIVACY_INFO,
    SETTINGS_API_SAVED,
    SETTINGS_API_FETCH_START,
    SETTINGS_API_FETCH_SUCCESS
}
