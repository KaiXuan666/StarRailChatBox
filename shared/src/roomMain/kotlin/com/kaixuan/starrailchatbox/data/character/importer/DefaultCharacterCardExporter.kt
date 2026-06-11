package com.kaixuan.starrailchatbox.data.character.importer

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import com.kaixuan.starrailchatbox.platform.readUriAsBytes
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.write
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DefaultCharacterCardExporter(
    private val fileManager: KmpFileManager = KmpFileManager.Default,
    private val pngEncoder: PngEncoder = getPngEncoder(),
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
) : CharacterCardExporter {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun exportToPng(
        character: Character,
        directory: PlatformFile
    ): ApiResult<Unit> {
        return try {
            val avatarBytes = readUriAsBytes(character.avatarUri)
            if (avatarBytes.isEmpty()) {
                return ApiResult.UnexpectedError("Failed to read character avatar")
            }
            Napier.d { "exportToPng character=$character" }

            val pngBytes = pngEncoder.toPng(avatarBytes)

            val voiceData = character.voiceSampleUri?.let { uri ->
                val bytes = readUriAsBytes(uri)
                if (bytes.isNotEmpty()) {
                    StarRailVoiceData(
                        fileName = uri.substringAfterLast('/'),
                        mimeType = "audio/mpeg",
                        base64Content = Base64.encode(bytes)
                    )
                } else null
            }

            val card = StarRailCharacterCard(
                name = character.name,
                description = character.description,
                systemPrompt = character.prompt,
                openingMessage = character.openingMessage,
                temperature = character.temperature,
                topP = character.topP,
                voice = voiceData
            )

            val cardJson = json.encodeToString(card)
            val base64Json = Base64.encode(cardJson.encodeToByteArray())

            val finalPng = PngMetadataCodec.writeTextChunk(
                pngBytes,
                "starrail_chat_box_character",
                base64Json
            )

            val sanitizedName = character.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val fileName = "$sanitizedName.png"
            val targetFile = directory / fileName

            targetFile.write(finalPng)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to export character card", e)
            ApiResult.UnexpectedError(e.message ?: "Unknown error")
        }
    }
}
