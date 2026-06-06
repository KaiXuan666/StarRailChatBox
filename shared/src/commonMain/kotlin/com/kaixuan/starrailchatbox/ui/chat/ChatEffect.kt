package com.kaixuan.starrailchatbox.ui.chat

sealed interface ChatEffect {
    data class ShowMessage(val message: EffectMessage) : ChatEffect
}

enum class EffectMessage {
    VOICE_NOT_READY,
    PROFILE_NOT_READY,
    ATTACH_NOT_READY,
    EMOJI_NOT_READY,
    MICROPHONE_NOT_READY,
    THEME_CHANGED,
    SETTINGS_API_NOT_READY,
    SETTINGS_UPDATE_CHECK,
    SETTINGS_NOTICE_NOT_READY,
    SETTINGS_ABOUT_INFO,
    SETTINGS_PRIVACY_INFO,
}
