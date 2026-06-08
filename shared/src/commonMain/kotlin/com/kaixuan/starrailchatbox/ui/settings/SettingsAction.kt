package com.kaixuan.starrailchatbox.ui.settings

enum class SettingsItem {
    PROFILE,
    API_SETTINGS,
    MULTIMODAL_API_SETTINGS,
    VOICE_API_SETTINGS,
    CHECK_UPDATE,
    MESSAGE_NOTIFICATION,
    THEME_STYLE,
    ABOUT_US,
    PRIVACY_SECURITY,
}

sealed interface SettingsAction {
    // 点击了设置项（主要由 SettingsViewModel 拦截 CHECK_UPDATE, MESSAGE_NOTIFICATION, ABOUT_US, PRIVACY_SECURITY 并发射 Side-effect）
    data class SettingsItemClicked(val item: SettingsItem) : SettingsAction
    
    // API 设置修改
    data class ApiHostChanged(val host: String, val isMultimodal: Boolean = false, val isVoice: Boolean = false) : SettingsAction
    data class ApiKeyChanged(val key: String, val isMultimodal: Boolean = false, val isVoice: Boolean = false) : SettingsAction
    data object ToggleApiKeyVisibility : SettingsAction
    data object ToggleMultimodalApiKeyVisibility : SettingsAction
    data object ToggleVoiceApiKeyVisibility : SettingsAction
    
    // 模型拉取与选择
    data object FetchModelsClicked : SettingsAction
    data object FetchMultimodalModelsClicked : SettingsAction
    data object FetchVoiceModelsClicked : SettingsAction
    data class SelectModel(val model: String, val isMultimodal: Boolean = false, val isVoice: Boolean = false, val isVoiceClone: Boolean = false) : SettingsAction
    
    // 点击保存 API 设置
    data object SaveApiSettingsClicked : SettingsAction
    data object SaveMultimodalApiSettingsClicked : SettingsAction
    data object SaveVoiceApiSettingsClicked : SettingsAction

    // 清空配置并退出
    data class ClearApiSettingsClicked(val isMultimodal: Boolean = false, val isVoice: Boolean = false) : SettingsAction
}
