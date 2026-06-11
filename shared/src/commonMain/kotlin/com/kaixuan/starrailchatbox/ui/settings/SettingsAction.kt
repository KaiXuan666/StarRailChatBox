package com.kaixuan.starrailchatbox.ui.settings

enum class SettingsItem {
    PROFILE,
    API_SETTINGS,
    MULTIMODAL_API_SETTINGS,
    IMAGE_GENERATION_API_SETTINGS,
    VOICE_API_SETTINGS,
    CHECK_UPDATE,
    MESSAGE_NOTIFICATION,
    THEME_STYLE,
    ABOUT_US,
    PRIVACY_SECURITY,
}

sealed interface SettingsAction {
    // 点击了设置项
    data class SettingsItemClicked(val item: SettingsItem) : SettingsAction
    
    // 复制到剪贴板
    data class CopyToClipboard(val text: String) : SettingsAction
}
