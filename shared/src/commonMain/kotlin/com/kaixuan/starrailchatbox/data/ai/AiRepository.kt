package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.ai.tool.ToolCallCoordinator
import com.kaixuan.starrailchatbox.data.ai.tool.ToolContext
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig

/**
 * 面向应用层的 AI 门面，统一提供模型发现、能力探测和聊天能力。
 *
 * UI 和 ViewModel 只依赖此接口，不直接依赖具体 Provider 或传输协议模型。
 */
interface AiRepository {
    suspend fun getModels(
        apiHost: String,
        apiKey: String,
        providerId: String = OpenAiCompatibleProvider.Id,
    ): ApiResult<List<String>>

    suspend fun createChatCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
        characterName: String,
    ): ApiResult<ChatCompletionResult>

    suspend fun createPromptCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> = ApiResult.UnexpectedError("createPromptCompletion not implemented")

    suspend fun createConversationSummary(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult>

    suspend fun createSessionTitle(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult>

    suspend fun testToolCallSupport(
        apiHost: String,
        apiKey: String,
        model: String,
        providerId: String = OpenAiCompatibleProvider.Id,
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

/**
 * 组合 Provider 选择与完整工具调用生命周期的默认实现。
 *
 * Provider 和工具均通过注册表扩展，此门面无需增加特定 Provider 或工具的条件分支。
 */
class DefaultAiRepository(
    private val providerRegistry: AiProviderRegistry,
    private val toolCallCoordinator: ToolCallCoordinator,
) : AiRepository {
    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
        providerId: String,
    ): ApiResult<List<String>> {
        val provider = providerRegistry.find(providerId)
            ?: return ApiResult.UnexpectedError("Unknown AI provider: $providerId")
        return provider.getModels(
            AiProviderConfig(
                providerId = providerId,
                apiHost = apiHost,
                apiKey = apiKey,
                model = "",
            ),
        )
    }

    override suspend fun createChatCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
        characterName: String,
    ): ApiResult<ChatCompletionResult> {
        val provider = providerRegistry.find(config.provider)
            ?: return ApiResult.UnexpectedError("Unknown AI provider: ${config.provider}")
        val request = AiChatRequest(
            model = config.modelName,
            messages = messages,
            temperature = config.temperature,
            topP = config.topP,
            maxTokens = config.maxOutputTokens,
        )
        return when (
            val result = toolCallCoordinator.complete(
                provider = provider,
                providerConfig = config.toProviderConfig(),
                request = request,
                context = ToolContext(characterName),
                supportsToolCalls = config.supportToolCall,
            )
        ) {
            is ApiResult.Success -> {
                val completion = result.value
                ApiResult.Success(
                    ChatCompletionResult(
                        content = completion.content,
                        finishReason = completion.finishReason,
                        promptTokens = completion.usage.promptTokens,
                        completionTokens = completion.usage.completionTokens,
                        totalTokens = completion.usage.totalTokens,
                        suggestions = completion.suggestions,
                    ),
                )
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnexpectedError -> result
        }
    }

    override suspend fun createPromptCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> {
        val provider = providerRegistry.find(config.provider)
            ?: return ApiResult.UnexpectedError("Unknown AI provider: ${config.provider}")
        return when (
            val result = provider.complete(
                config = config.toProviderConfig(),
                request = AiChatRequest(
                    model = config.modelName,
                    messages = messages,
                    temperature = config.temperature,
                    topP = config.topP,
                    maxTokens = config.maxOutputTokens,
                    toolChoice = ToolChoice.None,
                ),
            )
        ) {
            is ApiResult.Success -> {
                val completion = result.value
                ApiResult.Success(
                    ChatCompletionResult(
                        content = completion.message.content.orEmpty(),
                        finishReason = completion.finishReason,
                        promptTokens = completion.usage.promptTokens,
                        completionTokens = completion.usage.completionTokens,
                        totalTokens = completion.usage.totalTokens,
                    ),
                )
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnexpectedError -> result
        }
    }

    override suspend fun createConversationSummary(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> {
        val provider = providerRegistry.find(config.provider)
            ?: return ApiResult.UnexpectedError("Unknown AI provider: ${config.provider}")
        return when (
            val result = provider.complete(
                config = config.toProviderConfig(),
                request = AiChatRequest(
                    model = config.modelName,
                    messages = messages,
                    temperature = 0.2,
                    topP = 1.0,
                    maxTokens = minOf(config.maxOutputTokens, SUMMARY_MAX_OUTPUT_TOKENS),
                    toolChoice = ToolChoice.None,
                ),
            )
        ) {
            is ApiResult.Success -> {
                val completion = result.value
                ApiResult.Success(
                    ChatCompletionResult(
                        content = completion.message.content.orEmpty(),
                        finishReason = completion.finishReason,
                        promptTokens = completion.usage.promptTokens,
                        completionTokens = completion.usage.completionTokens,
                        totalTokens = completion.usage.totalTokens,
                    ),
                )
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnexpectedError -> result
        }
    }

    override suspend fun createSessionTitle(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> {
        val provider = providerRegistry.find(config.provider)
            ?: return ApiResult.UnexpectedError("Unknown AI provider: ${config.provider}")
        return when (
            val result = provider.complete(
                config = config.toProviderConfig(),
                request = AiChatRequest(
                    model = config.modelName,
                    messages = messages,
                    temperature = 0.5,
                    topP = 1.0,
                    maxTokens = 1024,
                    toolChoice = ToolChoice.None,
                ),
            )
        ) {
            is ApiResult.Success -> {
                val completion = result.value
                ApiResult.Success(
                    ChatCompletionResult(
                        content = completion.message.content.orEmpty(),
                        finishReason = completion.finishReason,
                        promptTokens = completion.usage.promptTokens,
                        completionTokens = completion.usage.completionTokens,
                        totalTokens = completion.usage.totalTokens,
                    ),
                )
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnexpectedError -> result
        }
    }

    override suspend fun testToolCallSupport(
        apiHost: String,
        apiKey: String,
        model: String,
        providerId: String,
    ): Boolean {
        val provider = providerRegistry.find(providerId) ?: return false
        return provider.supportsToolCalls(
            AiProviderConfig(
                providerId = providerId,
                apiHost = apiHost,
                apiKey = apiKey,
                model = model,
            ),
        )
    }
}

private const val SUMMARY_MAX_OUTPUT_TOKENS = 1_024
