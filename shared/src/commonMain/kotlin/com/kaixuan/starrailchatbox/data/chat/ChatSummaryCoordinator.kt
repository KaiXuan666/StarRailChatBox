package com.kaixuan.starrailchatbox.data.chat

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

class ChatSummaryCoordinator(
    private val chatSessionRepository: ChatSessionRepository,
    private val aiRepository: AiRepository,
    private val currentTimeMillis: () -> Long = {
        Clock.System.now().toEpochMilliseconds()
    },
    private val idGenerator: (String) -> String = { prefix -> newChatId(prefix) },
) {
    private val summaryMutex = Mutex()

    suspend fun summarizeIfNeeded(
        session: ChatSession,
        config: ModelConfig,
    ) {
        if (!session.enableSummary) return

        summaryMutex.withLock {
            val source = chatSessionRepository.findSummarySource(session.id)
            val threshold = session.summaryThresholdMessageCount.coerceAtLeast(2)
            val retainedCount = session.summaryRetainedMessageCount
                .coerceIn(1, threshold - 1)
            if (source.messages.size < threshold) return

            val messagesToSummarize = source.messages.dropLast(retainedCount)
            if (messagesToSummarize.isEmpty()) return

            val result = aiRepository.createConversationSummary(
                config = config,
                messages = buildSummaryRequest(source.summary, messagesToSummarize),
            )
            val completion = (result as? ApiResult.Success)?.value ?: return
            val content = completion.content.trim()
            if (content.isEmpty()) return

            chatSessionRepository.saveSummary(
                NewChatSummary(
                    id = idGenerator("summary"),
                    sessionId = session.id,
                    fromSeq = source.summary?.fromSeq ?: messagesToSummarize.first().seq,
                    toSeq = messagesToSummarize.last().seq,
                    content = content,
                    sourceMessageCount = (source.summary?.sourceMessageCount ?: 0) +
                        messagesToSummarize.size,
                    modelConfigId = config.id,
                    modelNameSnapshot = config.modelName,
                    promptTokens = completion.promptTokens,
                    completionTokens = completion.completionTokens,
                    totalTokens = completion.totalTokens,
                    createdAt = currentTimeMillis(),
                ),
            )
        }
    }
}

internal fun buildSummaryRequest(
    previousSummary: ChatSummary?,
    messages: List<StoredChatMessage>,
): List<AiMessage> {
    val transcript = buildString {
        previousSummary?.content?.takeIf(String::isNotBlank)?.let {
            appendLine("<previous_summary>")
            appendLine(it)
            appendLine("</previous_summary>")
        }
        appendLine("<conversation>")
        messages.forEach { message ->
            append('<')
            append(message.role.apiValue)
            append('>')
            append(message.content.trim())
            append("</")
            append(message.role.apiValue)
            appendLine('>')
        }
        append("</conversation>")
    }
    return listOf(
        AiMessage(
            role = "system",
            content = """
                Compress the conversation into a durable factual summary for future context.
                Preserve user preferences, decisions, constraints, unresolved questions, names,
                important facts, and ongoing tasks. Preserve role-play continuity when relevant.
                Merge the previous summary with the new conversation. Do not invent information.
                Return only the summary, with no preamble or XML tags.
            """.trimIndent(),
        ),
        AiMessage(role = "user", content = transcript),
    )
}
