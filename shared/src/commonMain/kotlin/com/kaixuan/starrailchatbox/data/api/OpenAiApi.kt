package com.kaixuan.starrailchatbox.data.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface OpenAiApi {
    @GET("models")
    suspend fun getModels(
        @Header("Authorization") authorization: String,
    ): OpenAiModelsResponse

    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest,
    ): ChatCompletionResponse
}

@Serializable
data class OpenAiModelsResponse(
    val data: List<OpenAiModel> = emptyList(),
)

@Serializable
data class OpenAiModel(
    val id: String,
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val temperature: Double? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val tools: List<ToolDefinition>? = null,
    @SerialName("tool_choice")
    val toolChoice: ToolChoice? = null,
)

@Serializable
data class ToolChoice(
    val type: String,
    val function: ToolChoiceFunction,
)

@Serializable
data class ToolChoiceFunction(
    val name: String,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null,
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String,
    val function: FunctionCall,
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String,
)

@Serializable
data class ToolCallArguments(
    @SerialName("ai_response")
    val aiResponse: String,
    val suggestions: List<String>,
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
    val usage: ChatUsage? = null,
)

@Serializable
data class ChatChoice(
    val message: ChatMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class ChatUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
)

@Serializable
data class ToolCallTestRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDefinition>,
    @SerialName("max_tokens")
    val maxTokens: Int,
)

@Serializable
data class ToolDefinition(
    val type: String,
    val function: FunctionDefinition,
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: FunctionParameters? = null,
)

@Serializable
data class FunctionParameters(
    val type: String = "object",
    val properties: Map<String, PropertyDefinition>,
    val required: List<String>,
)

@Serializable
data class PropertyDefinition(
    val type: String,
    val description: String,
    val items: PropertyItems? = null,
)

@Serializable
data class PropertyItems(
    val type: String,
)


