package com.kaixuan.starrailchatbox.data.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatContextBuilderTest {
    @Test
    fun keepsSystemPromptAndCurrentInputWhileLimitingHistory() {
        val messages = buildChatContext(
            systemPrompt = "system prompt",
            history = listOf(
                stored("1", ChatRole.USER, "old user"),
                stored("2", ChatRole.ASSISTANT, "old assistant"),
                stored("3", ChatRole.USER, "recent user"),
            ),
            currentUserMessage = "current input",
            maxHistoryMessageCount = 2,
            supportToolCall = true,
            characterName = "流萤",
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
            supportToolCall = true,
            characterName = "流萤",
        )

        assertEquals(
            listOf("user" to "kept", "user" to "current"),
            messages.map { it.role to it.content },
        )
    }

    @Test
    fun injectsFormatSpecificationWhenToolCallNotSupported() {
        val messages = buildChatContext(
            systemPrompt = "保持温和人设。",
            history = emptyList(),
            currentUserMessage = "你好",
            maxHistoryMessageCount = null,
            supportToolCall = false,
            characterName = "瑕蝶",
        )

        val systemMsg = messages.first { it.role == "system" }.content.orEmpty()
        val userMsg = messages.first { it.role == "user" }.content.orEmpty()

        assertTrue(systemMsg.contains("【重要输出格式规范】"))
        assertTrue(systemMsg.contains("瑕蝶"))
        assertTrue(userMsg.contains("你好"))
        assertTrue(userMsg.contains("<suggestions>"))
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

