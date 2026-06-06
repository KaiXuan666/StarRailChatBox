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
}
