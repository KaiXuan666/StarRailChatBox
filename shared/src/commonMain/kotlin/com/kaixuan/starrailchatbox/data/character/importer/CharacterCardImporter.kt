package com.kaixuan.starrailchatbox.data.character.importer

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import com.kaixuan.starrailchatbox.platform.readUriAsBytes
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.Path
import okio.Path.Companion.toPath
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface CharacterCardImporter {
    suspend fun importFromFile(
        path: String,
        name: String,
        extension: String
    ): ApiResult<ImportedCharacterDraft>
}

class DefaultCharacterCardImporter(
    private val fileManager: KmpFileManager,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
) : CharacterCardImporter {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun importFromFile(
        path: String,
        name: String,
        extension: String
    ): ApiResult<ImportedCharacterDraft> {
        return try {
            val bytes = readUriAsBytes(path)
            if (bytes.isEmpty()) {
                return ApiResult.UnexpectedError("File is empty or not found")
            }

            if (bytes.size > 20 * 1024 * 1024) { // 20MB limit
                return ApiResult.UnexpectedError("File too large")
            }

            // For PNG, we still need a path for the avatar draft in the UI
            // But for parsing, we can use the bytes directly.
            // Requirement: "Copy to cache first as per 0.md requirements"
            val cacheFileName = "import_temp_${kotlin.time.Clock.System.now().toEpochMilliseconds()}.$extension"
            val cachePath = fileManager.cacheDir / cacheFileName
            fileManager.writeBytes(cachePath, bytes)

            when (extension.lowercase()) {
                "json" -> parseJsonToDraft(bytes.decodeToString(), null)
                "png" -> importFromPng(bytes, cachePath)
                else -> ApiResult.UnexpectedError("Unsupported file extension")
            }
        } catch (e: Exception) {
            Napier.e("Failed to import character card", e)
            ApiResult.UnexpectedError(e.message ?: "Unknown error")
        }
    }

    private fun importFromPng(bytes: ByteArray, cachePath: Path): ApiResult<ImportedCharacterDraft> {
        val chunks = readPngChunks(bytes)
        
        // Priority: ccv3 -> chara
        val v3Data = chunks["ccv3"]
        if (v3Data != null) {
            val jsonStr = try {
                @OptIn(ExperimentalEncodingApi::class)
                Base64.decode(v3Data).decodeToString()
            } catch (e: Exception) {
                return ApiResult.UnexpectedError("Invalid Base64 in PNG chunk")
            }
            return parseJsonToDraft(jsonStr, cachePath.toString())
        }

        val v2Data = chunks["chara"]
        if (v2Data != null) {
            val jsonStr = try {
                @OptIn(ExperimentalEncodingApi::class)
                Base64.decode(v2Data).decodeToString()
            } catch (e: Exception) {
                return ApiResult.UnexpectedError("Invalid Base64 in PNG chunk")
            }
            return parseJsonToDraft(jsonStr, cachePath.toString())
        }

        return ApiResult.UnexpectedError("No character data found in PNG")
    }

    private fun readPngChunks(bytes: ByteArray): Map<String, String> {
        val chunks = mutableMapOf<String, String>()
        val buffer = Buffer().write(bytes)
        
        val signature = buffer.readByteString(8)
        if (signature != "89504e470d0a1a0a".decodeHex()) {
            return chunks
        }

        while (!buffer.exhausted()) {
            val length = buffer.readInt()
            val type = buffer.readUtf8(4)
            if (type == "tEXt") {
                val data = buffer.readByteString(length.toLong())
                val dataStr = data.utf8()
                val nullPos = dataStr.indexOf('\u0000')
                if (nullPos != -1) {
                    val key = dataStr.substring(0, nullPos)
                    val value = dataStr.substring(nullPos + 1)
                    chunks[key] = value
                }
            } else if (type == "iTXt") {
                val data = buffer.readByteString(length.toLong())
                val dataStr = data.utf8()
                // iTXt: keyword(Null) + compressionFlag(1) + compressionMethod(1) + languageTag(Null) + translatedKeyword(Null) + text
                val parts = dataStr.split('\u0000')
                if (parts.size >= 4) {
                    val key = parts[0]
                    // We only support uncompressed iTXt for now as ST usually does
                    // The actual text is after the 3rd null separator (Keyword, Language, TranslatedKeyword)
                    // and skipping 2 bytes for compression flags.
                    val headerOffset = key.length + 1 + 2 + parts[1].length + 1 + parts[2].length + 1
                    if (dataStr.length > headerOffset) {
                        chunks[key] = dataStr.substring(headerOffset)
                    }
                }
            } else {
                buffer.skip(length.toLong())
            }
            buffer.skip(4) // CRC
        }
        return chunks
    }

    private fun parseJsonToDraft(
        jsonStr: String,
        avatarUri: String?
    ): ApiResult<ImportedCharacterDraft> {
        // Clean possible BOM or leading/trailing whitespace
        val cleanJson = jsonStr.trim().removePrefix("\uFEFF")
        return try {
            // Try V3
            runCatching { json.decodeFromString<SillyTavernV3Card>(cleanJson) }.getOrNull()?.let { v3 ->
                return ApiResult.Success(v3.toDraft(avatarUri))
            }

            // Try V2
            runCatching { json.decodeFromString<SillyTavernV2Card>(cleanJson) }.getOrNull()?.let { v2 ->
                return ApiResult.Success(v2.toDraft(avatarUri))
            }

            // Try V1
            runCatching { json.decodeFromString<SillyTavernV1Card>(cleanJson) }.getOrNull()?.let { v1 ->
                return ApiResult.Success(v1.toDraft(avatarUri))
            }

            ApiResult.UnexpectedError("Invalid character JSON format")
        } catch (e: Exception) {
            Napier.e("Failed to parse character JSON", e)
            ApiResult.UnexpectedError("JSON parsing failed: ${e.message}")
        }
    }

    private fun SillyTavernV1Card.toDraft(avatarUri: String?): ImportedCharacterDraft {
        return ImportedCharacterDraft(
            name = name,
            prompt = buildPrompt(
                description = description,
                personality = personality,
                scenario = scenario,
                mesExample = mesExample
            ),
            openingMessage = firstMes,
            avatarUri = avatarUri,
            sourceVersion = "V1"
        )
    }

    private fun SillyTavernV2Card.toDraft(avatarUri: String?): ImportedCharacterDraft {
        return ImportedCharacterDraft(
            name = data.name,
            prompt = buildPrompt(
                systemPrompt = data.systemPrompt,
                description = data.description,
                personality = data.personality,
                scenario = data.scenario,
                mesExample = data.mesExample,
                postHistoryInstructions = data.postHistoryInstructions
            ),
            openingMessage = data.firstMes,
            avatarUri = avatarUri,
            sourceVersion = "V2"
        )
    }

    private fun SillyTavernV3Card.toDraft(avatarUri: String?): ImportedCharacterDraft {
        return ImportedCharacterDraft(
            name = data.name,
            prompt = buildPrompt(
                systemPrompt = data.systemPrompt,
                description = data.description,
                personality = data.personality,
                scenario = data.scenario,
                mesExample = data.mesExample,
                postHistoryInstructions = data.postHistoryInstructions
            ),
            openingMessage = data.firstMes,
            avatarUri = avatarUri,
            sourceVersion = "V3 ($specVersion)",
            warnings = listOf(ImportWarning.UNSUPPORTED_V3_FIELDS)
        )
    }

    private fun buildPrompt(
        systemPrompt: String = "",
        description: String = "",
        personality: String = "",
        scenario: String = "",
        mesExample: String = "",
        postHistoryInstructions: String = ""
    ): String {
        val parts = mutableListOf<String>()
        if (systemPrompt.isNotBlank()) parts.add("### System Prompt\n$systemPrompt")
        if (description.isNotBlank()) parts.add("### Description\n$description")
        if (personality.isNotBlank()) parts.add("### Personality\n$personality")
        if (scenario.isNotBlank()) parts.add("### Scenario\n$scenario")
        if (mesExample.isNotBlank()) parts.add("### Examples\n$mesExample")
        if (postHistoryInstructions.isNotBlank()) parts.add("### Post-History Instructions\n$postHistoryInstructions")
        
        return parts.joinToString("\n\n")
    }
}
