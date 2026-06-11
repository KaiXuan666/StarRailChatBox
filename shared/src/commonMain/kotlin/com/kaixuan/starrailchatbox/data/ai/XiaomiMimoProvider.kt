package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.api.ApiResult
import kotlinx.coroutines.flow.Flow

object XiaomiMimo {
    const val SubscriptionBaseUrl = "https://token-plan-cn.xiaomimimo.com/v1"
    const val UsageBasedBaseUrl = "https://api.xiaomimimo.com/v1"
}

/**
 * 小米 MiMo 使用 OpenAI 兼容协议，并在模型发现时自动选择可用计费 Host。
 */
class XiaomiMimoProvider(
    private val delegate: OpenAiCompatibleProvider,
) : AiProvider {
    override val id: String = Id

    override suspend fun getModels(config: AiProviderConfig): ApiResult<List<String>> {
        return when (val result = discoverModels(config)) {
            is ApiResult.Success -> ApiResult.Success(result.value.models)
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnexpectedError -> result
        }
    }

    override suspend fun discoverModels(config: AiProviderConfig): ApiResult<AiModelDiscovery> {
        var lastFailure: ApiResult<Nothing>? = null
        var sawEmptySuccess = false

        for (host in listOf(XiaomiMimo.SubscriptionBaseUrl, XiaomiMimo.UsageBasedBaseUrl)) {
            when (val result = delegate.getModels(config.copy(apiHost = host))) {
                is ApiResult.Success -> {
                    if (result.value.isNotEmpty()) {
                        return ApiResult.Success(
                            AiModelDiscovery(
                                models = result.value,
                                resolvedApiHost = host,
                            ),
                        )
                    }
                    sawEmptySuccess = true
                }
                is ApiResult.HttpError -> lastFailure = result
                is ApiResult.NetworkError -> lastFailure = result
                is ApiResult.UnexpectedError -> lastFailure = result
            }
        }

        if (lastFailure != null && !sawEmptySuccess) return lastFailure
        return ApiResult.Success(
            AiModelDiscovery(
                models = emptyList(),
                resolvedApiHost = config.apiHost,
            ),
        )
    }

    override suspend fun complete(
        config: AiProviderConfig,
        request: AiChatRequest,
    ): ApiResult<AiCompletion> = delegate.complete(config, request)

    override fun completeStreaming(
        config: AiProviderConfig,
        request: AiChatRequest,
    ): Flow<ApiResult<AiCompletionChunk>> = delegate.completeStreaming(config, request)

    override suspend fun supportsToolCalls(config: AiProviderConfig): Boolean {
        return delegate.supportsToolCalls(config)
    }

    companion object {
        const val Id = "xiaomimimo"
    }
}
