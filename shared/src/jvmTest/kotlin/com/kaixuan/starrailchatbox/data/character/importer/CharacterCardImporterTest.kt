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
              "author": "Astral Express",
              "description": "A cheerful photographer",
              "systemPrompt": "Be cheerful",
              "openingMessage": "Ready to go?",
              "temperature": 0.75,
              "topP": 0.8,
              "voice": {
                "fileName": "march.wav",
                "mimeType": "audio/wav",
                "base64Content": "AQID"
              }
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
            assertEquals("Astral Express", draft.author)
            assertEquals("A cheerful photographer", draft.description)
            assertEquals("Be cheerful", draft.prompt)
            assertEquals("Ready to go?", draft.openingMessage)
            assertEquals(0.75, draft.temperature)
            assertEquals(0.8, draft.topP)
            assertEquals("march.wav", draft.voice?.fileName)
            assertEquals("audio/wav", draft.voice?.mimeType)
            assertEquals("AQID", draft.voice?.base64Content)
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

    @Test
    fun importsSillyTavernV2CreatorAsAuthor() = runTest {
        val result = importJson(
            """
                {
                  "data": {
                    "name": "Firefly",
                    "creator": "Stellaron Hunter",
                    "description": "A traveler seeking life",
                    "first_mes": "Hello.",
                    "system_prompt": "Stay in character."
                  }
                }
            """.trimIndent(),
        )

        val draft = assertIs<ApiResult.Success<ImportedCharacterDraft>>(result).value
        assertEquals("Firefly", draft.name)
        assertEquals("Stellaron Hunter", draft.author)
        assertEquals("A traveler seeking life", draft.description)
        assertEquals("Hello.", draft.openingMessage)
    }

    @Test
    fun importsSillyTavernV3CreatorAsAuthor() = runTest {
        val result = importJson(
            """
                {
                  "spec": "chara_card_v3",
                  "spec_version": "3.0",
                  "data": {
                    "name": "Acheron",
                    "creator": "Galaxy Ranger",
                    "first_mes": "We meet again."
                  }
                }
            """.trimIndent(),
        )

        val draft = assertIs<ApiResult.Success<ImportedCharacterDraft>>(result).value
        assertEquals("Acheron", draft.name)
        assertEquals("Galaxy Ranger", draft.author)
        assertEquals("We meet again.", draft.openingMessage)
        assertEquals("V3 (3.0)", draft.sourceVersion)
    }

    private suspend fun importJson(json: String): ApiResult<ImportedCharacterDraft> {
        val directory = Files.createTempDirectory("character-import-json")
        val fileManager = object : KmpFileManager {
            override val appDataDir: Path = directory.toString().toPath()
            override val cacheDir: Path = directory.toString().toPath()
            override val fileSystem: FileSystem = FileSystem.SYSTEM
            override suspend fun saveImageToGallery(bytes: ByteArray, name: String) = Unit
        }
        val sourcePath = fileManager.cacheDir / "character.json".toPath()
        fileManager.writeBytes(sourcePath, json.encodeToByteArray())

        return try {
            DefaultCharacterCardImporter(fileManager).importFromFile(
                path = sourcePath.toString(),
                name = "character.json",
                extension = "json",
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
