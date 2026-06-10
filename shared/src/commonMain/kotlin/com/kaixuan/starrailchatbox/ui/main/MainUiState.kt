package com.kaixuan.starrailchatbox.ui.main

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.ui.navigation.Route

/**
 * 应用全局骨架状态 (包含导航栈以及全局主题风格)
 */
@Immutable
data class MainUiState(
    val backStack: List<Route> = listOf(Route.ChatSession),
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
