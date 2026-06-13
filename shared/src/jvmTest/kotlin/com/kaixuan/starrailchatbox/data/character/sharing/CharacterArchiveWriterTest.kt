package com.kaixuan.starrailchatbox.data.character.sharing

import no.synth.kmpzip.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CharacterArchiveWriterTest {
    @Test
    fun writesReadableZipEntries() {
        val archive = createCharacterArchiveWriter().createArchive(
            listOf(
                CharacterArchiveEntry("character.json", "{}".encodeToByteArray()),
                CharacterArchiveEntry("avatar.webp", byteArrayOf(1, 2, 3)),
            ),
        )

        ZipInputStream(archive).use { zip ->
            assertEquals("character.json", zip.nextEntry?.name)
            assertContentEquals("{}".encodeToByteArray(), zip.readBytes())
            zip.closeEntry()
            assertEquals("avatar.webp", zip.nextEntry?.name)
            assertContentEquals(byteArrayOf(1, 2, 3), zip.readBytes())
            zip.closeEntry()
        }
    }
}
