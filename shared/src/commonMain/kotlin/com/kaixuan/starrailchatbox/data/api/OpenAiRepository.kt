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
        characterName: String,
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
    val suggestions: List<String> = emptyList(),
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
        characterName: String,
    ): ApiResult<ChatCompletionResult> {
        return try {
            val api = ktorfit {
                baseUrl(config.baseUrl.normalizedBaseUrl())
                httpClient(httpClient)
            }.createOpenAiApi()

            val request = if (config.supportToolCall) {
                ChatCompletionRequest(
                    model = config.modelName,
                    messages = messages,
                    temperature = config.temperature,
                    topP = config.topP,
                    maxTokens = config.maxOutputTokens,
                    tools = listOf(
                        ToolDefinition(
                            type = "function",
                            function = FunctionDefinition(
                                name = "respond_with_quick_replies",
                                description = "生成${characterName}的角色扮演回复，并强制生成4个可供用户点击的快捷回复按钮选项。",
                                parameters = FunctionParameters(
                                    type = "object",
                                    properties = mapOf(
                                        "ai_response" to PropertyDefinition(
                                            type = "string",
                                            description = "${characterName}的角色扮演文本回复内容。请维持${characterName}人设。"
                                        ),
                                        "suggestions" to PropertyDefinition(
                                            type = "array",
                                            description = "提供给用户的4个快捷回复选项。规范：1. 每个选项字数在 10 字以内；2. 每个选项的开头必须包含一个且仅包含一个符合当前语境和${characterName}人设的表情符号(Emoji)，并用一个空格与文字隔开（例如：'🌸 陪我坐一会儿'、'🍃 只是想吹吹风'）。",
                                            items = PropertyItems(type = "string")
                                        )
                                    ),
                                    required = listOf("ai_response", "suggestions")
                                )
                            )
                        )
                    ),
                    toolChoice = ToolChoice(
                        type = "function",
                        function = ToolChoiceFunction(name = "respond_with_quick_replies")
                    )
                )
            } else {
                ChatCompletionRequest(
                    model = config.modelName,
                    messages = messages,
                    temperature = config.temperature,
                    topP = config.topP,
                    maxTokens = config.maxOutputTokens,
                )
            }

            val response = api.createChatCompletion(
                authorization = "Bearer ${config.apiKey.trim()}",
                request = request,
            )
            val choice = response.choices.firstOrNull()
                ?: return ApiResult.UnexpectedError("Chat completion returned no choices.")
            val usage = response.usage

            var finalContent = choice.message.content.orEmpty()
            var suggestions: List<String> = emptyList()

            if (config.supportToolCall) {
                val toolCall = choice.message.toolCalls?.firstOrNull { it.function.name == "respond_with_quick_replies" }
                if (toolCall != null) {
                    try {
                        val args = kotlinx.serialization.json.Json.decodeFromString<ToolCallArguments>(toolCall.function.arguments)
                        finalContent = args.aiResponse
                        suggestions = args.suggestions
                    } catch (e: Exception) {
                        // 解析异常
                    }
                }
            } else {
                val suggestionsRegex = Regex("<suggestions>([\\s\\S]*?)</suggestions>")
                val match = suggestionsRegex.find(finalContent)
                if (match != null) {
                    val rawSuggestionsSection = match.groupValues[1]
                    suggestions = rawSuggestionsSection.lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { line ->
                            line.removePrefix("-").removePrefix("*").trim()
                        }
                        .filter { it.isNotEmpty() }
                    finalContent = finalContent.replace(suggestionsRegex, "").trim()
                }
            }

            ApiResult.Success(
                ChatCompletionResult(
                    content = finalContent,
                    finishReason = choice.finishReason,
                    promptTokens = usage?.promptTokens ?: 0,
                    completionTokens = usage?.completionTokens ?: 0,
                    totalTokens = usage?.totalTokens ?: 0,
                    suggestions = suggestions,
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
