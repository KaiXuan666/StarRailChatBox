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
        val chunks = PngMetadataCodec.readChunks(bytes)
        Napier.d { "Import PNG: Found chunks: ${chunks.keys}" }
        
        // Priority: starrail_chat_box_character -> ccv3 -> chara
        val projectData = chunks["starrail_chat_box_character"]
        if (projectData != null) {
            Napier.d { "Import PNG: Found project-specific character data" }
            val jsonStr = try {
                @OptIn(ExperimentalEncodingApi::class)
                Base64.decode(projectData).decodeToString()
            } catch (e: Exception) {
                Napier.e("Import PNG: Project card data corrupted (Base64 fail)", e)
                return ApiResult.UnexpectedError("Project-specific card data is corrupted")
            }
            return parseStarRailJsonToDraft(jsonStr, cachePath.toString())
        }

        val v3Data = chunks["ccv3"]
        if (v3Data != null) {
            val jsonStr = try {
                @OptIn(ExperimentalEncodingApi::class)
                Base64.decode(v3Data).decodeToString()
            } catch (e: Exception) {
                return ApiResult.UnexpectedError("Invalid Base64 in PNG chunk (ccv3)")
            }
            return parseJsonToDraft(jsonStr, cachePath.toString())
        }

        val v2Data = chunks["chara"]
        if (v2Data != null) {
            val jsonStr = try {
                @OptIn(ExperimentalEncodingApi::class)
                Base64.decode(v2Data).decodeToString()
            } catch (e: Exception) {
                return ApiResult.UnexpectedError("Invalid Base64 in PNG chunk (chara)")
            }
            return parseJsonToDraft(jsonStr, cachePath.toString())
        }

        return ApiResult.UnexpectedError("No character data found in PNG")
    }

    private fun parseStarRailJsonToDraft(
        jsonStr: String,
        avatarUri: String?
    ): ApiResult<ImportedCharacterDraft> {
        Napier.d { "Import PNG: Parsing project JSON: $jsonStr" }
        return try {
            val card = json.decodeFromString<StarRailCharacterCard>(jsonStr)
            Napier.d { "Import PNG: Project card parsed successfully: ${card.name}" }
            ApiResult.Success(
                ImportedCharacterDraft(
                    name = card.name,
                    description = card.description,
                    prompt = card.systemPrompt,
                    openingMessage = card.openingMessage,
                    avatarUri = avatarUri,
                    temperature = card.temperature,
                    topP = card.topP,
                    voice = card.voice,
                    sourceVersion = "StarRailChatBox ${card.specVersion}"
                )
            )
        } catch (e: Exception) {
            Napier.e("Import PNG: Failed to parse project character JSON", e)
            ApiResult.UnexpectedError("Project character card corrupted: ${e.message}")
        }
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
            description = description,
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
            description = data.description,
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
            description = data.description,
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
