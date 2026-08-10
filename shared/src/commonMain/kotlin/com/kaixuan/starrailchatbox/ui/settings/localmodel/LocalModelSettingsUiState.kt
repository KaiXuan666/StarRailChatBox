package com.kaixuan.starrailchatbox.ui.settings.localmodel

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.data.localmodel.ChatModelMode
import com.kaixuan.starrailchatbox.data.localmodel.LocalModel
import com.kaixuan.starrailchatbox.data.localmodel.LocalModelInstallState
import com.kaixuan.starrailchatbox.data.localmodel.LocalRuntimeStatus
import com.kaixuan.starrailchatbox.data.localmodel.StagedLocalModel

@Immutable
data class LocalModelSettingsUiState(
    val mode: ChatModelMode = ChatModelMode.ONLINE,
    val selectedModelId: String? = null,
    val installedModels: List<LocalModel> = emptyList(),
    val installState: LocalModelInstallState = LocalModelInstallState.NOT_INSTALLED,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val bytesPerSecond: Long = 0,
    val stagedImport: StagedLocalModel? = null,
    val pendingDelete: LocalModel? = null,
    val isRuntimeSupported: Boolean = true,
    val runtimeStatus: LocalRuntimeStatus = LocalRuntimeStatus(),
) {
    val progress: Float
        get() = if (totalBytes <= 0L) 0f else downloadedBytes.toFloat() / totalBytes
}
