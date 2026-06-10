package com.kaixuan.starrailchatbox.data.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ModelConfig(
    val id: String,
    val provider: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val contextWindow: Int,
    val maxOutputTokens: Int,
    val supportVision: Boolean,
    val supportToolCall: Boolean,
    val supportReasoning: Boolean,
    val temperature: Double,
    val topP: Double,
    val enabled: Boolean,
)

interface ModelConfigRepository {
    suspend fun getDefault(): ModelConfig?

    fun observeDefault(): Flow<ModelConfig?>

    suspend fun saveDefault(config: ModelConfig)

    suspend fun getMultimodal(): ModelConfig?

    fun observeMultimodal(): Flow<ModelConfig?>

    suspend fun saveMultimodal(config: ModelConfig)

    suspend fun getVoice(): ModelConfig?

    fun observeVoice(): Flow<ModelConfig?>

    suspend fun saveVoice(config: ModelConfig)

    suspend fun getVoiceClone(): ModelConfig?

    fun observeVoiceClone(): Flow<ModelConfig?>

    suspend fun saveVoiceClone(config: ModelConfig)

    suspend fun getImageGeneration(): ModelConfig?

    fun observeImageGeneration(): Flow<ModelConfig?>

    suspend fun saveImageGeneration(config: ModelConfig)

    suspend fun deleteConfig(id: String)
}

class InMemoryModelConfigRepository(
    initial: ModelConfig? = null,
    initialMultimodal: ModelConfig? = null,
    initialVoice: ModelConfig? = null,
) : ModelConfigRepository {
    private val _config = MutableStateFlow(initial)
    private val _multimodalConfig = MutableStateFlow(initialMultimodal)
    private val _voiceConfig = MutableStateFlow(initialVoice)
    private val _voiceCloneConfig = MutableStateFlow<ModelConfig?>(null)
    private val _imageGenerationConfig = MutableStateFlow<ModelConfig?>(null)

    override suspend fun getDefault(): ModelConfig? = _config.value

    override fun observeDefault(): Flow<ModelConfig?> = _config.asStateFlow()

    override suspend fun saveDefault(config: ModelConfig) {
        _config.value = config
    }

    override suspend fun getMultimodal(): ModelConfig? = _multimodalConfig.value

    override fun observeMultimodal(): Flow<ModelConfig?> = _multimodalConfig.asStateFlow()

    override suspend fun saveMultimodal(config: ModelConfig) {
        _multimodalConfig.value = config
    }

    override suspend fun getVoice(): ModelConfig? = _voiceConfig.value

    override fun observeVoice(): Flow<ModelConfig?> = _voiceConfig.asStateFlow()

    override suspend fun saveVoice(config: ModelConfig) {
        _voiceConfig.value = config
    }

    override suspend fun getVoiceClone(): ModelConfig? = _voiceCloneConfig.value

    override fun observeVoiceClone(): Flow<ModelConfig?> = _voiceCloneConfig.asStateFlow()

    override suspend fun saveVoiceClone(config: ModelConfig) {
        _voiceCloneConfig.value = config
    }

    override suspend fun getImageGeneration(): ModelConfig? = _imageGenerationConfig.value

    override fun observeImageGeneration(): Flow<ModelConfig?> = _imageGenerationConfig.asStateFlow()

    override suspend fun saveImageGeneration(config: ModelConfig) {
        _imageGenerationConfig.value = config
    }

    override suspend fun deleteConfig(id: String) {
        when (id) {
            DefaultModelConfig.Id -> _config.value = null
            MultimodalModelConfig.Id -> _multimodalConfig.value = null
            VoiceModelConfig.Id -> _voiceConfig.value = null
            VoiceCloneModelConfig.Id -> _voiceCloneConfig.value = null
            ImageGenerationModelConfig.Id -> _imageGenerationConfig.value = null
        }
    }
}

object DefaultModelConfig {
    const val Id = "default"
    const val Provider = "custom"
    const val Name = "默认模型"
    const val ContextWindow = 128_000
    const val MaxOutputTokens = 4_096
    const val Temperature = 0.7
    const val TopP = 1.0
}

object MultimodalModelConfig {
    const val Id = "multimodal"
    const val Provider = "custom"
    const val Name = "多模态模型"
    const val ContextWindow = 128_000
    const val MaxOutputTokens = 4_096
    const val Temperature = 0.7
    const val TopP = 1.0
}

object VoiceModelConfig {
    const val Id = "voice"
    const val Provider = "xiaomimimo"
    const val Name = "语音合成模型"
    const val ContextWindow = 128_000
    const val MaxOutputTokens = 4_096
    const val Temperature = 0.7
    const val TopP = 1.0
}

object VoiceCloneModelConfig {
    const val Id = "voice_clone"
    const val Provider = "xiaomimimo"
    const val Name = "音色克隆模型"
    const val ContextWindow = 128_000
    const val MaxOutputTokens = 4_096
    const val Temperature = 0.7
    const val TopP = 1.0
}

object ImageGenerationModelConfig {
    const val Id = "image_generation"
    const val Provider = "custom"
    const val Name = "图片生成模型"
    const val ContextWindow = 128_000
    const val MaxOutputTokens = 4_096
    const val Temperature = 0.7
    const val TopP = 1.0
}

