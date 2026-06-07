package com.kaixuan.starrailchatbox.data.api

import de.jensklingenberg.ktorfit.ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import kotlinx.coroutines.CancellationException

interface OpenAiRepository {
    suspend fun getModels(
        apiHost: String,
        apiKey: String,
    ): ApiResult<List<String>>

    suspend fun createChatCompletion(
        config: ModelConfig,
        messages: List<ChatMessage>,
    ): ApiResult<ChatCompletionResult>

    suspend fun testToolCallSupport(
        apiHost: String,
        apiKey: String,
        model: String,
    ): Boolean
}

data class ChatCompletionResult(
    val content: String,
    val finishReason: String?,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

class KtorfitOpenAiRepository(
    private val httpClient: HttpClient,
) : OpenAiRepository {
    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
    ): ApiResult<List<String>> {
        return try {
            val api = ktorfit {
                baseUrl(apiHost.normalizedBaseUrl())
                httpClient(httpClient)
            }.createOpenAiApi()

            val models = api.getModels(
                authorization = "Bearer ${apiKey.trim()}",
            ).data
                .map { it.id.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()

            ApiResult.Success(models)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: ResponseException) {
            ApiResult.HttpError(
                statusCode = error.response.status.value,
                message = error.message,
            )
        } catch (error: Throwable) {
            ApiResult.NetworkError(error.message)
        }
    }

    override suspend fun createChatCompletion(
        config: ModelConfig,
        messages: List<ChatMessage>,
    ): ApiResult<ChatCompletionResult> {
        return try {
            val api = ktorfit {
                baseUrl(config.baseUrl.normalizedBaseUrl())
                httpClient(httpClient)
            }.createOpenAiApi()
            val response = api.createChatCompletion(
                authorization = "Bearer ${config.apiKey.trim()}",
                request = ChatCompletionRequest(
                    model = config.modelName,
                    messages = messages,
                    temperature = config.temperature,
                    topP = config.topP,
//                    maxTokens = config.maxOutputTokens,
                    maxTokens = 1024,
                ),
            )
            val choice = response.choices.firstOrNull()
                ?: return ApiResult.UnexpectedError("Chat completion returned no choices.")
            val usage = response.usage
            ApiResult.Success(
                ChatCompletionResult(
                    content = choice.message.content,
                    finishReason = choice.finishReason,
                    promptTokens = usage?.promptTokens ?: 0,
                    completionTokens = usage?.completionTokens ?: 0,
                    totalTokens = usage?.totalTokens ?: 0,
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: ResponseException) {
            ApiResult.HttpError(
                statusCode = error.response.status.value,
                message = error.message,
            )
        } catch (error: Throwable) {
            ApiResult.NetworkError(error.message)
        }
    }

    override suspend fun testToolCallSupport(
        apiHost: String,
        apiKey: String,
        model: String,
    ): Boolean {
        return try {
            val response = httpClient.post("${apiHost.normalizedBaseUrl()}chat/completions") {
                header("Authorization", "Bearer ${apiKey.trim()}")
                header("Content-Type", "application/json")
                setBody(
                    ToolCallTestRequest(
                        model = model,
                        messages = listOf(ChatMessage(role = "user", content = "1+1=")),
                        tools = listOf(
                            ToolDefinition(
                                type = "function",
                                function = FunctionDefinition(name = "test", description = "test")
                            )
                        ),
                        maxTokens = 5
                    )
                )
            }
            if (response.status.value == 200) {
                true
            } else if (response.status.value == 400) {
                val body = response.bodyAsText()
                body.contains("parameters", ignoreCase = true) ||
                        body.contains("missing", ignoreCase = true) ||
                        body.contains("required", ignoreCase = true)
            } else {
                false
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            false
        }
    }
}

internal fun String.normalizedBaseUrl(): String {
    val normalized = trim().trimEnd('/')
    return "$normalized/"
}
