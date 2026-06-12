package com.kaixuan.starrailchatbox.ui.main

sealed interface MainEffect {
    data class ShowMessage(val message: MainEffectMessage) : MainEffect
}

enum class MainEffectMessage {
    THEME_CHANGED,
    IMAGE_SAVED,
    IMAGE_SAVE_FAILED,
    ALREADY_LATEST_VERSION,
    CHECKING_FOR_UPDATE,
    UPDATE_CHECK_FAILED,
    COPIED_SUCCESS,
    TEXT_COPIED,
    CHARACTER_SAVED,
    CHARACTER_DELETED,
    PROFILE_SAVED,
    API_SETTINGS_SAVED,
}
