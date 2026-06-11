package com.kaixuan.starrailchatbox.ui.settings.api

import com.kaixuan.starrailchatbox.ui.settings.SettingsEffectMessage

sealed interface ApiSettingsEffect {
    data class ShowMessage(val message: SettingsEffectMessage) : ApiSettingsEffect
    data object ApiSettingsSaved : ApiSettingsEffect
    data object NavigateBack : ApiSettingsEffect
}
