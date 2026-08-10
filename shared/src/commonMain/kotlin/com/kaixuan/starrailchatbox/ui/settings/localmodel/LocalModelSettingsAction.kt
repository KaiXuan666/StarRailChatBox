package com.kaixuan.starrailchatbox.ui.settings.localmodel

import com.kaixuan.starrailchatbox.data.localmodel.ChatModelMode

sealed interface LocalModelSettingsAction {
    data class ModeSelected(val mode: ChatModelMode) : LocalModelSettingsAction
    data object DownloadCatalogModel : LocalModelSettingsAction
    data object CancelDownload : LocalModelSettingsAction
    data class ImportSelected(
        val source: String,
        val fileName: String,
        val displayName: String,
    ) : LocalModelSettingsAction
    data object ConfirmImport : LocalModelSettingsAction
    data object DismissImport : LocalModelSettingsAction
    data class SelectModel(val id: String) : LocalModelSettingsAction
    data class RequestDelete(val id: String) : LocalModelSettingsAction
    data object ConfirmDelete : LocalModelSettingsAction
    data object DismissDelete : LocalModelSettingsAction
}

sealed interface LocalModelSettingsEffect {
    data class ShowMessage(val code: String, val detail: String? = null) : LocalModelSettingsEffect
}
