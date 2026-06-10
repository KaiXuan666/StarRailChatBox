package com.kaixuan.starrailchatbox.data.character

interface CharacterAvatarStorage {
    fun saveBytes(characterId: String, avatarBytes: ByteArray): String

    fun copyFrom(characterId: String, sourceUri: String): String

    fun delete(avatarUri: String)
}
