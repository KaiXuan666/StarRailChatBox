package com.kaixuan.starrailchatbox.data.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatContextBuilderTest {
    @Test
    fun injectsSummaryAfterSystemPromptWithoutCountingItAsHistory() {
        val messages = buildChatContext(
            systemPrompt = "role prompt",
            summary = ChatSummary(
                id = "summary",
                sessionId = "session",
                fromSeq = 1,
                toSeq = 4,
                content = "Earlier facts",
                sourceMessageCount = 4,
                modelConfigId = null,
                modelNameSnapshot = null,
                promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0,
                createdAt = 1,
            ),
            history = listOf(stored("5", ChatRole.ASSISTANT, "recent")),
            currentUserMessage = "current",
            maxHistoryMessageCount = 0,
        )

        assertEquals(
            listOf(
                "system" to "role prompt",
                "system" to "<chat_history_summary>\nEarlier facts\n</chat_history_summary>",
                "user" to "current",
            ),
            messages.map { it.role to it.content },
        )
    }

    @Test
    fun keepsSystemPromptAndCurrentInputWhileLimitingHistory() {
        val messages = buildChatContext(
            systemPrompt = "system prompt",
            summary = null,
            history = listOf(
                stored("1", ChatRole.USER, "old user"),
                stored("2", ChatRole.ASSISTANT, "old assistant"),
                stored("3", ChatRole.USER, "recent user"),
            ),
            currentUserMessage = "current input",
            maxHistoryMessageCount = 2,
        )

        assertEquals(
            listOf(
                "system" to "system prompt",
                "assistant" to "old assistant",
                "user" to "recent user",
                "user" to "current input",
            ),
            messages.map { it.role to it.content },
        )
    }

    @Test
    fun filtersFailedAndContextExcludedMessages() {
        val messages = buildChatContext(
            systemPrompt = "",
            summary = null,
            history = listOf(
                stored("1", ChatRole.USER, "kept"),
                stored(
                    "2",
                    ChatRole.ASSISTANT,
                    "failed",
                    ChatMessageStatus.FAILED,
                ),
                stored("3", ChatRole.USER, "excluded", excluded = true),
            ),
            currentUserMessage = "current",
            maxHistoryMessageCount = null,
        )

        assertEquals(
            listOf("user" to "kept", "user" to "current"),
            messages.map { it.role to it.content },
        )
    }

    @Test
    fun doesNotInjectToolSpecificFormatting() {
        val messages = buildChatContext(
            systemPrompt = "保持温和人设。",
            summary = null,
            history = emptyList(),
            currentUserMessage = "你好",
            maxHistoryMessageCount = null,
        )

        val systemMsg = messages.first { it.role == "system" }.content.orEmpty()
        val userMsg = messages.first { it.role == "user" }.content.orEmpty()

        assertEquals("保持温和人设。", systemMsg)
        assertEquals("你好", userMsg)
    }
}

private fun stored(
    id: String,
    role: ChatRole,
    content: String,
    status: ChatMessageStatus = ChatMessageStatus.COMPLETED,
    excluded: Boolean = false,
) = StoredChatMessage(
    id = id,
    sessionId = "session",
    seq = id.toLong(),
    role = role,
    content = content,
    status = status,
    isContextExcluded = excluded,
    createdAt = id.toLong(),
)
