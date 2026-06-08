package com.kaixuan.starrailchatbox.ui.character

sealed interface CharacterEffect {
    data class ShowMessage(val message: CharacterEffectMessage) : CharacterEffect
    data object CharacterSaved : CharacterEffect
    data object CharacterDeleted : CharacterEffect
}

enum class CharacterEffectMessage {
    CHARACTER_NAME_EMPTY,
    CHARACTER_SAVE_FAILED,
    PROMPT_GEN_FAILED,
    CHARACTER_NAME_REQUIRED,
    MODEL_CONFIG_REQUIRED,
}
