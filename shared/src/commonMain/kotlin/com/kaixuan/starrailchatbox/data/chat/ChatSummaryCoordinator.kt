package com.kaixuan.starrailchatbox.data.chat

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import io.github.aakira.napier.Napier
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
            Napier.d { "performChatRequest 判断消息总结 summaryThresholdMessageCount=${session.summaryThresholdMessageCount}, summaryRetainedMessageCount=${session.summaryRetainedMessageCount}" }
            val threshold = session.summaryThresholdMessageCount.coerceAtLeast(2)
            val retainedCount = session.summaryRetainedMessageCount
                .coerceIn(1, threshold - 1)
            Napier.d { "performChatRequest 判断消息总结 messages.size=${source.messages.size}, threshold=${threshold}, retainedCount=$retainedCount" }
            if (source.messages.size < threshold) return
            val messagesToSummarize = source.messages.dropLast(retainedCount)
            if (messagesToSummarize.isEmpty()) return
            Napier.d { "performChatRequest 判断消息总结 开始消息总结 messagesToSummarize.size=${messagesToSummarize.size} messagesToSummarize=${messagesToSummarize.joinToString { it.seq.toString() }}" }
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
将对话内容压缩成一份持久的事实摘要，以便日后参考。
保留用户偏好、决策、限制条件、未解决的问题、姓名、
重要事实和正在进行的任务。在相关情况下，保持角色扮演的连贯性。
将之前的摘要与新的对话合并。不要捏造信息。
仅返回摘要，不包含任何前导符或 XML 标签。
            """.trimIndent(),
        ),
        AiMessage(role = "user", content = transcript),
    )
}
