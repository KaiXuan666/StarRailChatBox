package com.kaixuan.starrailchatbox.data.chat

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.map
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.kaixuan.starrailchatbox.data.database.StarRailDatabase
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessageEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessagePageRow
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessageWithAttachments
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatSummaryEntity
import com.kaixuan.starrailchatbox.data.database.entity.toDomain
import com.kaixuan.starrailchatbox.data.database.entity.toEntity
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

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
        return sessionDao.observeSummariesByAgent(agentId).map { rows ->
            rows.map { row ->
                ChatSessionSummary(
                    session = row.session.toDomain(),
                    lastMessagePreview = row.lastMessagePreview,
                    messageCount = row.messageCount,
                )
            }
        }
    }

    override fun pagedMessages(
        sessionId: String,
        initialOffset: Int,
    ): Flow<PagingData<ChatMessagePageEntry>> = flow {
        val agentId = sessionDao.findById(sessionId)?.agentId.orEmpty()
        val generationId = "$sessionId@$initialOffset"
        Napier.d(
            message = "observe start agent=$agentId session=$sessionId generation=$generationId",
            tag = CHAT_PAGING_TAG,
        )
        try {
            emitAll(
                Pager(
                    config = ChatMessagePagingConfig,
                    initialKey = initialOffset,
                    pagingSourceFactory = {
                        LoggingChatMessagePagingSource(
                            delegate = messageDao.pagingSourceBySession(sessionId),
                            agentId = agentId,
                            sessionId = sessionId,
                            generationId = generationId,
                        )
                    },
                ).flow.map { pagingData ->
                    pagingData.map(ChatMessagePageRow::toDomain)
                },
            )
        } finally {
            Napier.d(
                message = "observe stop agent=$agentId session=$sessionId generation=$generationId",
                tag = CHAT_PAGING_TAG,
            )
        }
    }

    override suspend fun oldestMessagePageOffset(sessionId: String): Int {
        return (
            messageDao.visibleMessageCount(sessionId) -
                ChatMessagePagingConfig.initialLoadSize
            ).coerceAtLeast(0)
    }

    override fun observeLatestSuggestions(sessionId: String): Flow<List<String>> {
        return messageDao.observeLatestSuggestionsJson(sessionId).map { json ->
            json?.let {
                runCatching {
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(it)
                }.getOrDefault(emptyList())
            }.orEmpty()
        }
    }

    override suspend fun findMessage(messageId: String): StoredChatMessage? {
        return messageDao.findById(messageId)?.toDomain()
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

    override suspend fun deleteFailedMessages(sessionId: String) {
        messageDao.deleteFailedMessages(sessionId)
    }

}

private class LoggingChatMessagePagingSource(
    private val delegate: PagingSource<Int, ChatMessagePageRow>,
    private val agentId: String,
    private val sessionId: String,
    private val generationId: String,
) : PagingSource<Int, ChatMessagePageRow>() {
    init {
        delegate.registerInvalidatedCallback(::invalidate)
        registerInvalidatedCallback(delegate::invalidate)
    }

    override suspend fun load(
        params: LoadParams<Int>,
    ): LoadResult<Int, ChatMessagePageRow> {
        val loadType = when (params) {
            is LoadParams.Refresh -> "REFRESH"
            is LoadParams.Append -> "APPEND"
            is LoadParams.Prepend -> "PREPEND"
        }
        Napier.d(
            message = "load start agent=$agentId session=$sessionId generation=$generationId " +
                "type=$loadType key=${params.key} requested=${params.loadSize}",
            tag = CHAT_PAGING_TAG,
        )
        val result = delegate.load(params)
        when (result) {
            is LoadResult.Page -> Napier.d(
                message = "load success agent=$agentId session=$sessionId generation=$generationId " +
                    "type=$loadType returned=${result.data.size} " +
                    "prevKey=${result.prevKey} nextKey=${result.nextKey}",
                tag = CHAT_PAGING_TAG,
            )
            is LoadResult.Error -> Napier.e(
                message = "load failed agent=$agentId session=$sessionId generation=$generationId " +
                    "type=$loadType key=${params.key}",
                throwable = result.throwable,
                tag = CHAT_PAGING_TAG,
            )
            is LoadResult.Invalid -> Napier.d(
                message = "load invalid agent=$agentId session=$sessionId generation=$generationId " +
                    "type=$loadType key=${params.key}",
                tag = CHAT_PAGING_TAG,
            )
        }
        return result
    }

    override fun getRefreshKey(state: PagingState<Int, ChatMessagePageRow>): Int? {
        return delegate.getRefreshKey(state)
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

private fun ChatMessagePageRow.toDomain() = ChatMessagePageEntry(
    message = message.toDomain().copy(
        attachments = attachments.map { it.toDomain() },
    ),
    hasFailedResponse = hasFailedResponse,
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
