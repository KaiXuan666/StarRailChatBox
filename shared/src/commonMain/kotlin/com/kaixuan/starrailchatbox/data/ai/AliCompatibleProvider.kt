package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.api.ApiResult
import kotlinx.coroutines.flow.Flow

object AliBailian {
    const val CompatibleModeBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"
}

/**
 * 阿里百炼的聊天接口使用 OpenAI 兼容协议。
 *
 * 此适配器只提供稳定的服务商 ID，协议映射统一复用 OpenAiCompatibleProvider。
 */
class AliCompatibleProvider(
    private val delegate: OpenAiCompatibleProvider,
) : AiProvider {
    override val id: String = Id

    override suspend fun getModels(config: AiProviderConfig): ApiResult<List<String>> {
        return delegate.getModels(config)
    }

    override suspend fun complete(
        config: AiProviderConfig,
        request: AiChatRequest,
    ): ApiResult<AiCompletion> {
        return delegate.complete(config, request)
    }

    override fun completeStreaming(
        config: AiProviderConfig,
        request: AiChatRequest,
    ): Flow<ApiResult<AiCompletionChunk>> {
        return delegate.completeStreaming(config, request)
    }

    override suspend fun supportsToolCalls(config: AiProviderConfig): Boolean {
        return delegate.supportsToolCalls(config)
    }

    companion object {
        const val Id = "ali-bailian"
    }
}
