package com.kaixuan.starrailchatbox.data.localmodel

import com.kaixuan.starrailchatbox.data.ai.AiChatRequest
import com.kaixuan.starrailchatbox.data.ai.AiContentPart
import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiProviderConfig
import com.kaixuan.starrailchatbox.data.ai.LiteRtLmProvider
import com.kaixuan.starrailchatbox.data.ai.prepareLocalRequest
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.InMemoryModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.settings.InMemoryAppSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalModelIntegrationTest {
    @Test
    fun syntheticLocalConfigIdIsNotPersistedAsOnlineConfigForeignKey() {
        val local = onlineConfig().copy(
            id = "local:qwen",
            provider = LiteRtLmProviderId,
        )
        val online = onlineConfig().copy(
            id = "default",
            provider = "openai-compatible",
        )

        assertNull(local.persistedModelConfigId())
        assertEquals("default", online.persistedModelConfigId())
    }

    @Test
    fun resolverSwitchesModesWithoutOverwritingOnlineConfiguration() = runTest {
        val online = onlineConfig()
        val onlineRepository = InMemoryModelConfigRepository(online)
        val localRepository = InMemoryLocalModelRepository(listOf(localModel()))
        val settings = InMemoryAppSettingsStore()
        val resolver = ChatModelResolver(onlineRepository, localRepository, settings)

        assertEquals(online, resolver.resolveTextModel())

        settings.setSelectedLocalModelId("qwen")
        settings.setChatModelMode(ChatModelMode.LOCAL)
        val local = requireNotNull(resolver.resolveTextModel())
        assertEquals("local:qwen", local.id)
        assertEquals(LiteRtLmProviderId, local.provider)
        assertEquals("", local.baseUrl)
        assertEquals("", local.apiKey)
        assertEquals(0.7, local.temperature)
        assertEquals(0.8, local.topP)

        settings.setChatModelMode(ChatModelMode.ONLINE)
        assertEquals(online, resolver.resolveTextModel())
        assertEquals(online, onlineRepository.getDefault())
    }

    @Test
    fun resolverRequiresAnInstalledSelectedModel() = runTest {
        val settings = InMemoryAppSettingsStore()
        settings.setSelectedLocalModelId("missing")
        settings.setChatModelMode(ChatModelMode.LOCAL)

        assertNull(
            ChatModelResolver(
                InMemoryModelConfigRepository(onlineConfig()),
                InMemoryLocalModelRepository(),
                settings,
            ).resolveTextModel(),
        )
    }

    @Test
    fun requestMappingPreservesSystemHistoryAndCurrentInputOrder() {
        val prepared = requireNotNull(
            prepareLocalRequest(
                AiChatRequest(
                    model = "qwen",
                    messages = listOf(
                        AiMessage("system", "role prompt"),
                        AiMessage("system", "summary"),
                        AiMessage("user", "old question"),
                        AiMessage("assistant", "old answer"),
                        AiMessage("user", "current question"),
                    ),
                    temperature = 0.65,
                    topP = 0.75,
                    maxTokens = 512,
                ),
            ),
        )

        assertEquals("role prompt\n\nsummary", prepared.systemInstruction)
        assertEquals(
            listOf(
                LocalInferenceMessage("user", "old question"),
                LocalInferenceMessage("assistant", "old answer"),
            ),
            prepared.initialMessages,
        )
        assertEquals("current question", prepared.prompt)
        assertEquals(0.65, prepared.temperature)
        assertEquals(0.75, prepared.topP)
        assertEquals(512, prepared.maxOutputTokens)
    }

    @Test
    fun providerRejectsAttachmentsAndDisablesTools() = runTest {
        val runtime = RecordingRuntime()
        val provider = LiteRtLmProvider(InMemoryLocalModelRepository(listOf(localModel())), runtime)
        val request = AiChatRequest(
            model = "qwen",
            messages = listOf(
                AiMessage(
                    role = "user",
                    contentParts = listOf(AiContentPart.ImageUrl("data:image/png;base64,AA==")),
                ),
            ),
        )

        val result = assertIs<ApiResult.UnexpectedError>(provider.complete(providerConfig(), request))
        assertEquals("local_multimodal_unsupported", result.code)
        assertFalse(runtime.completeCalled)
        assertFalse(provider.supportsToolCalls(providerConfig()))
    }

    @Test
    fun providerReturnsZeroUsageAndRuntimeReply() = runTest {
        val runtime = RecordingRuntime()
        val provider = LiteRtLmProvider(InMemoryLocalModelRepository(listOf(localModel())), runtime)
        val result = assertIs<ApiResult.Success<*>>(
            provider.complete(
                providerConfig(),
                AiChatRequest("qwen", listOf(AiMessage("user", "hello"))),
            ),
        )
        val completion = result.value as com.kaixuan.starrailchatbox.data.ai.AiCompletion
        assertEquals("reply", completion.message.content)
        assertEquals(0, completion.usage.totalTokens)
        assertTrue(runtime.completeCalled)
        assertEquals(40, runtime.lastRequest?.topK)
    }
}

private class RecordingRuntime : LocalLanguageModelRuntime {
    override val isSupported = true
    override val status = MutableStateFlow(LocalRuntimeStatus())
    var completeCalled = false
    var lastRequest: LocalInferenceRequest? = null

    override suspend fun validate(modelPath: String): LocalRuntimeResult =
        LocalRuntimeResult.Success(LocalInferenceResult("", InferenceBackend.CPU))

    override suspend fun complete(model: LocalModel, request: LocalInferenceRequest): LocalRuntimeResult {
        completeCalled = true
        lastRequest = request
        return LocalRuntimeResult.Success(LocalInferenceResult("reply", InferenceBackend.CPU))
    }

    override suspend fun close(modelId: String?) = Unit
}

private fun localModel() = LocalModel(
    id = "qwen",
    name = "Qwen",
    filePath = "/models/qwen.litertlm",
    sizeBytes = 100,
    sha256 = "abc",
    source = LocalModelSource.CATALOG,
    sourceUrl = "https://example.invalid/model",
    license = "Apache-2.0",
    contextWindow = 4_096,
    maxOutputTokens = 1_024,
    createdAt = 1,
    updatedAt = 1,
)

private fun onlineConfig() = ModelConfig(
    id = "default",
    provider = "custom",
    name = "Online",
    baseUrl = "https://example.invalid/v1",
    apiKey = "key",
    modelName = "online-model",
    contextWindow = 8_192,
    maxOutputTokens = 2_048,
    supportVision = false,
    supportToolCall = true,
    supportReasoning = false,
    temperature = 0.7,
    topP = 1.0,
    enabled = true,
)

private fun providerConfig() = AiProviderConfig(
    providerId = LiteRtLmProviderId,
    apiHost = "",
    apiKey = "",
    model = "qwen",
)
