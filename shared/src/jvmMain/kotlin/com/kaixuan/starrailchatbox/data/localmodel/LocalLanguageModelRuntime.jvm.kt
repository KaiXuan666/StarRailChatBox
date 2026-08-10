package com.kaixuan.starrailchatbox.data.localmodel

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

actual fun createLocalLanguageModelRuntime(): LocalLanguageModelRuntime = LiteRtLocalRuntime()

private class LiteRtLocalRuntime : LocalLanguageModelRuntime {
    override val isSupported: Boolean = true
    private val _status = MutableStateFlow(LocalRuntimeStatus())
    override val status = _status.asStateFlow()
    private val inferenceMutex = Mutex()
    private val engines = mutableMapOf<String, EngineHolder>()

    override suspend fun validate(modelPath: String): LocalRuntimeResult = withContext(Dispatchers.Default) {
        try {
            Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU())).use {
                it.initialize()
            }
            LocalRuntimeResult.Success(LocalInferenceResult("", InferenceBackend.CPU))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            LocalRuntimeResult.Failure(
                "local_model_incompatible",
                error.message ?: "LiteRT-LM could not initialize this model.",
            )
        }
    }

    override suspend fun complete(
        model: LocalModel,
        request: LocalInferenceRequest,
    ): LocalRuntimeResult {
        _status.value = _status.value.copy(modelId = model.id, isBusy = true)
        return try {
            inferenceMutex.withLock {
                _status.value = _status.value.copy(modelId = model.id, isBusy = true)
                withContext(Dispatchers.Default) {
            try {
                val holder = engineFor(model)
                val initialMessages = request.initialMessages.mapNotNull { message ->
                    when (message.role) {
                        "user" -> Message.user(message.text)
                        "assistant" -> Message.model(message.text)
                        else -> null
                    }
                }
                val conversationConfig = ConversationConfig(
                    systemInstruction = request.systemInstruction?.let(Contents::of),
                    initialMessages = initialMessages,
                    samplerConfig = SamplerConfig(
                        topK = request.topK,
                        topP = request.topP,
                        temperature = request.temperature,
                    ),
                    automaticToolCalling = false,
                    channels = emptyList(),
                    maxOutputToken = request.maxOutputTokens,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                )
                val conversation = holder.engine.createConversation(conversationConfig)
                try {
                    val output = StringBuilder()
                    conversation.sendMessageAsync(
                        request.prompt,
                        thinkingConfig = ThinkingConfig(enableThinking = false),
                    ).collect { message ->
                        message.contents.contents.filterIsInstance<Content.Text>().forEach {
                            output.append(it.text)
                        }
                    }
                    LocalRuntimeResult.Success(
                        LocalInferenceResult(
                            content = output.toString(),
                            backend = holder.backend,
                            fallbackReason = holder.fallbackReason,
                        ),
                    )
                } catch (cancellation: CancellationException) {
                    conversation.cancelProcess()
                    throw cancellation
                } finally {
                    conversation.close()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                val message = error.message ?: "Local inference failed."
                LocalRuntimeResult.Failure(
                    code = if (message.contains("token", ignoreCase = true) ||
                        message.contains("context", ignoreCase = true)
                    ) {
                        "local_context_too_long"
                    } else {
                        "local_inference_failed"
                    },
                    message = message,
                )
            }
                }
            }
        } finally {
            _status.value = _status.value.copy(isBusy = false)
        }
    }

    override suspend fun close(modelId: String?) {
        inferenceMutex.withLock {
            if (modelId == null) {
                engines.values.forEach { it.engine.close() }
                engines.clear()
            } else {
                engines.remove(modelId)?.engine?.close()
            }
            if (modelId == null || _status.value.modelId == modelId) {
                _status.value = LocalRuntimeStatus()
            }
        }
    }

    private fun engineFor(model: LocalModel): EngineHolder {
        engines[model.id]?.takeIf { it.modelPath == model.filePath }?.let { return it }
        engines.remove(model.id)?.engine?.close()
        val dependencyFailure = windowsGpuDependencyFailure()
        val gpuFailure = if (dependencyFailure != null) {
            Result.failure(IllegalStateException(dependencyFailure))
        } else runCatching {
            val engine = Engine(
                EngineConfig(
                    modelPath = model.filePath,
                    backend = Backend.GPU(),
                    maxNumTokens = model.contextWindow,
                ),
            )
            try {
                engine.initialize()
                engine
            } catch (error: Throwable) {
                engine.close()
                throw error
            }
        }
        val holder = gpuFailure.fold(
            onSuccess = {
                EngineHolder(model.filePath, it, InferenceBackend.GPU, null)
            },
            onFailure = { gpuError ->
                val cpu = Engine(
                    EngineConfig(
                        modelPath = model.filePath,
                        backend = Backend.CPU(),
                        maxNumTokens = model.contextWindow,
                    ),
                )
                cpu.initialize()
                EngineHolder(
                    model.filePath,
                    cpu,
                    InferenceBackend.CPU,
                    gpuError.message ?: "GPU initialization failed.",
                )
            },
        )
        engines[model.id] = holder
        _status.value = LocalRuntimeStatus(model.id, holder.backend, holder.fallbackReason, isBusy = true)
        return holder
    }
}

private fun windowsGpuDependencyFailure(): String? {
    if (!System.getProperty("os.name").contains("windows", ignoreCase = true)) return null
    val searchDirectories = buildList {
        add(File(System.getProperty("user.dir")))
        System.getProperty("java.library.path").orEmpty()
            .split(File.pathSeparatorChar)
            .filter(String::isNotBlank)
            .mapTo(this, ::File)
    }
    val hasCompiler = searchDirectories.any { File(it, "dxcompiler.dll").isFile }
    val hasDxil = searchDirectories.any { File(it, "dxil.dll").isFile }
    return if (hasCompiler && hasDxil) null else {
        "Windows GPU requires dxcompiler.dll and dxil.dll next to the executable or on java.library.path."
    }
}

private data class EngineHolder(
    val modelPath: String,
    val engine: Engine,
    val backend: InferenceBackend,
    val fallbackReason: String?,
)
