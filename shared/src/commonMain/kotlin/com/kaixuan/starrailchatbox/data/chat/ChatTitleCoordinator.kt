package com.kaixuan.starrailchatbox.data.chat

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChatTitleCoordinator(
    private val chatSessionRepository: ChatSessionRepository,
    private val aiRepository: AiRepository,
) {
    private val titleMutex = Mutex()

    suspend fun renameSessionIfNeeded(
        session: ChatSession,
        config: ModelConfig,
        defaultTitle: String,
    ) {
        // 只有当会话当前的 title 还是默认标题时才进行自动重命名
        if (session.title != defaultTitle) return

        titleMutex.withLock {
            // 双重检查，避免在锁等待期间已经被其他协程更新
            val freshSession = chatSessionRepository.findSession(session.id)
            if (freshSession == null || freshSession.title != defaultTitle) return

            val context = chatSessionRepository.findContext(session.id, maxHistoryMessageCount = null)
            val userMessagesCount = context.messages.count {
                it.role == ChatRole.USER && it.status == ChatMessageStatus.COMPLETED
            }
            val lastMessage = context.messages.lastOrNull()

            // 当且仅当已完成的 USER 消息数量大于等于 2，并且最后一条消息是已完成的 ASSISTANT 消息时才触发
            val isTwoRoundsFinished = userMessagesCount >= 2 &&
                lastMessage?.role == ChatRole.ASSISTANT &&
                lastMessage.status == ChatMessageStatus.COMPLETED

            if (!isTwoRoundsFinished) return

            val requestMessages = buildTitleRequest(context.messages)
            val result = aiRepository.createSessionTitle(config, requestMessages)
            var title = (result as? ApiResult.Success)?.value?.content?.trim().orEmpty()

            if (title.isNotEmpty()) {
                // 后处理清洗：去除常见的包裹引号和修饰符
                title = title.removeSurrounding("\"").removeSurrounding("'")
                    .removeSurrounding("“", "”").removeSurrounding("「", "」")
                    .removeSurrounding("《", "》")
                // 去除可能遗留的 XML 标签
                title = title.replace(Regex("<[^>]*>"), "").trim()
                // 去除常见的“标题：”等修饰前缀
                title = title.removePrefix("标题：").removePrefix("标题:").trim()
                
                // 限制长度，防止破坏UI列表布局，通常 4-8 个字，这里最大限制在 12 个字符
                if (title.length > 12) {
                    title = title.substring(0, 12).trim()
                }

                if (title.isNotEmpty() && title != defaultTitle) {
                    chatSessionRepository.updateSessionTitle(session.id, title)
                }
            }
        }
    }
}

internal fun buildTitleRequest(
    messages: List<StoredChatMessage>,
): List<AiMessage> {
    val transcript = buildString {
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
                你是一个会话标题生成助手。请根据以下用户和 AI 的对话内容，总结并生成一个非常简短、生动的会话标题。
                要求：
                1. 标题必须极其简练，通常在 4 到 8 个字之间。
                2. 必须直接返回总结出的标题，不需要任何前言、后记、解释，不要包含标点符号。
                3. 不要包含 XML 标签。
            """.trimIndent(),
        ),
        AiMessage(role = "user", content = transcript),
    )
}
