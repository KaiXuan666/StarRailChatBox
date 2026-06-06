package com.kaixuan.starrailchatbox.data.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.POST
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface OpenAiApi {
    @GET("models")
    suspend fun getModels(
        @Header("Authorization") authorization: String,
    ): OpenAiModelsResponse

    @POST("chat/completions")
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
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
data class ChatChoice(
    val message: ChatMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)
