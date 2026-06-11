package com.kaixuan.starrailchatbox.data.character.importer

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import java.nio.file.Files
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class CharacterCardImporterTest {
    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun importsProjectPngWithMetadataFreeAvatar() = runTest {
        val appDataDirectory = Files.createTempDirectory("character-import-app")
        val cacheDirectory = Files.createTempDirectory("character-import-cache")
        val fileManager = object : KmpFileManager {
            override val appDataDir: Path = appDataDirectory.toString().toPath()
            override val cacheDir: Path = cacheDirectory.toString().toPath()
            override val fileSystem: FileSystem = FileSystem.SYSTEM
            override suspend fun saveImageToGallery(bytes: ByteArray, name: String) = Unit
        }
        val sourcePath = fileManager.cacheDir / "import_raw.png".toPath()
        val cardJson = """
            {
              "name": "March 7th",
              "systemPrompt": "Be cheerful",
              "openingMessage": "Ready to go?",
              "temperature": 0.85,
              "topP": 0.9
            }
        """.trimIndent()
        val emptyPng = "89504e470d0a1a0a0000000049454e44ae426082".decodeHex().toByteArray()
        val cardPng = PngMetadataCodec.writeTextChunk(
            pngBytes = emptyPng,
            keyword = "starrail_chat_box_character",
            text = Base64.encode(cardJson.encodeToByteArray()),
        )
        fileManager.writeBytes(sourcePath, cardPng)

        try {
            val result = DefaultCharacterCardImporter(fileManager).importFromFile(
                path = sourcePath.toString(),
                name = "march.png",
                extension = "png",
            )

            val draft = assertIs<ApiResult.Success<ImportedCharacterDraft>>(result).value
            assertEquals("March 7th", draft.name)
            val avatarPath = requireNotNull(draft.avatarUri).toPath()
            assertNotEquals(sourcePath, avatarPath)
            assertTrue(fileManager.exists(avatarPath))
            assertEquals(emptyMap(), PngMetadataCodec.readChunks(fileManager.readBytes(avatarPath)))
            assertEquals(
                setOf(sourcePath, avatarPath),
                fileManager.list(fileManager.cacheDir).toSet(),
            )
        } finally {
            appDataDirectory.toFile().deleteRecursively()
            cacheDirectory.toFile().deleteRecursively()
        }
    }
}
