package com.kaixuan.starrailchatbox.ui.profile

import androidx.compose.runtime.Immutable

@Immutable
data class ProfileUiState(
    val nickname: String = "星空旅人",
    val customAvatarBase64: String? = null,
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false
)
