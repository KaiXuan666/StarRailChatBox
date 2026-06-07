package com.kaixuan.starrailchatbox.data.chat

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.database.StarRailDatabase
import com.kaixuan.starrailchatbox.data.database.StarRailDatabaseConstructor
import com.kaixuan.starrailchatbox.data.database.entity.AgentRoleEntity
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomChatSessionRepositoryTest {
    @Test
    fun createsSessionAndMaintainsOrderedMessagesAndLatestSession() = runTest {
        val databasePath = Files.createTempFile("starrail-chat-session", ".db")
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
        val repository = RoomChatSessionRepository(database)

        try {
            database.agentRoleDao().upsert(testRole())
            repository.createSessionWithMessages(
                session = newSession("session-1", 1_000L),
                messages = listOf(
                    newMessage(
                        "opening-1",
                        "session-1",
                        ChatRole.ASSISTANT,
                        "welcome",
                        1_000L,
                    ),
                    newMessage("user-1", "session-1", ChatRole.USER, "hello", 1_000L),
                ),
            )
            repository.appendMessage(
                newMessage(
                    "assistant-1",
                    "session-1",
                    ChatRole.ASSISTANT,
                    "hi",
                    2_000L,
                ),
            )

            assertEquals(
                listOf("welcome", "hello", "hi"),
                repository.observeMessages("session-1").first().map { it.content },
            )
            assertEquals("session-1", repository.findLatestSession("agent")?.id)
            assertEquals(
                listOf("welcome", "hello", "hi"),
                repository.findContext("session-1", null).messages.map { it.content },
            )
            val summary = repository.observeSessions("agent").first().single()
            assertEquals("session-1", summary.session.id)
            assertEquals("hi", summary.lastMessagePreview)
            assertEquals(3, summary.messageCount)

            assertEquals(
                true,
                repository.saveSummary(
                    NewChatSummary(
                        id = "summary-1",
                        sessionId = "session-1",
                        fromSeq = 1,
                        toSeq = 2,
                        content = "welcome and hello",
                        sourceMessageCount = 2,
                        modelConfigId = null,
                        modelNameSnapshot = "model",
                        promptTokens = 10,
                        completionTokens = 3,
                        totalTokens = 13,
                        createdAt = 2_500L,
                    ),
                ),
            )
            val compactedContext = repository.findContext("session-1", null)
            assertEquals("welcome and hello", compactedContext.summary?.content)
            assertEquals(listOf("hi"), compactedContext.messages.map { it.content })

            repository.deleteSession("session-1", 3_000L)
            assertEquals(emptyList(), repository.observeSessions("agent").first())
            assertEquals(null, repository.findLatestSession("agent"))
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
        }
    }
}

private fun testRole() = AgentRoleEntity(
    id = "agent",
    name = "Agent",
    avatarUri = "",
    description = "",
    systemPrompt = "prompt",
    openingMessage = "",
    sortOrder = 0,
    isBuiltin = false,
    createdAt = 1_000L,
    updatedAt = 1_000L,
)

private fun newSession(id: String, now: Long) = NewChatSession(
    id = id,
    title = "新对话",
    agentId = "agent",
    modelConfigId = null,
    systemPromptSnapshot = "prompt",
    maxContextMessageCount = null,
    createdAt = now,
)

private fun newMessage(
    id: String,
    sessionId: String,
    role: ChatRole,
    content: String,
    now: Long,
) = NewChatMessage(
    id = id,
    sessionId = sessionId,
    role = role,
    content = content,
    status = ChatMessageStatus.COMPLETED,
    modelConfigId = null,
    modelNameSnapshot = null,
    createdAt = now,
)
