package com.kaixuan.starrailchatbox.data.character.importer

import com.kaixuan.starrailchatbox.getPlatform
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SillyTavernV1Card(
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    @SerialName("first_mes") val firstMes: String = "",
    @SerialName("mes_example") val mesExample: String = "",
)

@Serializable
data class SillyTavernV2Card(
    val data: SillyTavernV2Data
)

@Serializable
data class SillyTavernV2Data(
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    @SerialName("first_mes") val firstMes: String = "",
    @SerialName("mes_example") val mesExample: String = "",
    @SerialName("system_prompt") val systemPrompt: String = "",
    @SerialName("post_history_instructions") val postHistoryInstructions: String = "",
)

@Serializable
data class SillyTavernV3Card(
    val spec: String,
    @SerialName("spec_version") val specVersion: String,
    val data: SillyTavernV3Data
)

@Serializable
data class SillyTavernV3Data(
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    @SerialName("first_mes") val firstMes: String = "",
    @SerialName("mes_example") val mesExample: String = "",
    @SerialName("system_prompt") val systemPrompt: String = "",
    @SerialName("post_history_instructions") val postHistoryInstructions: String = "",
)

@Serializable
data class StarRailCharacterCard(
    val spec: String = "starrail_chat_box_character",
    val specVersion: String = getPlatform().versionName,
    val name: String,
    val description: String = "",
    val systemPrompt: String,
    val openingMessage: String,
    val temperature: Double,
    val topP: Double,
    val voice: StarRailVoiceData? = null
)

@Serializable
data class StarRailVoiceData(
    val fileName: String,
    val mimeType: String,
    val base64Content: String
)

data class ImportedCharacterDraft(
    val name: String,
    val description: String = "",
    val prompt: String,
    val openingMessage: String,
    val avatarUri: String? = null,
    val temperature: Double = 0.85,
    val topP: Double = 0.9,
    val voice: StarRailVoiceData? = null,
    val sourceVersion: String,
    val warnings: List<ImportWarning> = emptyList(),
)

enum class ImportWarning {
    UNSUPPORTED_V3_FIELDS,
    UNSUPPORTED_ALTERNATE_GREETINGS,
    UNSUPPORTED_CHARACTER_BOOK,
    UNSUPPORTED_TAGS,
    UNSUPPORTED_CREATOR_NOTES,
    OTHER_EXTENSIONS_IGNORED
}

sealed interface ImportError {
    data object InvalidPng : ImportError
    data object MissingCharacterData : ImportError
    data object InvalidBase64 : ImportError
    data object InvalidJson : ImportError
    data object FileTooLarge : ImportError
    data class Unknown(val message: String) : ImportError
}
