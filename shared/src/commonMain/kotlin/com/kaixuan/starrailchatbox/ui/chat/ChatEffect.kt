package com.kaixuan.starrailchatbox.ui.chat

sealed interface ChatEffect {
    data class ShowMessage(val message: EffectMessage) : ChatEffect
    data object CharacterSaved : ChatEffect
    data object CharacterDeleted : ChatEffect
}

enum class EffectMessage {
    VOICE_NOT_READY,
    PROFILE_NOT_READY,
    ATTACH_NOT_READY,
    EMOJI_NOT_READY,
    MICROPHONE_NOT_READY,
    MODEL_CONFIG_REQUIRED,
    CHAT_REQUEST_FAILED,
    CHAT_EMPTY_RESPONSE,
    CHARACTER_NAME_EMPTY,
    CHARACTER_SAVE_FAILED,
    PROMPT_GEN_FAILED,
    CHARACTER_NAME_REQUIRED,
}
