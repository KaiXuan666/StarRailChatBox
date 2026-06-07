package com.kaixuan.starrailchatbox.data.chat

import com.kaixuan.starrailchatbox.data.ai.AiMessage

fun buildChatContext(
    systemPrompt: String,
    summary: ChatSummary?,
    history: List<StoredChatMessage>,
    currentUserMessage: String,
    maxHistoryMessageCount: Int?,
): List<AiMessage> {
    val completedHistory = history.filter {
        it.status == ChatMessageStatus.COMPLETED && !it.isContextExcluded
    }
    val limitedHistory = maxHistoryMessageCount
        ?.takeIf { it >= 0 }
        ?.let(completedHistory::takeLast)
        ?: completedHistory

    return buildList {
        systemPrompt.trim().takeIf(String::isNotEmpty)?.let {
            add(AiMessage(role = "system", content = it))
        }
        summary?.content?.trim()?.takeIf(String::isNotEmpty)?.let {
            add(
                AiMessage(
                    role = "system",
                    content = """<chat_history_summary>$it</chat_history_summary>""".trimIndent(),
                ),
            )
        }
        limitedHistory.forEach {
            add(AiMessage(role = it.role.apiValue, content = it.content))
        }
        add(AiMessage(role = ChatRole.USER.apiValue, content = currentUserMessage.trim()))
    }
}
