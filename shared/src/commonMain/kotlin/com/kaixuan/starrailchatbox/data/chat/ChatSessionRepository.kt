package com.kaixuan.starrailchatbox.data.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.random.Random
import kotlin.time.Clock

data class ChatSession(
    val id: String,
    val title: String,
    val agentId: String,
    val modelConfigId: String?,
    val systemPromptSnapshot: String,
    val customSystemPrompt: String?,
    val maxContextMessageCount: Int?,
    val lastMessageAt: Long,
)

data class ChatSessionSummary(
    val session: ChatSession,
    val lastMessagePreview: String,
    val messageCount: Int,
)

data class StoredChatMessage(
    val id: String,
    val sessionId: String,
    val seq: Long,
    val role: ChatRole,
    val content: String,
    val status: ChatMessageStatus,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val modelConfigId: String? = null,
    val modelNameSnapshot: String? = null,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val estimatedTokens: Int = 0,
    val isContextExcluded: Boolean = false,
    val createdAt: Long,
    val suggestions: List<String> = emptyList(),
)

enum class ChatRole(val apiValue: String) {
    USER("user"),
    ASSISTANT("assistant"),
}

enum class ChatMessageStatus(val storageValue: String) {
    COMPLETED("completed"),
    FAILED("failed"),
}

data class NewChatSession(
    val id: String,
    val title: String,
    val agentId: String,
    val modelConfigId: String?,
    val systemPromptSnapshot: String,
    val maxContextMessageCount: Int?,
    val createdAt: Long,
)

data class NewChatMessage(
    val id: String,
    val sessionId: String,
    val role: ChatRole,
    val content: String,
    val status: ChatMessageStatus,
    val modelConfigId: String?,
    val modelNameSnapshot: String?,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long,
    val suggestions: List<String> = emptyList(),
)

interface ChatSessionRepository {
    suspend fun findLatestSession(agentId: String): ChatSession?

    suspend fun findSession(sessionId: String): ChatSession?

    fun observeSessions(agentId: String): Flow<List<ChatSessionSummary>>

    fun observeMessages(sessionId: String): Flow<List<StoredChatMessage>>

    suspend fun findContextMessages(
        sessionId: String,
        maxHistoryMessageCount: Int?,
    ): List<StoredChatMessage>

    suspend fun createSessionWithMessages(
        session: NewChatSession,
        messages: List<NewChatMessage>,
    )

    suspend fun appendMessage(message: NewChatMessage)

    suspend fun deleteSession(sessionId: String, deletedAt: Long)
}

class InMemoryChatSessionRepository : ChatSessionRepository {
    private val sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    private val messages = MutableStateFlow<List<StoredChatMessage>>(emptyList())

    override suspend fun findLatestSession(agentId: String): ChatSession? {
        return sessions.value
            .filter { it.agentId == agentId }
            .maxByOrNull(ChatSession::lastMessageAt)
    }

    override suspend fun findSession(sessionId: String): ChatSession? {
        return sessions.value.firstOrNull { it.id == sessionId }
    }

    override fun observeSessions(agentId: String): Flow<List<ChatSessionSummary>> {
        return combine(sessions, messages) { storedSessions, storedMessages ->
            storedSessions
                .filter { it.agentId == agentId }
                .sortedByDescending(ChatSession::lastMessageAt)
                .map { session ->
                    val sessionMessages = storedMessages
                        .filter { it.sessionId == session.id }
                        .sortedBy(StoredChatMessage::seq)
                    ChatSessionSummary(
                        session = session,
                        lastMessagePreview = sessionMessages
                            .lastOrNull {
                                it.status == ChatMessageStatus.COMPLETED &&
                                    it.content.isNotBlank()
                            }
                            ?.content
                            .orEmpty(),
                        messageCount = sessionMessages.count {
                            it.status == ChatMessageStatus.COMPLETED
                        },
                    )
                }
        }
    }

    override fun observeMessages(sessionId: String): Flow<List<StoredChatMessage>> {
        return messages.map { stored ->
            stored.filter { it.sessionId == sessionId }.sortedBy(StoredChatMessage::seq)
        }
    }

    override suspend fun findContextMessages(
        sessionId: String,
        maxHistoryMessageCount: Int?,
    ): List<StoredChatMessage> {
        val context = messages.value.filter {
            it.sessionId == sessionId &&
                it.status == ChatMessageStatus.COMPLETED &&
                !it.isContextExcluded
        }.sortedBy(StoredChatMessage::seq)
        return maxHistoryMessageCount
            ?.takeIf { it >= 0 }
            ?.let(context::takeLast)
            ?: context
    }

    override suspend fun createSessionWithMessages(
        session: NewChatSession,
        messages: List<NewChatMessage>,
    ) {
        require(messages.isNotEmpty())
        sessions.update {
            it + session.toStored(lastMessageAt = messages.last().createdAt)
        }
        val storedMessages = messages.mapIndexed { index, message ->
            message.toStored(seq = index + 1L)
        }
        this.messages.update { it + storedMessages }
    }

    override suspend fun appendMessage(message: NewChatMessage) {
        val seq = messages.value
            .filter { it.sessionId == message.sessionId }
            .maxOfOrNull(StoredChatMessage::seq)
            ?.plus(1)
            ?: 1
        messages.update { it + message.toStored(seq) }
        sessions.update { stored ->
            stored.map {
                if (it.id == message.sessionId) {
                    it.copy(lastMessageAt = message.createdAt)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun deleteSession(sessionId: String, deletedAt: Long) {
        sessions.update { stored -> stored.filterNot { it.id == sessionId } }
        messages.update { stored -> stored.filterNot { it.sessionId == sessionId } }
    }
}

fun newChatId(
    prefix: String,
    now: Long = Clock.System.now().toEpochMilliseconds(),
): String = "$prefix-$now-${Random.nextLong().toULong()}"

private fun NewChatSession.toStored(lastMessageAt: Long) = ChatSession(
    id = id,
    title = title,
    agentId = agentId,
    modelConfigId = modelConfigId,
    systemPromptSnapshot = systemPromptSnapshot,
    customSystemPrompt = null,
    maxContextMessageCount = maxContextMessageCount,
    lastMessageAt = lastMessageAt,
)

private fun NewChatMessage.toStored(seq: Long) = StoredChatMessage(
    id = id,
    sessionId = sessionId,
    seq = seq,
    role = role,
    content = content,
    status = status,
    errorCode = errorCode,
    errorMessage = errorMessage,
    modelConfigId = modelConfigId,
    modelNameSnapshot = modelNameSnapshot,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
    createdAt = createdAt,
    suggestions = suggestions,
)
