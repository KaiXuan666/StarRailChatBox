package com.kaixuan.starrailchatbox.data.chat

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.kaixuan.starrailchatbox.data.database.StarRailDatabase
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessageEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChatSessionRepository(
    private val database: StarRailDatabase,
) : ChatSessionRepository {
    private val sessionDao = database.chatSessionDao()
    private val messageDao = database.chatMessageDao()

    override suspend fun findLatestSession(agentId: String): ChatSession? {
        return sessionDao.findLatestByAgent(agentId)?.toDomain()
    }

    override fun observeMessages(sessionId: String): Flow<List<StoredChatMessage>> {
        return messageDao.observeBySession(sessionId).map { messages ->
            messages.map(ChatMessageEntity::toDomain)
        }
    }

    override suspend fun findContextMessages(
        sessionId: String,
        maxHistoryMessageCount: Int?,
    ): List<StoredChatMessage> {
        val limit = maxHistoryMessageCount
            ?.takeIf { it >= 0 }
            ?: Int.MAX_VALUE
        return messageDao.findRecentContext(sessionId, limit)
            .asReversed()
            .map(ChatMessageEntity::toDomain)
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
                }
            }
        }
    }

    override suspend fun appendMessage(message: NewChatMessage) {
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                val seq = messageDao.nextSeq(message.sessionId)
                messageDao.upsert(message.toEntity(seq))
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
}

private fun NewChatSession.toEntity(message: NewChatMessage) = ChatSessionEntity(
    id = id,
    title = title,
    agentId = agentId,
    modelConfigId = modelConfigId,
    systemPromptSnapshot = systemPromptSnapshot,
    customSystemPrompt = null,
    maxContextMessageCount = maxContextMessageCount ?: Int.MAX_VALUE,
    enableSummary = false,
    summaryThresholdTokens = 0,
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
    lastMessageAt = lastMessageAt,
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
