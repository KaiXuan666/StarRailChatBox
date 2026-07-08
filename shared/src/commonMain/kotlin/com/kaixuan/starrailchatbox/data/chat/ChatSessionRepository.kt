package com.kaixuan.starrailchatbox.data.chat

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
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
    val enableSummary: Boolean,
    val parentSessionId: String? = null,
    val branchedFromMessageId: String? = null,
    val branchDepth: Int = 0,
    /**
     * 未压缩有效消息达到该数量时触发总结
     */
    val summaryThresholdMessageCount: Int,
    /**
     * 每次压缩后仍保留的最近原始消息数量
     */
    val summaryRetainedMessageCount: Int,
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
    val attachments: List<MessageAttachment> = emptyList(),
)

data class ChatMessagePageEntry(
    val message: StoredChatMessage,
    val hasFailedResponse: Boolean,
)

data class MessageAttachment(
    val id: String,
    val messageId: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val uri: String,
    val createdAt: Long,
    val durationMs: Long? = null,
)

data class ChatSummary(
    val id: String,
    val sessionId: String,
    val fromSeq: Long,
    val toSeq: Long,
    val content: String,
    val sourceMessageCount: Int,
    val modelConfigId: String?,
    val modelNameSnapshot: String?,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val createdAt: Long,
)

data class ChatContextSnapshot(
    val summary: ChatSummary?,
    val messages: List<StoredChatMessage>,
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
    val enableSummary: Boolean = true,
    val parentSessionId: String? = null,
    val branchedFromMessageId: String? = null,
    val branchDepth: Int = 0,
    val summaryThresholdMessageCount: Int = DEFAULT_SUMMARY_THRESHOLD_MESSAGE_COUNT,
    val summaryRetainedMessageCount: Int = DEFAULT_SUMMARY_RETAINED_MESSAGE_COUNT,
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
    val attachments: List<MessageAttachment> = emptyList(),
)

data class NewChatSummary(
    val id: String,
    val sessionId: String,
    val fromSeq: Long,
    val toSeq: Long,
    val content: String,
    val sourceMessageCount: Int,
    val modelConfigId: String?,
    val modelNameSnapshot: String?,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val createdAt: Long,
)

interface ChatSessionRepository {
    suspend fun findLatestSession(agentId: String): ChatSession?

    suspend fun findSession(sessionId: String): ChatSession?

    fun observeSessions(agentId: String): Flow<List<ChatSessionSummary>>

    fun pagedMessages(
        sessionId: String,
        initialOffset: Int = 0,
    ): Flow<PagingData<ChatMessagePageEntry>>

    suspend fun oldestMessagePageOffset(sessionId: String): Int

    fun observeLatestSuggestions(sessionId: String): Flow<List<String>>

    suspend fun findMessage(messageId: String): StoredChatMessage?

    suspend fun findContext(
        sessionId: String,
        maxHistoryMessageCount: Int?,
    ): ChatContextSnapshot

    suspend fun findSummarySource(sessionId: String): ChatContextSnapshot

    suspend fun createSessionWithMessages(
        session: NewChatSession,
        messages: List<NewChatMessage>,
    )

    suspend fun createBranchFromMessage(
        activeSessionId: String,
        messageId: String,
        title: String,
        createdAt: Long,
    ): ChatSession?

    suspend fun appendMessage(message: NewChatMessage)

    suspend fun saveSummary(summary: NewChatSummary): Boolean

    suspend fun deleteSession(sessionId: String, deletedAt: Long)

    suspend fun updateSessionTitle(sessionId: String, title: String)

    suspend fun deleteFailedMessages(sessionId: String)

    suspend fun deleteLatestAssistantMessage(messageId: String, deletedAt: Long): Boolean
}

class InMemoryChatSessionRepository : ChatSessionRepository {
    private data class Segment(
        val ownerSessionId: String,
        val segmentIndex: Int,
        val sourceSessionId: String,
        val fromSeq: Long,
        val toSeq: Long?,
    )

    private val sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    private val messages = MutableStateFlow<List<StoredChatMessage>>(emptyList())
    private val summaries = MutableStateFlow<List<ChatSummary>>(emptyList())
    private val deletedSessionIds = MutableStateFlow<Set<String>>(emptySet())
    private val segments = MutableStateFlow<List<Segment>>(emptyList())
    private val hiddenMessages = MutableStateFlow<Set<Pair<String, String>>>(emptySet())
    private val pagingSources = mutableMapOf<String, MutableSet<PagingSource<Int, ChatMessagePageEntry>>>()

