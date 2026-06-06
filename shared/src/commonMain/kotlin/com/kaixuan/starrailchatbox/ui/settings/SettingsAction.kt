package com.kaixuan.starrailchatbox.ui.settings

enum class SettingsItem {
    API_SETTINGS,
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
    data class ApiHostChanged(val host: String) : SettingsAction
    data class ApiKeyChanged(val key: String) : SettingsAction
    data object ToggleApiKeyVisibility : SettingsAction
    
    // 模型拉取与选择
    data object FetchModelsClicked : SettingsAction
    data class SelectModel(val model: String) : SettingsAction
    
    // 点击保存 API 设置
    data object SaveApiSettingsClicked : SettingsAction
}
