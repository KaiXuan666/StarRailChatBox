package com.kaixuan.starrailchatbox.data.localmodel

import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import kotlinx.coroutines.flow.first

class ChatModelResolver(
    private val onlineModels: ModelConfigRepository,
    private val localModels: LocalModelRepository,
    private val settings: AppSettingsStore,
) {
    suspend fun currentMode(): ChatModelMode = settings.chatModelMode.first()

    suspend fun resolveTextModel(): ModelConfig? {
        return when (currentMode()) {
            ChatModelMode.ONLINE -> onlineModels.getDefault()?.takeIf(ModelConfig::isProviderUsable)
            ChatModelMode.LOCAL -> resolveSelectedLocalModel()
        }
    }

    suspend fun resolveMultimodalModel(): ModelConfig? =
        onlineModels.getMultimodal()?.takeIf(ModelConfig::isProviderUsable)

    private suspend fun resolveSelectedLocalModel(): ModelConfig? {
        val selectedId = settings.selectedLocalModelId.first() ?: return null
        val local = localModels.getById(selectedId) ?: return null
        return ModelConfig(
            id = "local:${local.id}",
            provider = LiteRtLmProviderId,
            name = local.name,
            baseUrl = "",
            apiKey = "",
            modelName = local.id,
            contextWindow = local.contextWindow,
            maxOutputTokens = local.maxOutputTokens,
            supportVision = false,
            supportToolCall = false,
            supportReasoning = false,
            temperature = 0.7,
            topP = 0.8,
            enabled = true,
        )
    }
}

fun ModelConfig.isProviderUsable(): Boolean = when (provider) {
    LiteRtLmProviderId -> enabled && modelName.isNotBlank()
    else -> enabled && baseUrl.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
}

/**
 * Local model configurations are resolved at request time and are not rows in model_config.
 * Persist their model snapshot, but never write their synthetic ID into a Room foreign key.
 */
fun ModelConfig.persistedModelConfigId(): String? =
    id.takeUnless { provider == LiteRtLmProviderId }

const val LiteRtLmProviderId = "litert-lm"
