package com.kaixuan.starrailchatbox.ui.profile

sealed interface ProfileAction {
    data class NicknameChanged(val name: String) : ProfileAction
    data class AvatarChanged(val avatarBase64: String?) : ProfileAction
    data object RestoreDefaultAvatar : ProfileAction
    data object SaveClicked : ProfileAction
}
