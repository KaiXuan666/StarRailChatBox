package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.localmodel.LiteRtLmProviderId
import com.kaixuan.starrailchatbox.data.localmodel.LocalInferenceMessage
import com.kaixuan.starrailchatbox.data.localmodel.LocalInferenceRequest
import com.kaixuan.starrailchatbox.data.localmodel.LocalLanguageModelRuntime
import com.kaixuan.starrailchatbox.data.localmodel.LocalModelRepository
import com.kaixuan.starrailchatbox.data.localmodel.LocalRuntimeResult
import kotlinx.coroutines.flow.first

class LiteRtLmProvider(
    private val models: LocalModelRepository,
    private val runtime: LocalLanguageModelRuntime,
) : AiProvider {
    override val id: String = LiteRtLmProviderId

    override suspend fun getModels(config: AiProviderConfig): ApiResult<List<String>> =
        ApiResult.Success(models.observeModels().first().map { it.id })

    override suspend fun complete(
        config: AiProviderConfig,
        request: AiChatRequest,
    ): ApiResult<AiCompletion> {
        val model = models.getById(config.model)
            ?: return ApiResult.UnexpectedError("Local model is not installed.", "local_model_not_installed")
        val prepared = prepareLocalRequest(request)
            ?: return ApiResult.UnexpectedError(
                "Local models only support text messages in this version.",
                "local_multimodal_unsupported",
            )
        return when (val result = runtime.complete(model, prepared)) {
            is LocalRuntimeResult.Success -> ApiResult.Success(
                AiCompletion(
                    message = AiMessage(role = "assistant", content = result.value.content),
                    finishReason = "stop",
                    usage = AiUsage(),
                ),
            )
            is LocalRuntimeResult.Failure -> ApiResult.UnexpectedError(result.message, result.code)
        }
    }

    override suspend fun supportsToolCalls(config: AiProviderConfig): Boolean = false
}

internal fun prepareLocalRequest(request: AiChatRequest): LocalInferenceRequest? {
    if (request.messages.any { message ->
            message.contentParts?.any { it !is AiContentPart.Text } == true ||
                message.toolCalls.isNotEmpty() || message.toolCallId != null
        }
    ) return null

    val system = request.messages.filter { it.role == "system" }
        .mapNotNull { it.textContent().takeIf(String::isNotBlank) }
        .joinToString("\n\n")
        .takeIf(String::isNotBlank)
    val conversational = request.messages.filter { it.role == "user" || it.role == "assistant" }
    val lastUserIndex = conversational.indexOfLast { it.role == "user" }
    if (lastUserIndex < 0) return null
    val prompt = conversational[lastUserIndex].textContent()
    if (prompt.isBlank()) return null
    val history = conversational.take(lastUserIndex).map {
        LocalInferenceMessage(it.role, it.textContent())
    }
    return LocalInferenceRequest(
        systemInstruction = system,
        initialMessages = history,
        prompt = prompt,
        temperature = request.temperature ?: 0.7,
        topP = request.topP ?: 0.8,
        maxOutputTokens = request.maxTokens ?: 1_024,
    )
}

private fun AiMessage.textContent(): String = buildString {
    content?.let(::append)
    contentParts?.filterIsInstance<AiContentPart.Text>()?.forEach {
        if (isNotEmpty()) append('\n')
        append(it.text)
    }
}
