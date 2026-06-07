package com.kaixuan.starrailchatbox.data.chat

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.ChatCompletionResult
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatSummaryCoordinatorTest {
    @Test
    fun createsRollingSummaryAndRetainsRecentMessages() = runTest {
        val repository = InMemoryChatSessionRepository()
        val aiRepository = SummaryAiRepository()
        repository.createSessionWithMessages(
            session = NewChatSession(
                id = "session",
                title = "title",
                agentId = "agent",
                modelConfigId = "default",
                systemPromptSnapshot = "prompt",
                maxContextMessageCount = null,
                enableSummary = true,
                summaryThresholdMessageCount = 4,
                summaryRetainedMessageCount = 2,
                createdAt = 1,
            ),
            messages = (1..4).map { index ->
                message(id = "$index", content = "message-$index", createdAt = index.toLong())
            },
        )
        val session = requireNotNull(repository.findSession("session"))
        var id = 0
        val coordinator = ChatSummaryCoordinator(
            chatSessionRepository = repository,
            aiRepository = aiRepository,
            currentTimeMillis = { 100 },
            idGenerator = { "summary-${++id}" },
        )

        coordinator.summarizeIfNeeded(session, config())

        val firstContext = repository.findContext("session", null)
        assertEquals("summary-1", firstContext.summary?.id)
        assertEquals(2L, firstContext.summary?.toSeq)
        assertEquals(listOf("message-3", "message-4"), firstContext.messages.map { it.content })

        repository.appendMessage(message("5", "message-5", 5))
        repository.appendMessage(message("6", "message-6", 6))
        coordinator.summarizeIfNeeded(session, config())

        val secondContext = repository.findContext("session", null)
        assertEquals("summary-2", secondContext.summary?.id)
        assertEquals(4L, secondContext.summary?.toSeq)
        assertEquals(listOf("message-5", "message-6"), secondContext.messages.map { it.content })
        assertTrue(
            aiRepository.summaryRequests.last()[1].content.orEmpty()
                .contains("<previous_summary>\nsummary-1\n</previous_summary>"),
        )
    }
}

private class SummaryAiRepository : AiRepository {
    val summaryRequests = mutableListOf<List<AiMessage>>()

    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
        providerId: String,
    ): ApiResult<List<String>> = ApiResult.Success(emptyList())

    override suspend fun createChatCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
        characterName: String,
    ): ApiResult<ChatCompletionResult> = ApiResult.UnexpectedError("Not used.")

    override suspend fun createConversationSummary(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> {
        summaryRequests += messages
        return ApiResult.Success(
            ChatCompletionResult(
                content = "summary-${summaryRequests.size}",
                finishReason = "stop",
                promptTokens = 10,
                completionTokens = 2,
                totalTokens = 12,
            ),
        )
    }

    override suspend fun testToolCallSupport(
        apiHost: String,
        apiKey: String,
        model: String,
        providerId: String,
    ): Boolean = false
}

private fun message(
    id: String,
    content: String,
    createdAt: Long,
) = NewChatMessage(
    id = id,
    sessionId = "session",
    role = if (id.toInt() % 2 == 0) ChatRole.ASSISTANT else ChatRole.USER,
    content = content,
    status = ChatMessageStatus.COMPLETED,
    modelConfigId = "default",
    modelNameSnapshot = "model",
    createdAt = createdAt,
)

private fun config() = ModelConfig(
    id = "default",
    provider = "custom",
    name = "Test",
    baseUrl = "https://example.com/v1",
    apiKey = "key",
    modelName = "model",
    contextWindow = 128_000,
    maxOutputTokens = 4_096,
    supportVision = false,
    supportToolCall = true,
    supportReasoning = false,
    temperature = 0.7,
    topP = 1.0,
    enabled = true,
)
