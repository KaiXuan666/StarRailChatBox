package com.kaixuan.starrailchatbox.data.model

import com.kaixuan.starrailchatbox.data.database.dao.ModelConfigDao
import com.kaixuan.starrailchatbox.data.database.entity.ModelConfigEntity
import com.kaixuan.starrailchatbox.data.settings.ApiKeyCipher
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomModelConfigRepository(
    private val dao: ModelConfigDao,
    private val cipher: ApiKeyCipher,
    private val currentTimeMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : ModelConfigRepository {
    override suspend fun getDefault(): ModelConfig? {
        return dao.findById(DefaultModelConfig.Id)?.toModelConfig()
    }

    override fun observeDefault(): Flow<ModelConfig?> {
        return dao.observeById(DefaultModelConfig.Id).map {
            it?.toModelConfig()
        }
    }

    override suspend fun saveDefault(config: ModelConfig) {
        saveConfig(DefaultModelConfig.Id, config)
    }

    override suspend fun getMultimodal(): ModelConfig? {
        return dao.findById(MultimodalModelConfig.Id)?.toModelConfig()
    }

    override fun observeMultimodal(): Flow<ModelConfig?> {
        return dao.observeById(MultimodalModelConfig.Id).map { it?.toModelConfig() }
    }

    override suspend fun saveMultimodal(config: ModelConfig) {
        saveConfig(MultimodalModelConfig.Id, config)
    }

    override suspend fun getVoice(): ModelConfig? {
        return dao.findById(VoiceModelConfig.Id)?.toModelConfig()
    }

    override fun observeVoice(): Flow<ModelConfig?> {
        return dao.observeById(VoiceModelConfig.Id).map { it?.toModelConfig() }
    }

    override suspend fun saveVoice(config: ModelConfig) {
        saveConfig(VoiceModelConfig.Id, config)
    }

    override suspend fun getVoiceClone(): ModelConfig? {
        return dao.findById(VoiceCloneModelConfig.Id)?.toModelConfig()
    }

    override fun observeVoiceClone(): Flow<ModelConfig?> {
        return dao.observeById(VoiceCloneModelConfig.Id).map { it?.toModelConfig() }
    }

    override suspend fun saveVoiceClone(config: ModelConfig) {
        saveConfig(VoiceCloneModelConfig.Id, config)
    }

    override suspend fun getImageGeneration(): ModelConfig? {
        return dao.findById(ImageGenerationModelConfig.Id)?.toModelConfig()
    }

    override fun observeImageGeneration(): Flow<ModelConfig?> {
        return dao.observeById(ImageGenerationModelConfig.Id).map { it?.toModelConfig() }
    }

    override suspend fun saveImageGeneration(config: ModelConfig) {
        saveConfig(ImageGenerationModelConfig.Id, config)
    }

    override suspend fun deleteConfig(id: String) {
        dao.deleteById(id)
    }

    private suspend fun saveConfig(id: String, config: ModelConfig) {
        val existing = dao.findById(id)
        val now = currentTimeMillis()
        dao.upsert(
            ModelConfigEntity(
                id = id,
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
