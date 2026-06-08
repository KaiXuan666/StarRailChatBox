package com.kaixuan.starrailchatbox.data.chat

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.kaixuan.starrailchatbox.data.database.StarRailDatabase
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessageEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessageWithAttachments
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatSummaryEntity
import com.kaixuan.starrailchatbox.data.database.entity.toDomain
import com.kaixuan.starrailchatbox.data.database.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class RoomChatSessionRepository(
    private val database: StarRailDatabase,
) : ChatSessionRepository {
    private val sessionDao = database.chatSessionDao()
    private val messageDao = database.chatMessageDao()
    private val summaryDao = database.chatSummaryDao()
    private val attachmentDao = database.messageAttachmentDao()

    override suspend fun findLatestSession(agentId: String): ChatSession? {
        return sessionDao.findLatestByAgent(agentId)?.toDomain()
    }

    override suspend fun findSession(sessionId: String): ChatSession? {
        return sessionDao.findById(sessionId)?.toDomain()
    }

    override fun observeSessions(agentId: String): Flow<List<ChatSessionSummary>> {
        return sessionDao.observeByAgent(agentId).flatMapLatest { sessions ->
            if (sessions.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    sessions.map { session ->
                        messageDao.observeBySession(session.id).map { messages ->
                            ChatSessionSummary(
                                session = session.toDomain(),
                                lastMessagePreview = messages
                                    .lastOrNull {
                                        it.message.status == ChatMessageStatus.COMPLETED.storageValue &&
                                            it.message.content.isNotBlank()
                                    }
                                    ?.message
                                    ?.content
                                    .orEmpty(),
                                messageCount = messages.count {
                                    it.message.status == ChatMessageStatus.COMPLETED.storageValue
                                },
                            )
                        }
                    },
                ) { summaries -> summaries.toList() }
            }
        }
    }

    override fun observeMessages(sessionId: String): Flow<List<StoredChatMessage>> {
        return messageDao.observeBySession(sessionId).map { messages ->
            messages.map(ChatMessageWithAttachments::toDomain)
        }
    }

    override suspend fun findContext(
        sessionId: String,
        maxHistoryMessageCount: Int?,
    ): ChatContextSnapshot {
        val summary = summaryDao.findActive(sessionId)?.toDomain()
        val limit = maxHistoryMessageCount
            ?.takeIf { it >= 0 }
            ?: Int.MAX_VALUE
        val messages = messageDao.findRecentContext(
            sessionId = sessionId,
            afterSeq = summary?.toSeq ?: 0,
            limit = limit,
        )
            .asReversed()
            .map(ChatMessageWithAttachments::toDomain)
        return ChatContextSnapshot(summary = summary, messages = messages)
    }

    override suspend fun findSummarySource(sessionId: String): ChatContextSnapshot {
        return findContext(sessionId, maxHistoryMessageCount = null)
    }

    override suspend fun createSessionWithMessages(
        session: NewChatSession,
        messages: List<NewChatMessage>,
    ) {
        require(messages.isNotEmpty())
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                sessionDao.upsert(session.toEntity(messages.last()))
                messages.forEachIndexed { index, message ->
                    messageDao.upsert(message.toEntity(seq = index + 1L))
                    attachmentDao.insertAll(message.attachments.map { it.toEntity() })
                }
            }
        }
    }

    override suspend fun appendMessage(message: NewChatMessage) {
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                val seq = messageDao.nextSeq(message.sessionId)
                messageDao.upsert(message.toEntity(seq))
                attachmentDao.insertAll(message.attachments.map { it.toEntity() })
                check(
                    sessionDao.updateLastMessage(
                        sessionId = message.sessionId,
                        messageId = message.id,
                        messageAt = message.createdAt,
                    ) == 1,
                ) { "Chat session ${message.sessionId} does not exist." }
            }
        }
    }

    override suspend fun saveSummary(summary: NewChatSummary): Boolean {
        val entity = summary.toEntity()
        return database.useWriterConnection { connection ->
            connection.immediateTransaction {
                summaryDao.upsert(entity)
                val activated = sessionDao.activateSummary(
                    sessionId = summary.sessionId,
                    summaryId = summary.id,
                    toSeq = summary.toSeq,
                    updatedAt = summary.createdAt,
                ) == 1
                if (!activated) {
                    summaryDao.delete(entity)
                }
                activated
            }
        }
    }

    override suspend fun deleteSession(sessionId: String, deletedAt: Long) {
        check(sessionDao.softDelete(sessionId, deletedAt) == 1) {
            "Chat session $sessionId does not exist."
        }
    }

    override suspend fun updateSessionTitle(sessionId: String, title: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        sessionDao.updateTitle(sessionId, title, now)
    }
}

