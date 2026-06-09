package com.kaixuan.starrailchatbox.ui.profile

import androidx.compose.runtime.Immutable

@Immutable
data class ProfileUiState(
    val customAvatarUri: String? = null,
    val summaryThreshold: Int = 20,
    val saveMultimodalToken: Boolean = false,
    val enableWebSearch: Boolean = false,
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
)
