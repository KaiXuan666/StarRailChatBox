package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.ai.tool.QuickRepliesTool
import com.kaixuan.starrailchatbox.data.ai.tool.RiskBasedToolApprovalGateway
import com.kaixuan.starrailchatbox.data.ai.tool.ToolCallCoordinator
import com.kaixuan.starrailchatbox.data.ai.tool.ToolRegistry
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultAiRepositoryTest {
    @Test
    fun registeredQuickRepliesToolProducesApplicationResult() = runTest {
        val provider = RecordingProvider(
            AiCompletion(
                message = AiMessage(
                    role = "assistant",
                    toolCalls = listOf(
                        AiToolCall(
                            id = "call-1",
                            name = QuickRepliesTool.Name,
                            arguments = """
                                {
                                  "ai_response": "你好，我在。",
                                  "suggestions": ["🌸 坐一会儿", "🍃 去散步", "✨ 讲故事", "🌙 看星星"]
                                }
                            """.trimIndent(),
                        ),
                    ),
                ),
                usage = AiUsage(totalTokens = 12),
            ),
        )
        val repository = repository(provider)

        val result = repository.createChatCompletion(
            config = modelConfig(supportToolCall = true),
            messages = listOf(AiMessage("user", "你好")),
            characterName = "流萤",
        )

        val value = assertIs<ApiResult.Success<ChatCompletionResult>>(result).value
        assertEquals("你好，我在。", value.content)
        assertEquals(4, value.suggestions.size)
        assertEquals(12, value.totalTokens)
        assertTrue(provider.requests.single().tools.any { it.name == QuickRepliesTool.Name })
    }

    @Test
    fun fallbackIsAppliedOutsideChatContextBuilder() = runTest {
        val provider = RecordingProvider(
            AiCompletion(
                message = AiMessage(
                    role = "assistant",
                    content = """
                        你好，我在。
                        <suggestions>
                        🌸 坐一会儿
                        🍃 去散步
                        ✨ 讲故事
                        🌙 看星星
                        </suggestions>
                    """.trimIndent(),
                ),
            ),
        )
        val repository = repository(provider)

        val result = repository.createChatCompletion(
            config = modelConfig(supportToolCall = false),
            messages = listOf(
                AiMessage("system", "保持人设"),
                AiMessage("user", "你好"),
            ),
            characterName = "流萤",
        )

        val value = assertIs<ApiResult.Success<ChatCompletionResult>>(result).value
        assertEquals("你好，我在。", value.content)
        assertEquals(4, value.suggestions.size)
        assertTrue(
            provider.requests.single().messages.first().content.orEmpty()
                .contains("重要输出格式规范"),
        )
    }

    @Test
    fun unknownProviderReturnsDomainError() = runTest {
        val repository = repository(RecordingProvider(AiCompletion(AiMessage("assistant"))))

        val result = repository.createChatCompletion(
            config = modelConfig(supportToolCall = true).copy(provider = "missing"),
            messages = listOf(AiMessage("user", "你好")),
            characterName = "流萤",
        )

        assertIs<ApiResult.UnexpectedError>(result)
    }
}

private fun repository(provider: AiProvider): DefaultAiRepository {
    val tools = ToolRegistry(listOf(QuickRepliesTool()))
    return DefaultAiRepository(
        providerRegistry = AiProviderRegistry(
            providers = listOf(provider),
            aliases = mapOf("custom" to provider.id),
        ),
        toolCallCoordinator = ToolCallCoordinator(
            toolRegistry = tools,
            approvalGateway = RiskBasedToolApprovalGateway,
        ),
    )
}

private class RecordingProvider(
    private val completion: AiCompletion,
) : AiProvider {
    override val id: String = "recording"
    val requests = mutableListOf<AiChatRequest>()

    override suspend fun getModels(config: AiProviderConfig): ApiResult<List<String>> {
        return ApiResult.Success(emptyList())
    }

    override suspend fun complete(
        config: AiProviderConfig,
        request: AiChatRequest,
    ): ApiResult<AiCompletion> {
        requests += request
        return ApiResult.Success(completion)
    }

    override suspend fun supportsToolCalls(config: AiProviderConfig): Boolean = true
}

private fun modelConfig(supportToolCall: Boolean) = ModelConfig(
    id = "default",
    provider = "custom",
    name = "Test",
    baseUrl = "https://example.com/v1",
    apiKey = "key",
    modelName = "model",
    contextWindow = 128_000,
    maxOutputTokens = 2_048,
    supportVision = false,
    supportToolCall = supportToolCall,
    supportReasoning = false,
    temperature = 0.7,
    topP = 1.0,
    enabled = true,
)
