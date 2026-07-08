package com.kaixuan.starrailchatbox.ui.character

sealed interface CharacterEffect {
    data class ShowMessage(
        val message: CharacterEffectMessage,
        val customMessage: String? = null,
        val detail: String? = null,
    ) : CharacterEffect
    data object CharacterSaved : CharacterEffect
    data object CharacterDeleted : CharacterEffect
    data object RequestDirectoryPicker : CharacterEffect
    data object NavigateToProfile : CharacterEffect
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
    CHARACTER_SHARE_BUILTIN_RESTRICTED,
    VOICE_CONFIG_REQUIRED,
    VOICE_GEN_FAILED,
    VOICE_SAMPLE_TOO_LARGE,
    CHARACTER_RESTORE_BUILTIN_NO_DELETED,
    CHARACTER_RESTORE_BUILTIN_SUCCESS,
    CHARACTER_DELETE_LAST_RESTRICTED,
}
