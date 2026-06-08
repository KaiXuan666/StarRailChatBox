package com.kaixuan.starrailchatbox.data.character

interface CharacterAvatarStorage {
    fun saveBytes(characterId: String, avatarBytes: ByteArray): String

    fun copyFrom(characterId: String, sourceUri: String): String

    fun delete(avatarUri: String)
}

internal fun characterAvatarFileName(characterId: String): String {
    return characterId.encodeToByteArray()
        .joinToString(separator = "") { byte -> byte.toUByte().toString(16).padStart(2, '0') }
        .plus(".webp")
}
