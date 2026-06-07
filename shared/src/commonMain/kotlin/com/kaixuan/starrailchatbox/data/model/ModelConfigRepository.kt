package com.kaixuan.starrailchatbox.data.model

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

    suspend fun saveDefault(config: ModelConfig)
}

class InMemoryModelConfigRepository(
    initial: ModelConfig? = null,
) : ModelConfigRepository {
    private var config = initial

    override suspend fun getDefault(): ModelConfig? = config

    override suspend fun saveDefault(config: ModelConfig) {
        this.config = config
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
