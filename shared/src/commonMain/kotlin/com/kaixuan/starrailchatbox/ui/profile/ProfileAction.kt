package com.kaixuan.starrailchatbox.ui.profile

sealed interface ProfileAction {
    data class NicknameChanged(val name: String) : ProfileAction
    data class AvatarChanged(val avatarUri: String?) : ProfileAction
    data object RestoreDefaultAvatar : ProfileAction
    data object SaveClicked : ProfileAction
    data object BackClicked : ProfileAction
    data object ConfirmDiscard : ProfileAction
    data object CancelDiscard : ProfileAction
}
