package com.kaixuan.starrailchatbox.data.character

interface CharacterAvatarStorage {
    fun save(characterId: String, avatarBytes: ByteArray): String

    fun read(avatarUri: String): ByteArray
}

internal fun characterAvatarFileName(characterId: String): String {
    return characterId.encodeToByteArray()
        .joinToString(separator = "") { byte -> byte.toUByte().toString(16).padStart(2, '0') }
        .plus(".webp")
}
