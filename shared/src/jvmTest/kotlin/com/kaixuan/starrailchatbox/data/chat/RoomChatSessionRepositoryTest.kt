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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

            repository.createSessionWithMessages(
                session = newSession("session-orphan-user", 3_000L),
                messages = listOf(
                    newMessage(
                        id = "orphan-user",
                        sessionId = "session-orphan-user",
                        role = ChatRole.USER,
                        content = "retry after restart",
                        now = 3_000L,
                    ),
                ),
            )
            val orphanEntries = repository.pagedMessages("session-orphan-user").asSnapshot()
            assertEquals(1, orphanEntries.size)
            assertEquals("orphan-user", orphanEntries.single().message.id)
            assertTrue(orphanEntries.single().hasFailedResponse)
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
        }
    }

    @Test
    fun hidesOnlyLatestAssistantMessageForRegeneration() = runTest {
        val databasePath = Files.createTempFile("starrail-chat-regenerate", ".db")
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
                session = newSession("session-regenerate", 1_000L),
                messages = listOf(
                    newMessage("user-1", "session-regenerate", ChatRole.USER, "hello", 1_000L),
                    newMessage("assistant-1", "session-regenerate", ChatRole.ASSISTANT, "hi", 2_000L),
                    newMessage("user-2", "session-regenerate", ChatRole.USER, "again", 3_000L),
                    newMessage("assistant-2", "session-regenerate", ChatRole.ASSISTANT, "again hi", 4_000L),
                ),
            )

            assertFalse(repository.deleteLatestAssistantMessage("assistant-1", 5_000L))
            assertTrue(repository.deleteLatestAssistantMessage("assistant-2", 5_000L))

            assertEquals("again hi", repository.findMessage("assistant-2")?.content)
            assertEquals(
                listOf("hello", "hi", "again"),
                repository.findContext("session-regenerate", null).messages.map { it.content },
            )
            assertEquals(
                listOf("hello", "hi", "again"),
                repository.pagedMessages("session-regenerate")
                    .asSnapshot()
                    .map { it.message }
                    .sortedBy { it.seq }
                    .map { it.content },
            )
            assertEquals("again", repository.observeSessions("agent").first().single().lastMessagePreview)
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
        }
    }

    @Test
    fun createsBranchesWithoutCopyingMessagesAndKeepsThemAfterParentDelete() = runTest {
        val databasePath = Files.createTempFile("starrail-chat-branch", ".db")
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
                session = newSession("root-session", 1_000L),
                messages = listOf(
                    newMessage("root-user-1", "root-session", ChatRole.USER, "hello", 1_000L),
                    newMessage(
                        "root-assistant-1",
                        "root-session",
                        ChatRole.ASSISTANT,
                        "hi",
                        2_000L,
                        attachments = listOf(
                            MessageAttachment(
                                id = "root-attachment-1",
                                messageId = "root-assistant-1",
                                name = "voice.wav",
                                size = 0,
                                mimeType = "audio/wav",
                                uri = "file://voice.wav",
                                createdAt = 2_000L,
                            ),
                        ),
                    ),
                    newMessage("root-user-2", "root-session", ChatRole.USER, "after fork", 3_000L),
                    newMessage("root-assistant-2", "root-session", ChatRole.ASSISTANT, "after reply", 4_000L),
                ),
            )

            val branch = requireNotNull(
                repository.createBranchFromMessage(
                    activeSessionId = "root-session",
                    messageId = "root-assistant-1",
                    title = "分支对话",
                    createdAt = 5_000L,
                ),
            )
            assertEquals("root-session", branch.parentSessionId)
            assertEquals("root-assistant-1", branch.branchedFromMessageId)
            assertEquals(1, branch.branchDepth)
            assertEquals(2, database.chatSessionSegmentDao().findByOwner(branch.id).size)
            assertEquals(
                listOf("hello", "hi"),
                repository.findContext(branch.id, null).messages.map { it.content },
            )
            assertEquals(
                listOf("root-user-1", "root-assistant-1"),
                repository.pagedMessages(branch.id)
                    .asSnapshot()
                    .map { it.message }
                    .sortedBy { it.createdAt }
                    .map { it.id },
            )
            assertEquals(
                "root-attachment-1",
                repository.findContext(branch.id, null).messages
                    .single { it.id == "root-assistant-1" }
                    .attachments
                    .single()
                    .id,
            )

            repository.appendMessage(
                newMessage("branch-user-1", branch.id, ChatRole.USER, "branch question", 6_000L),
            )
            repository.appendMessage(
                newMessage("branch-assistant-1", branch.id, ChatRole.ASSISTANT, "branch reply", 7_000L),
            )
            assertEquals(
                listOf("hello", "hi", "branch question", "branch reply"),
                repository.findContext(branch.id, null).messages.map { it.content },
            )

            val nested = requireNotNull(
                repository.createBranchFromMessage(
                    activeSessionId = branch.id,
                    messageId = "branch-assistant-1",
                    title = "二级分支",
                    createdAt = 8_000L,
                ),
            )
            assertEquals(2, nested.branchDepth)
            assertEquals(
                listOf("hello", "hi", "branch question", "branch reply"),
                repository.findContext(nested.id, null).messages.map { it.content },
            )

            repository.deleteSession("root-session", 9_000L)
            assertEquals(null, repository.findSession("root-session"))
            assertEquals(
                listOf("hello", "hi", "branch question", "branch reply"),
                repository.findContext(branch.id, null).messages.map { it.content },
            )
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
        }
    }

    @Test
    fun parentRegenerationDoesNotHideBranchedSnapshotMessage() = runTest {
        val databasePath = Files.createTempFile("starrail-chat-branch-regenerate", ".db")
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
                session = newSession("root-session", 1_000L),
                messages = listOf(
                    newMessage("root-user-1", "root-session", ChatRole.USER, "hello", 1_000L),
                    newMessage("root-assistant-1", "root-session", ChatRole.ASSISTANT, "hi", 2_000L),
                ),
            )

            val branch = requireNotNull(
                repository.createBranchFromMessage(
                    activeSessionId = "root-session",
                    messageId = "root-assistant-1",
                    title = "分支对话",
                    createdAt = 3_000L,
                ),
            )

            assertTrue(repository.deleteLatestAssistantMessage("root-assistant-1", 4_000L))
            assertEquals("hi", repository.findMessage("root-assistant-1")?.content)
            assertEquals(
                listOf("hello"),
                repository.findContext("root-session", null).messages.map { it.content },
            )
            assertEquals(
                listOf("hello", "hi"),
                repository.findContext(branch.id, null).messages.map { it.content },
            )
            assertEquals(
                listOf("root-user-1", "root-assistant-1"),
                repository.pagedMessages(branch.id)
                    .asSnapshot()
                    .map { it.message }
                    .sortedBy { it.seq }
                    .map { it.id },
            )
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
    attachments: List<MessageAttachment> = emptyList(),
) = NewChatMessage(
    id = id,
    sessionId = sessionId,
    role = role,
    content = content,
    status = status,
    modelConfigId = null,
    modelNameSnapshot = null,
    createdAt = now,
    attachments = attachments,
)
