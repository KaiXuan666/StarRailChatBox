package com.kaixuan.starrailchatbox.data.character

interface CharacterVoiceSampleStorage {
    fun saveBytes(characterId: String, voiceBytes: ByteArray): String

    fun copyFrom(characterId: String, sourceUri: String): String

    fun delete(voiceUri: String)
}