    override suspend fun findLatestSession(agentId: String): ChatSession? {
        return sessions.value
            .filter { it.agentId == agentId && it.id !in deletedSessionIds.value }
            .maxByOrNull(ChatSession::lastMessageAt)
    }

    override suspend fun findSession(sessionId: String): ChatSession? {
        return sessions.value.firstOrNull { it.id == sessionId && it.id !in deletedSessionIds.value }
    }

    override fun observeSessions(agentId: String): Flow<List<ChatSessionSummary>> {
        return combine(
            sessions,
            messages,
            deletedSessionIds,
            segments,
            hiddenMessages,
        ) { storedSessions, storedMessages, deletedIds, storedSegments, hidden ->
            storedSessions
                .filter { it.agentId == agentId && it.id !in deletedIds }
                .sortedByDescending(ChatSession::lastMessageAt)
                .map { session ->
                    val sessionMessages = visibleMessages(
                        sessionId = session.id,
                        storedMessages = storedMessages,
                        storedSegments = storedSegments,
                        hidden = hidden,
                    )
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

    override fun pagedMessages(
        sessionId: String,
        initialOffset: Int,
    ): Flow<PagingData<ChatMessagePageEntry>> {
        return Pager(
            config = ChatMessagePagingConfig,
            initialKey = initialOffset,
            pagingSourceFactory = {
                InMemoryChatMessagePagingSource(
                    snapshot = visibleMessages(sessionId),
                ).also { source ->
                    pagingSources.getOrPut(sessionId) { mutableSetOf() } += source
                    source.registerInvalidatedCallback {
                        pagingSources[sessionId]?.remove(source)
                    }
                }
            },
        ).flow
    }

    override suspend fun oldestMessagePageOffset(sessionId: String): Int {
        val visibleMessageCount = visibleMessages(sessionId).count {
            !(it.role == ChatRole.ASSISTANT && it.status == ChatMessageStatus.FAILED)
        }
        return (visibleMessageCount - ChatMessagePagingConfig.initialLoadSize).coerceAtLeast(0)
    }

    override fun observeLatestSuggestions(sessionId: String): Flow<List<String>> {
        return combine(messages, segments, hiddenMessages) { _, _, _ ->
            visibleMessages(sessionId)
                .lastOrNull()
                ?.takeIf {
                    it.role == ChatRole.ASSISTANT &&
                        it.status == ChatMessageStatus.COMPLETED
                }
                ?.suggestions
                .orEmpty()
        }
    }

    override suspend fun findMessage(messageId: String): StoredChatMessage? {
        return messages.value.firstOrNull { it.id == messageId }
    }

    override suspend fun findContext(
        sessionId: String,
        maxHistoryMessageCount: Int?,
    ): ChatContextSnapshot {
        val session = sessions.value.firstOrNull { it.id == sessionId }
        val summary = if (session?.parentSessionId == null) {
            summaries.value
            .filter { it.sessionId == sessionId }
            .maxByOrNull(ChatSummary::toSeq)
        } else {
            null
        }
        val context = visibleMessages(sessionId).filter {
            it.status == ChatMessageStatus.COMPLETED &&
                !it.isContextExcluded &&
                (summary == null || it.sessionId != sessionId || it.seq > summary.toSeq)
        }
        val limited = maxHistoryMessageCount
            ?.takeIf { it >= 0 }
            ?.let(context::takeLast)
            ?: context
        return ChatContextSnapshot(summary = summary, messages = limited)
    }

    override suspend fun findSummarySource(sessionId: String): ChatContextSnapshot {
        val summary = summaries.value
            .filter { it.sessionId == sessionId }
            .maxByOrNull(ChatSummary::toSeq)
        val context = messages.value.filter {
            it.sessionId == sessionId &&
                it.status == ChatMessageStatus.COMPLETED &&
                !it.isContextExcluded &&
                it.seq > (summary?.toSeq ?: 0) &&
                (sessionId to it.id) !in hiddenMessages.value
        }.sortedBy(StoredChatMessage::seq)
        return ChatContextSnapshot(summary = summary, messages = context)
    }

    override suspend fun createSessionWithMessages(
        session: NewChatSession,
        messages: List<NewChatMessage>,
    ) {
        require(messages.isNotEmpty())
        sessions.update {
            it + session.toStored(lastMessageAt = messages.last().createdAt)
        }
        segments.update {
            it + Segment(
                ownerSessionId = session.id,
                segmentIndex = 0,
                sourceSessionId = session.id,
                fromSeq = 1,
                toSeq = null,
            )
        }
        val storedMessages = messages.mapIndexed { index, message ->
            message.toStored(seq = index + 1L)
        }
        this.messages.update { it + storedMessages }
        invalidatePagingSources(session.id)
    }

    override suspend fun createBranchFromMessage(
        activeSessionId: String,
        messageId: String,
        title: String,
        createdAt: Long,
    ): ChatSession? {
        val parent = sessions.value.firstOrNull { it.id == activeSessionId && it.id !in deletedSessionIds.value }
            ?: return null
        val parentSegments = segments.value
            .filter { it.ownerSessionId == activeSessionId }
            .sortedBy(Segment::segmentIndex)
        val visible = visibleMessages(activeSessionId)
        val target = visible.firstOrNull {
            it.id == messageId &&
                it.role == ChatRole.ASSISTANT &&
                it.status == ChatMessageStatus.COMPLETED
        } ?: return null
        val targetSegmentIndex = parentSegments.indexOfFirst { segment ->
            segment.sourceSessionId == target.sessionId &&
                target.seq >= segment.fromSeq &&
                (segment.toSeq == null || target.seq <= segment.toSeq)
        }
        if (targetSegmentIndex < 0) return null

        val branch = parent.copy(
            id = newChatId("session", createdAt),
            title = title,
            parentSessionId = parent.id,
            branchedFromMessageId = target.id,
            branchDepth = parent.branchDepth + 1,
            lastMessageAt = createdAt,
        )
        val inheritedSegments = parentSegments
            .take(targetSegmentIndex + 1)
            .mapIndexed { index, segment ->
                val toSeq = if (index == targetSegmentIndex) {
                    target.seq
                } else {
                    segment.toSeq
                }
                segment.copy(
                    ownerSessionId = branch.id,
                    segmentIndex = index,
                    toSeq = toSeq,
                )
            }
        sessions.update { it + branch }
        segments.update {
            it + inheritedSegments + Segment(
                ownerSessionId = branch.id,
                segmentIndex = inheritedSegments.size,
                sourceSessionId = branch.id,
                fromSeq = 1,
                toSeq = null,
            )
        }
        invalidatePagingSources(branch.id)
        return branch
    }

    override suspend fun appendMessage(message: NewChatMessage) {
        val seq = messages.value
            .filter { it.sessionId == message.sessionId }
            .maxOfOrNull(StoredChatMessage::seq)
            ?.plus(1)
            ?: 1
        messages.update { it + message.toStored(seq) }
        invalidatePagingSources(message.sessionId)
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

    override suspend fun saveSummary(summary: NewChatSummary): Boolean {
        val active = summaries.value
            .filter { it.sessionId == summary.sessionId }
            .maxByOrNull(ChatSummary::toSeq)
        if (active != null && active.toSeq >= summary.toSeq) {
            return false
        }
        summaries.update { it + summary.toStored() }
        return true
    }

    override suspend fun deleteSession(sessionId: String, deletedAt: Long) {
        deletedSessionIds.update { it + sessionId }
        invalidatePagingSources(sessionId)
    }

    override suspend fun updateSessionTitle(sessionId: String, title: String) {
        sessions.update { stored ->
            stored.map {
                if (it.id == sessionId) {
                    it.copy(title = title)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun deleteFailedMessages(sessionId: String) {
        messages.update { stored ->
            stored.filterNot { it.sessionId == sessionId && it.status == ChatMessageStatus.FAILED }
        }
        invalidatePagingSources(sessionId)
    }

    override suspend fun deleteLatestAssistantMessage(messageId: String, deletedAt: Long): Boolean {
        val target = messages.value.firstOrNull { it.id == messageId }
            ?.takeIf {
                it.role == ChatRole.ASSISTANT &&
                    it.status == ChatMessageStatus.COMPLETED
            }
            ?: return false
        val latest = visibleMessages(target.sessionId)
            .lastOrNull()
            ?: return false
        if (latest.id != target.id) {
            return false
        }

        hiddenMessages.update { it + (target.sessionId to target.id) }
        invalidatePagingSources(target.sessionId)
        visibleMessages(target.sessionId)
            .lastOrNull {
                !(it.role == ChatRole.ASSISTANT && it.status == ChatMessageStatus.FAILED)
            }
            ?.let { latestRemaining ->
                sessions.update { stored ->
                    stored.map {
                        if (it.id == target.sessionId) {
                            it.copy(lastMessageAt = latestRemaining.createdAt)
                        } else {
                            it
                        }
                    }
                }
            }
        return true
    }

    fun getAllMessagesDirectly(): List<StoredChatMessage> = messages.value

    private fun invalidatePagingSources(sessionId: String) {
        pagingSources.remove(sessionId)?.toList()?.forEach(PagingSource<Int, ChatMessagePageEntry>::invalidate)
    }

    private fun visibleMessages(
        sessionId: String,
        storedMessages: List<StoredChatMessage> = messages.value,
        storedSegments: List<Segment> = segments.value,
        hidden: Set<Pair<String, String>> = hiddenMessages.value,
    ): List<StoredChatMessage> {
        return storedSegments
            .filter { it.ownerSessionId == sessionId }
            .sortedBy(Segment::segmentIndex)
            .flatMap { segment ->
                storedMessages
                    .filter {
                        it.sessionId == segment.sourceSessionId &&
                            it.seq >= segment.fromSeq &&
                            (segment.toSeq == null || it.seq <= segment.toSeq) &&
                            (sessionId to it.id) !in hidden
                    }
                    .sortedBy(StoredChatMessage::seq)
            }
    }
}

val ChatMessagePagingConfig = PagingConfig(
    pageSize = 50,
    initialLoadSize = 50,
    prefetchDistance = 10,
    maxSize = 200,
    enablePlaceholders = false,
)

private class InMemoryChatMessagePagingSource(
    snapshot: List<StoredChatMessage>,
) : PagingSource<Int, ChatMessagePageEntry>() {
    private val latestVisibleUserId = snapshot.asSequence()
        .filter {
            !(it.role == ChatRole.ASSISTANT && it.status == ChatMessageStatus.FAILED)
        }
        .lastOrNull()
        ?.takeIf { it.role == ChatRole.USER }
        ?.id
    private val failedResponseKeys = snapshot.asSequence()
        .filter {
            it.role == ChatRole.ASSISTANT &&
                it.status == ChatMessageStatus.FAILED
        }
        .map { it.sessionId to it.seq }
        .toSet()
    private val entries = snapshot
        .asReversed()
        .mapNotNull { message ->
            if (message.role == ChatRole.ASSISTANT && message.status == ChatMessageStatus.FAILED) {
                null
            } else {
                ChatMessagePageEntry(
                    message = message,
                    hasFailedResponse = message.role == ChatRole.USER &&
                        (
                            (message.sessionId to message.seq + 1) in failedResponseKeys ||
                                message.id == latestVisibleUserId
                        ),
                )
            }
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChatMessagePageEntry> {
        val start = params.key ?: 0
        if (start >= entries.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null,
            )
        }
        val end = minOf(start + params.loadSize, entries.size)
        return LoadResult.Page(
            data = entries.subList(start, end),
            prevKey = if (start == 0) null else maxOf(0, start - params.loadSize),
            nextKey = end.takeIf { it < entries.size },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, ChatMessagePageEntry>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(state.config.pageSize)
            ?: page.nextKey?.minus(state.config.pageSize)
    }
}

const val DEFAULT_SUMMARY_THRESHOLD_MESSAGE_COUNT = 30
const val DEFAULT_SUMMARY_RETAINED_MESSAGE_COUNT = 10
const val CHAT_PAGING_TAG = "ChatPaging"

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
    enableSummary = enableSummary,
    parentSessionId = parentSessionId,
    branchedFromMessageId = branchedFromMessageId,
    branchDepth = branchDepth,
    summaryThresholdMessageCount = summaryThresholdMessageCount,
    summaryRetainedMessageCount = summaryRetainedMessageCount,
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
    attachments = attachments,
)

private fun NewChatSummary.toStored() = ChatSummary(
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
