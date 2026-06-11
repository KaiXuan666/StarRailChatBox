package com.kaixuan.starrailchatbox.ui.chat

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.ChatCompletionResult
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig

class ChatMessageSender(
    private val aiRepository: AiRepository,
) {
    suspend fun send(
        config: ModelConfig,
        messages: List<AiMessage>,
        characterName: String,
        voiceSampleUri: String?,
    ): ApiResult<ChatCompletionResult> =
        aiRepository.createChatCompletion(
            config = config,
            messages = messages,
            characterName = characterName,
            voiceSampleUri = voiceSampleUri,
        )
}
