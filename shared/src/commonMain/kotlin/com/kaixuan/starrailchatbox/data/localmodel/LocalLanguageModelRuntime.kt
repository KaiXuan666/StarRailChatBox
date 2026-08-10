package com.kaixuan.starrailchatbox.data.localmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LocalInferenceMessage(
    val role: String,
    val text: String,
)

data class LocalInferenceRequest(
    val systemInstruction: String?,
    val initialMessages: List<LocalInferenceMessage>,
    val prompt: String,
    val temperature: Double,
    val topP: Double,
    val topK: Int = 40,
    val maxOutputTokens: Int,
)

data class LocalInferenceResult(
    val content: String,
    val backend: InferenceBackend,
    val fallbackReason: String? = null,
)

sealed interface LocalRuntimeResult {
    data class Success(val value: LocalInferenceResult) : LocalRuntimeResult
    data class Failure(val code: String, val message: String) : LocalRuntimeResult
}

data class LocalRuntimeStatus(
    val modelId: String? = null,
    val backend: InferenceBackend? = null,
    val fallbackReason: String? = null,
    val isBusy: Boolean = false,
)

interface LocalLanguageModelRuntime {
    val isSupported: Boolean
    val status: StateFlow<LocalRuntimeStatus>
    suspend fun validate(modelPath: String): LocalRuntimeResult
    suspend fun complete(model: LocalModel, request: LocalInferenceRequest): LocalRuntimeResult
    suspend fun close(modelId: String? = null)
}

class UnsupportedLocalLanguageModelRuntime : LocalLanguageModelRuntime {
    override val isSupported: Boolean = false
    override val status = MutableStateFlow(LocalRuntimeStatus())
    override suspend fun validate(modelPath: String) = LocalRuntimeResult.Failure(
        code = "local_model_platform_unsupported",
        message = "Local inference is not supported on this platform.",
    )
    override suspend fun complete(model: LocalModel, request: LocalInferenceRequest) = validate(model.filePath)
    override suspend fun close(modelId: String?) = Unit
}

expect fun createLocalLanguageModelRuntime(): LocalLanguageModelRuntime
