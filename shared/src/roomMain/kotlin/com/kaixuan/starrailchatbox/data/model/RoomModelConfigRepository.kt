package com.kaixuan.starrailchatbox.data.model

import com.kaixuan.starrailchatbox.data.database.dao.ModelConfigDao
import com.kaixuan.starrailchatbox.data.database.entity.ModelConfigEntity
import com.kaixuan.starrailchatbox.data.settings.ApiKeyCipher

class RoomModelConfigRepository(
    private val dao: ModelConfigDao,
    private val cipher: ApiKeyCipher,
    private val currentTimeMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : ModelConfigRepository {
    override suspend fun getDefault(): ModelConfig? {
        return dao.findById(DefaultModelConfig.Id)?.toModelConfig()
    }

    override suspend fun saveDefault(config: ModelConfig) {
        val existing = dao.findById(DefaultModelConfig.Id)
        val now = currentTimeMillis()
        dao.upsert(
            ModelConfigEntity(
                id = DefaultModelConfig.Id,
                provider = config.provider,
                name = config.name,
                baseUrl = config.baseUrl,
                apiKeyEncrypted = cipher.encrypt(config.apiKey),
                modelName = config.modelName,
                contextWindow = config.contextWindow,
                maxOutputTokens = config.maxOutputTokens,
                supportVision = config.supportVision,
                supportToolCall = config.supportToolCall,
                supportReasoning = config.supportReasoning,
                temperature = config.temperature,
                topP = config.topP,
                enabled = config.enabled,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    private suspend fun ModelConfigEntity.toModelConfig() = ModelConfig(
        id = id,
        provider = provider,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKeyEncrypted?.let { cipher.decrypt(it) }.orEmpty(),
        modelName = modelName,
        contextWindow = contextWindow,
        maxOutputTokens = maxOutputTokens,
        supportVision = supportVision,
        supportToolCall = supportToolCall,
        supportReasoning = supportReasoning,
        temperature = temperature,
        topP = topP,
        enabled = enabled,
    )
}
