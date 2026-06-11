package com.kaixuan.starrailchatbox.ui.main

import androidx.compose.runtime.Immutable
/**
 * 应用全局骨架状态。
 */
@Immutable
data class MainUiState(
    val darkThemeOverride: Boolean? = null,
    val showThemeDialog: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val updateInfo: UpdateInfo? = null,
)

@Immutable
data class UpdateInfo(
    val version: String,
    val description: String,
    val downloadUrl: String,
    val isForceUpdate: Boolean = false,
)
