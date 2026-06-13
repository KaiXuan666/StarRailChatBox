package com.kaixuan.starrailchatbox.data.character.sharing

data class CharacterArchiveEntry(
    val name: String,
    val content: ByteArray,
)

interface CharacterArchiveWriter {
    val isSupported: Boolean

    fun createArchive(entries: List<CharacterArchiveEntry>): ByteArray
}

expect fun createCharacterArchiveWriter(): CharacterArchiveWriter
