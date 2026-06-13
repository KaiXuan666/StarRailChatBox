package com.kaixuan.starrailchatbox.data.character.sharing

actual fun createCharacterArchiveWriter(): CharacterArchiveWriter = UnsupportedCharacterArchiveWriter

private object UnsupportedCharacterArchiveWriter : CharacterArchiveWriter {
    override val isSupported: Boolean = false

    override fun createArchive(entries: List<CharacterArchiveEntry>): ByteArray {
        throw UnsupportedOperationException("Public character sharing is not supported on JavaScript.")
    }
}
