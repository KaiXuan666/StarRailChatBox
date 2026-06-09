package com.kaixuan.starrailchatbox.ui.profile

sealed interface ProfileEffect {
    data class ShowMessage(val message: ProfileEffectMessage) : ProfileEffect
    data object ProfileSaved : ProfileEffect
    data object NavigateBack : ProfileEffect
    data object RestartApp : ProfileEffect
}

enum class ProfileEffectMessage {
    PROFILE_SAVED,
    EXPORT_SUCCESS,
    IMPORT_SUCCESS,
}
