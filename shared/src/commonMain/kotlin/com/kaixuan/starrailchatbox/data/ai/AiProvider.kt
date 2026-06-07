package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.api.ApiResult

/**
 * 一类 AI 服务协议或 Provider 家族的适配策略。
 *
 * 实现类负责鉴权、网络调用、协议 DTO 转换和能力探测；具体工具行为归工具层负责。
 */
interface AiProvider {
    val id: String

    suspend fun getModels(config: AiProviderConfig): ApiResult<List<String>>

    suspend fun complete(
        config: AiProviderConfig,
        request: AiChatRequest,
    ): ApiResult<AiCompletion>

    suspend fun supportsToolCalls(config: AiProviderConfig): Boolean
}

/**
 * 根据持久化的 Provider ID 和显式兼容别名查找 Provider。
 *
 * 未知 ID 不会静默回退，避免配置拼写错误时把凭据发送到非预期服务。
 */
class AiProviderRegistry(
    providers: List<AiProvider>,
    aliases: Map<String, String> = mapOf(
        "custom" to OpenAiCompatibleProvider.Id,
        "openai" to OpenAiCompatibleProvider.Id,
    ),
) {
    private val providersById = providers.associateBy(AiProvider::id)
    private val aliasesById = aliases

    fun find(providerId: String): AiProvider? {
        return providersById[providerId]
            ?: aliasesById[providerId]?.let(providersById::get)
    }
}
