package com.kaixuan.starrailchatbox.ui.profile

sealed interface ProfileEffect {
    data class ShowMessage(val message: ProfileEffectMessage) : ProfileEffect
    data object ProfileSaved : ProfileEffect
}

enum class ProfileEffectMessage {
    PROFILE_SAVED,
    NICKNAME_EMPTY
}
