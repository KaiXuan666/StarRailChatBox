package com.kaixuan.starrailchatbox.data.character.sharing

import no.synth.kmpzip.io.ByteArrayOutputStream
import no.synth.kmpzip.zip.ZipEntry
import no.synth.kmpzip.zip.ZipOutputStream

actual fun createCharacterArchiveWriter(): CharacterArchiveWriter = KmpZipCharacterArchiveWriter

private object KmpZipCharacterArchiveWriter : CharacterArchiveWriter {
    override val isSupported: Boolean = true

    override fun createArchive(entries: List<CharacterArchiveEntry>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry.name))
                zip.write(entry.content)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
