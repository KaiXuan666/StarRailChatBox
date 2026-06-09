package com.kaixuan.starrailchatbox.data.character

interface CharacterVoiceSampleStorage {
    fun saveBytes(characterId: String, voiceBytes: ByteArray): String

    fun delete(voiceUri: String)
}

internal fun characterVoiceSampleFileName(characterId: String): String {
    return characterId.encodeToByteArray()
        .joinToString(separator = "") { byte -> byte.toUByte().toString(16).padStart(2, '0') }
        .plus(".mp3")
}
