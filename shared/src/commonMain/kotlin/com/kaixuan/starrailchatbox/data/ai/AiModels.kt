package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.model.ModelConfig
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 应用层与工具协调器使用的 Provider 无关聊天请求。
 *
 * 各 Provider 负责将该模型转换为自身协议的请求 DTO，协议类型不得泄漏给调用方。
 */
data class AiChatRequest(
    val model: String,
    val messages: List<AiMessage>,
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxTokens: Int? = null,
    val tools: List<AiToolDefinition> = emptyList(),
    val toolChoice: ToolChoice = ToolChoice.None,
    val responseFormat: AiResponseFormat? = null,
)

data class AiMessage(
    val role: String,
    val content: String? = null,
    val toolCalls: List<AiToolCall> = emptyList(),
    val toolCallId: String? = null,
)

data class AiCompletion(
    val message: AiMessage,
    val finishReason: String? = null,
    val usage: AiUsage = AiUsage(),
    val structuredOutput: JsonElement? = null,
)

data class AiUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
) {
    operator fun plus(other: AiUsage) = AiUsage(
        promptTokens = promptTokens + other.promptTokens,
        completionTokens = completionTokens + other.completionTokens,
        totalTokens = totalTokens + other.totalTokens,
    )
}

data class AiToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

data class AiToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
    val strict: Boolean = true,
)

data class AiResponseFormat(
    val name: String,
    val description: String? = null,
    val schema: JsonObject,
    val strict: Boolean = true,
)

/** Provider 无关的工具选择策略，由各 Provider 映射为自身协议格式。 */
sealed interface ToolChoice {
    data object None : ToolChoice
    data object Auto : ToolChoice
    data object Required : ToolChoice
    data class Specific(val name: String) : ToolChoice
}

data class AiProviderConfig(
    val providerId: String,
    val apiHost: String,
    val apiKey: String,
    val model: String,
)

internal fun ModelConfig.toProviderConfig() = AiProviderConfig(
    providerId = provider,
    apiHost = baseUrl,
    apiKey = apiKey,
    model = modelName,
)
