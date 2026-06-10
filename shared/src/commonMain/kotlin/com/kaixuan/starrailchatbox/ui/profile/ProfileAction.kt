package com.kaixuan.starrailchatbox.ui.profile

import io.github.vinceglb.filekit.PlatformFile

sealed interface ProfileAction {
    data class AvatarChanged(
        val avatarUri: String?,
        val name: String? = null,
        val extension: String? = null
    ) : ProfileAction
    data class SummaryThresholdChanged(val threshold: Int) : ProfileAction
    data class SaveMultimodalTokenChanged(val enabled: Boolean) : ProfileAction
    data class EnableWebSearchChanged(val enabled: Boolean) : ProfileAction
    data class ExportData(val directoryPath: PlatformFile) : ProfileAction
    data class ImportData(val filePath: PlatformFile) : ProfileAction
    data object RestoreDefaultAvatar : ProfileAction
    data object BackClicked : ProfileAction
}
