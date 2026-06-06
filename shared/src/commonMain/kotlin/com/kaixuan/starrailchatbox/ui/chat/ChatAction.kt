package com.kaixuan.starrailchatbox.ui.chat

sealed interface ChatAction {
    data class CharacterSelected(val character: CharacterId) : ChatAction
    data class MessageChanged(val message: String) : ChatAction
    data object SendClicked : ChatAction
    data class QuickReplyClicked(val message: String) : ChatAction
    data class NavigationSelected(
        val destination: NavigationDestination,
    ) : ChatAction
    data class HeaderActionClicked(val action: HeaderAction) : ChatAction
    data class ComposerActionClicked(val action: ComposerAction) : ChatAction
    data class SettingsItemClicked(val item: SettingsItem) : ChatAction
    data class ThemeDialogConfirm(val themeOverride: Boolean?) : ChatAction
    data object ThemeDialogDismiss : ChatAction
    data class ApiHostChanged(val host: String) : ChatAction
    data class ApiKeyChanged(val key: String) : ChatAction
    data object ToggleApiKeyVisibility : ChatAction
    data object FetchModelsClicked : ChatAction
    data class SelectModel(val model: String) : ChatAction
    data object SaveApiSettingsClicked : ChatAction
    data object BackFromApiSettings : ChatAction
}

enum class SettingsItem {
    API_SETTINGS,
    CHECK_UPDATE,
    MESSAGE_NOTIFICATION,
    THEME_STYLE,
    ABOUT_US,
    PRIVACY_SECURITY,
}

enum class HeaderAction {
    VOICE,
    PROFILE,
    SETTINGS,
}

enum class ComposerAction {
    ATTACH,
    EMOJI,
    VOICE,
}
