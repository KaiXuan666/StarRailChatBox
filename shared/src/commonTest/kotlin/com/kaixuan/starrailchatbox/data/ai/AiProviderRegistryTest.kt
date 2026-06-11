package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.api.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiProviderRegistryTest {
    @Test
    fun resolvesKnownProviderAndExplicitAliasesOnly() {
        val provider = StubProvider(OpenAiCompatibleProvider.Id)
        val aliProvider = StubProvider(AliCompatibleProvider.Id)
        val xiaomiProvider = StubProvider(XiaomiMimoProvider.Id)
        val registry = AiProviderRegistry(listOf(provider, aliProvider, xiaomiProvider))

        assertEquals(provider, registry.find(OpenAiCompatibleProvider.Id))
        assertEquals(provider, registry.find("custom"))
        assertEquals(provider, registry.find("openai"))
        assertEquals(aliProvider, registry.find(AliCompatibleProvider.Id))
        assertEquals(xiaomiProvider, registry.find(XiaomiMimoProvider.Id))
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
