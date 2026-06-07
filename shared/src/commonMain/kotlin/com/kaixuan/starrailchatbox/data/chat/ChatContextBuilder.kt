package com.kaixuan.starrailchatbox.data.chat

import com.kaixuan.starrailchatbox.data.api.ChatMessage

fun buildChatContext(
    systemPrompt: String,
    history: List<StoredChatMessage>,
    currentUserMessage: String,
    maxHistoryMessageCount: Int?,
): List<ChatMessage> {
    val completedHistory = history.filter {
        it.status == ChatMessageStatus.COMPLETED && !it.isContextExcluded
    }
    val limitedHistory = maxHistoryMessageCount
        ?.takeIf { it >= 0 }
        ?.let(completedHistory::takeLast)
        ?: completedHistory

    return buildList {
        systemPrompt.trim().takeIf(String::isNotEmpty)?.let {
            add(ChatMessage(role = "system", content = it))
        }
        limitedHistory.forEach {
            add(ChatMessage(role = it.role.apiValue, content = it.content))
        }
        add(ChatMessage(role = ChatRole.USER.apiValue, content = currentUserMessage))
    }
}