private fun NewChatSession.toEntity(message: NewChatMessage) = ChatSessionEntity(
    id = id,
    title = title,
    agentId = agentId,
    modelConfigId = modelConfigId,
    systemPromptSnapshot = systemPromptSnapshot,
    customSystemPrompt = null,
    maxContextMessageCount = maxContextMessageCount ?: Int.MAX_VALUE,
    enableSummary = enableSummary,
    summaryThresholdTokens = 0,
    summaryThresholdMessageCount = summaryThresholdMessageCount,
    summaryRetainedMessageCount = summaryRetainedMessageCount,
    activeSummaryId = null,
    compactionSeq = 0,
    lastMessageId = message.id,
    lastMessageAt = message.createdAt,
    pinned = false,
    archived = false,
    createdAt = createdAt,
    updatedAt = message.createdAt,
)

private fun NewChatMessage.toEntity(seq: Long) = ChatMessageEntity(
    id = id,
    sessionId = sessionId,
    seq = seq,
    role = role.apiValue,
    content = content,
    status = status.storageValue,
    errorCode = errorCode,
    errorMessage = errorMessage,
    modelConfigId = modelConfigId,
    modelNameSnapshot = modelNameSnapshot,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
    estimatedTokens = 0,
    isContextExcluded = false,
    createdAt = createdAt,
    updatedAt = createdAt,
    suggestionsJson = if (suggestions.isNotEmpty()) kotlinx.serialization.json.Json.encodeToString(suggestions) else null,
)

private fun ChatSessionEntity.toDomain() = ChatSession(
    id = id,
    title = title,
    agentId = requireNotNull(agentId),
    modelConfigId = modelConfigId,
    systemPromptSnapshot = systemPromptSnapshot,
    customSystemPrompt = customSystemPrompt,
    maxContextMessageCount = maxContextMessageCount.takeUnless { it == Int.MAX_VALUE },
    enableSummary = enableSummary,
    summaryThresholdMessageCount = summaryThresholdMessageCount,
    summaryRetainedMessageCount = summaryRetainedMessageCount,
    lastMessageAt = lastMessageAt,
)

private fun ChatMessageWithAttachments.toDomain() = message.toDomain().copy(
    attachments = attachments.map { it.toDomain() }
)

private fun ChatMessageEntity.toDomain() = StoredChatMessage(
    id = id,
    sessionId = sessionId,
    seq = seq,
    role = when (role) {
        ChatRole.USER.apiValue -> ChatRole.USER
        else -> ChatRole.ASSISTANT
    },
    content = content,
    status = when (status) {
        ChatMessageStatus.FAILED.storageValue -> ChatMessageStatus.FAILED
        else -> ChatMessageStatus.COMPLETED
    },
    errorCode = errorCode,
    errorMessage = errorMessage,
    modelConfigId = modelConfigId,
    modelNameSnapshot = modelNameSnapshot,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
    estimatedTokens = estimatedTokens,
    isContextExcluded = isContextExcluded,
    createdAt = createdAt,
    suggestions = suggestionsJson?.let {
        try {
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(it)
        } catch (e: Exception) {
            emptyList()
        }
    }.orEmpty(),
)

private fun NewChatSummary.toEntity() = ChatSummaryEntity(
    id = id,
    sessionId = sessionId,
    fromSeq = fromSeq,
    toSeq = toSeq,
    content = content,
    sourceMessageCount = sourceMessageCount,
    modelConfigId = modelConfigId,
    modelNameSnapshot = modelNameSnapshot,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
    createdAt = createdAt,
)

private fun ChatSummaryEntity.toDomain() = ChatSummary(
    id = id,
    sessionId = sessionId,
    fromSeq = fromSeq,
    toSeq = toSeq,
    content = content,
    sourceMessageCount = sourceMessageCount,
    modelConfigId = modelConfigId,
    modelNameSnapshot = modelNameSnapshot,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
    createdAt = createdAt,
)
