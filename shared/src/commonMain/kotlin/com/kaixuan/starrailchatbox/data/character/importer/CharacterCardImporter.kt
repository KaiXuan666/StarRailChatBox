package com.kaixuan.starrailchatbox.data.character.importer

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

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
    ): ApiResult<ImportedCharacterDraft> = withContext(Dispatchers.Default) {
        try {
            val bytes = fileManager.readSourceBytes(path)
            if (bytes.isEmpty()) {
                return@withContext ApiResult.UnexpectedError("File is empty or not found")
            }

            if (bytes.size > 20 * 1024 * 1024) { // 20MB limit
                return@withContext ApiResult.UnexpectedError("File too large")
            }

            when (extension.lowercase()) {
                "json" -> parseJsonToDraft(bytes.decodeToString(), null)
                "png" -> importFromPng(bytes)
                else -> ApiResult.UnexpectedError("Unsupported file extension")
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Napier.e("Failed to import character card", e)
            ApiResult.UnexpectedError(e.message ?: "Unknown error")
        }
    }

    private fun importFromPng(bytes: ByteArray): ApiResult<ImportedCharacterDraft> {
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
            return parseStarRailJsonToDraft(jsonStr, null).withPngAvatar(bytes)
        }

        val v3Data = chunks["ccv3"]
        if (v3Data != null) {
            val jsonStr = try {
                @OptIn(ExperimentalEncodingApi::class)
                Base64.decode(v3Data).decodeToString()
            } catch (e: Exception) {
                return ApiResult.UnexpectedError("Invalid Base64 in PNG chunk (ccv3)")
            }
            return parseJsonToDraft(jsonStr, null).withPngAvatar(bytes)
        }

        val v2Data = chunks["chara"]
        if (v2Data != null) {
            val jsonStr = try {
                @OptIn(ExperimentalEncodingApi::class)
                Base64.decode(v2Data).decodeToString()
            } catch (e: Exception) {
                return ApiResult.UnexpectedError("Invalid Base64 in PNG chunk (chara)")
            }
            return parseJsonToDraft(jsonStr, null).withPngAvatar(bytes)
        }

        return ApiResult.UnexpectedError("No character data found in PNG")
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun createCleanAvatar(bytes: ByteArray): String {
        val cleanPng = PngMetadataCodec.stripTextMetadata(bytes)
        if (!fileManager.isSupported) {
            return "data:image/png;base64,${Base64.encode(cleanPng)}"
        }
        val avatarPath = fileManager.cacheDir /
            "import_avatar_${Clock.System.now().toEpochMilliseconds()}.png".toPath()
        fileManager.writeBytes(avatarPath, cleanPng)
        return avatarPath.toString()
    }

    private fun ApiResult<ImportedCharacterDraft>.withPngAvatar(
        pngBytes: ByteArray,
    ): ApiResult<ImportedCharacterDraft> = when (this) {
        is ApiResult.Success -> ApiResult.Success(
            value.copy(avatarUri = createCleanAvatar(pngBytes)),
        )
        else -> this
    }

    private fun parseStarRailJsonToDraft(
        jsonStr: String,
        avatarUri: String?
    ): ApiResult<ImportedCharacterDraft> {
        Napier.d { "Import PNG: Parsing project JSON (${jsonStr.length} chars)" }
        return try {
            val card = json.decodeFromString<StarRailCharacterCard>(jsonStr)
            Napier.d { "Import PNG: Project card parsed successfully: ${card.name}" }
            ApiResult.Success(
                ImportedCharacterDraft(
                    name = card.name,
                    author = card.author,
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
