package com.kaixuan.starrailchatbox.ui.settings.api

sealed interface ApiSettingsAction {
    data class ApiHostChanged(val host: String) : ApiSettingsAction
    data class ApiKeyChanged(val key: String) : ApiSettingsAction
    data object ToggleApiKeyVisibility : ApiSettingsAction
    data object FetchModelsClicked : ApiSettingsAction
    data class SelectModel(val model: String, val isCloneModel: Boolean = false) : ApiSettingsAction
    data class ApiProviderSelected(val providerId: String) : ApiSettingsAction
    data class ImageProviderSelected(val providerId: String) : ApiSettingsAction
    data object SaveSettingsClicked : ApiSettingsAction
    data object ClearSettingsClicked : ApiSettingsAction
    data class CopyToClipboard(val text: String) : ApiSettingsAction
    data object ConfirmSuggestDefaultConfig : ApiSettingsAction
    data object DismissSuggestDefaultConfig : ApiSettingsAction
}
