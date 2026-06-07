package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.api.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiProviderRegistryTest {
    @Test
    fun resolvesKnownProviderAndExplicitAliasesOnly() {
        val provider = StubProvider(OpenAiCompatibleProvider.Id)
        val registry = AiProviderRegistry(listOf(provider))

        assertEquals(provider, registry.find(OpenAiCompatibleProvider.Id))
        assertEquals(provider, registry.find("custom"))
        assertEquals(provider, registry.find("openai"))
        assertNull(registry.find("typo"))
    }
}

private class StubProvider(override val id: String) : AiProvider {
    override suspend fun getModels(config: AiProviderConfig): ApiResult<List<String>> {
        return ApiResult.Success(emptyList())
    }

    override suspend fun complete(
        config: AiProviderConfig,
        request: AiChatRequest,
    ): ApiResult<AiCompletion> {
        return ApiResult.UnexpectedError("Not used.")
    }

    override suspend fun supportsToolCalls(config: AiProviderConfig): Boolean = false
}
