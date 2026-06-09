package com.kaixuan.starrailchatbox.ui.profile

import androidx.compose.runtime.Immutable

@Immutable
data class ProfileUiState(
    val nickname: String = "星空旅人",
    val customAvatarUri: String? = null,
    val summaryThreshold: Int = 20,
    val saveMultimodalToken: Boolean = false,
    val enableWebSearch: Boolean = false,
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
    val isDiscardDialogOpen: Boolean = false,
    val originalNickname: String = "星空旅人",
    val originalCustomAvatarUri: String? = null,
    val originalSummaryThreshold: Int = 20,
    val originalSaveMultimodalToken: Boolean = false,
    val originalEnableWebSearch: Boolean = false,
) {
    val hasUnsavedChanges: Boolean
        get() = nickname != originalNickname ||
                customAvatarUri != originalCustomAvatarUri ||
                summaryThreshold != originalSummaryThreshold ||
                saveMultimodalToken != originalSaveMultimodalToken ||
                enableWebSearch != originalEnableWebSearch
}

