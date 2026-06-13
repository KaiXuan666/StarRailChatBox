package com.kaixuan.starrailchatbox.ui.character

sealed interface CharacterEffect {
    data class ShowMessage(val message: CharacterEffectMessage) : CharacterEffect
    data object CharacterSaved : CharacterEffect
    data object CharacterDeleted : CharacterEffect
    data object RequestDirectoryPicker : CharacterEffect
}

enum class CharacterEffectMessage {
    CHARACTER_NAME_EMPTY,
    CHARACTER_SAVE_FAILED,
    PROMPT_GEN_FAILED,
    CHARACTER_NAME_REQUIRED,
    MODEL_CONFIG_REQUIRED,
    CHARACTER_DELETE_BUILTIN_RESTRICTED,
    CHARACTER_EXPORT_SUCCESS,
    CHARACTER_EXPORT_FAILED,
    CHARACTER_SHARE_SUCCESS,
    CHARACTER_SHARE_FAILED,
    CHARACTER_SHARE_REVIEWING,
    CHARACTER_SHARE_AUTHOR_REQUIRED,
    CHARACTER_SHARE_MEDIA_READ_FAILED,
    CHARACTER_SHARE_PLATFORM_UNSUPPORTED,
    CHARACTER_IMPORT_SUCCESS,
    CHARACTER_IMPORT_FAILED,
    AVATAR_READ_FAILED,
    VOICE_READ_FAILED,
    AVATAR_GEN_FAILED,
    IMAGE_CONFIG_REQUIRED,
}
