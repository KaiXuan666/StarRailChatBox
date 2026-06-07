package com.kaixuan.starrailchatbox.data.chat

import com.kaixuan.starrailchatbox.data.api.ChatMessage

fun buildChatContext(
    systemPrompt: String,
    history: List<StoredChatMessage>,
    currentUserMessage: String,
    maxHistoryMessageCount: Int?,
    supportToolCall: Boolean,
    characterName: String,
): List<ChatMessage> {
    val completedHistory = history.filter {
        it.status == ChatMessageStatus.COMPLETED && !it.isContextExcluded
    }
    val limitedHistory = maxHistoryMessageCount
        ?.takeIf { it >= 0 }
        ?.let(completedHistory::takeLast)
        ?: completedHistory

    val finalSystemPrompt = if (!supportToolCall && systemPrompt.isNotBlank()) {
        systemPrompt.trim() + "\n\n【重要输出格式规范】\n从现在起，你每次回复必须严格包含两部分：角色的聊天回复，以及提供给用户的 4 个快捷回复选项。你必须严格使用以下格式输出，不要输出任何额外的解释或 JSON 标签：\n\n[你的${characterName}角色回复文本]\n<suggestions>\n[Emoji1] [快捷回复选项1]\n[Emoji2] [快捷回复选项2]\n[Emoji3] [快捷回复选项3]\n[Emoji4] [快捷回复选项4]\n</suggestions>\n\n注意：快捷回复的每一个选项开头必须包含一个且仅包含一个符合语境的表情符号（Emoji），并用空格隔开。选项字数保持简短。"
    } else {
        systemPrompt
    }

    val finalUserMessage = if (!supportToolCall) {
        currentUserMessage.trim() + "\n\n(提示：请严格按照前述【重要输出格式规范】进行输出，确保回复包含 <suggestions> 标签并提供 4 个快捷回复选项。)"
    } else {
        currentUserMessage
    }

    return buildList {
        finalSystemPrompt.trim().takeIf(String::isNotEmpty)?.let {
            add(ChatMessage(role = "system", content = it))
        }
        limitedHistory.forEach {
            add(ChatMessage(role = it.role.apiValue, content = it.content))
        }
        add(ChatMessage(role = ChatRole.USER.apiValue, content = finalUserMessage))
    }
}
