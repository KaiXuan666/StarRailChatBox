package com.kaixuan.starrailchatbox.data.chat

import androidx.paging.testing.asSnapshot
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.database.StarRailDatabase
import com.kaixuan.starrailchatbox.data.database.StarRailDatabaseConstructor
import com.kaixuan.starrailchatbox.data.database.entity.AgentRoleEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessagePageRow
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RoomChatSessionRepositoryTest {
    @Test
    fun seedsNextRoomSessionWithOneThousandPagingTestMessagesOnlyOnce() = runTest {
        val databasePath = Files.createTempFile("starrail-chat-seed", ".db")
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
            val session = newSession("session-seeded", 3_000L)
            val trailingMessage = newMessage(
                id = "real-message",
                sessionId = session.id,
                role = ChatRole.USER,
                content = "real",
                now = 3_000L,
            )

            assertTrue(
                repository.createSessionWithPagingTestMessagesIfNeeded(
                    session = session,
                    trailingMessages = listOf(trailingMessage),
                ),
            )
            val stored = repository.findContext(
                sessionId = session.id,
                maxHistoryMessageCount = null,
            )
            assertEquals(listOf("real"), stored.messages.map { it.content })
            assertEquals(1_001, database.chatMessageDao().visibleMessageCount(session.id))

            assertEquals(
                false,
                repository.createSessionWithPagingTestMessagesIfNeeded(
                    session = newSession("session-not-seeded", 4_000L),
                    trailingMessages = listOf(
                        newMessage(
                            id = "second-real-message",
                            sessionId = "session-not-seeded",
                            role = ChatRole.USER,
                            content = "second",
                            now = 4_000L,
                        ),
                    ),
                ),
            )
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
        }
    }

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
                repository.pagedMessages("session-1")
                    .asSnapshot()
                    .map { it.message }
                    .sortedBy { it.seq }
                    .map { it.content },
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

    @Test
    fun pagesNewestMessagesAndMarksFailedResponseAcrossPageQuery() = runTest {
        val databasePath = Files.createTempFile("starrail-chat-paging", ".db")
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
                session = newSession("session-paging", 1_000L),
                messages = (1..120).map { index ->
                    newMessage(
                        id = "message-$index",
                        sessionId = "session-paging",
                        role = if (index % 2 == 0) ChatRole.ASSISTANT else ChatRole.USER,
                        content = "content-$index",
                        now = index.toLong(),
                    )
                },
            )

            val source = database.chatMessageDao().pagingSourceBySession("session-paging")
            val firstPage = assertIs<PagingSource.LoadResult.Page<Int, ChatMessagePageRow>>(
                source.load(
                    PagingSource.LoadParams.Refresh(
                        key = null,
                        loadSize = 50,
                        placeholdersEnabled = false,
                    ),
                ),
            )
            assertEquals(50, firstPage.data.size)
            assertEquals(120L, firstPage.data.first().message.seq)
            assertEquals(71L, firstPage.data.last().message.seq)

            val secondPage = assertIs<PagingSource.LoadResult.Page<Int, ChatMessagePageRow>>(
                source.load(
                    PagingSource.LoadParams.Append(
                        key = requireNotNull(firstPage.nextKey),
                        loadSize = 50,
                        placeholdersEnabled = false,
                    ),
                ),
            )
            assertEquals(50, secondPage.data.size)
            assertEquals(70L, secondPage.data.first().message.seq)
            assertEquals(21L, secondPage.data.last().message.seq)

            val oldestOffset = repository.oldestMessagePageOffset("session-paging")
            assertEquals(70, oldestOffset)
            val oldestPage = repository.pagedMessages(
                sessionId = "session-paging",
                initialOffset = oldestOffset,
            ).asSnapshot()
            assertEquals(50, oldestPage.size)
            assertEquals(50L, oldestPage.first().message.seq)
            assertEquals(1L, oldestPage.last().message.seq)

            repository.createSessionWithMessages(
                session = newSession("session-failed", 2_000L),
                messages = listOf(
                    newMessage(
                        id = "failed-user",
                        sessionId = "session-failed",
                        role = ChatRole.USER,
                        content = "retry me",
                        now = 2_000L,
                    ),
                    newMessage(
                        id = "failed-assistant",
                        sessionId = "session-failed",
                        role = ChatRole.ASSISTANT,
                        content = "",
                        now = 2_001L,
                        status = ChatMessageStatus.FAILED,
                    ),
                ),
            )
            val failedEntries = repository.pagedMessages("session-failed").asSnapshot()
            assertEquals(1, failedEntries.size)
            assertEquals("failed-user", failedEntries.single().message.id)
            assertTrue(failedEntries.single().hasFailedResponse)
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
    status: ChatMessageStatus = ChatMessageStatus.COMPLETED,
) = NewChatMessage(
    id = id,
    sessionId = sessionId,
    role = role,
    content = content,
    status = status,
    modelConfigId = null,
    modelNameSnapshot = null,
    createdAt = now,
)
