package com.kaixuan.starrailchatbox.data.ai.image

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig

data class ImageGenerationRequest(
    val prompt: String,
    val aspectRatio: String,
)

sealed interface ImageGenerationOutput {
    val mimeType: String?

    data class Url(
        val url: String,
        override val mimeType: String? = null,
    ) : ImageGenerationOutput

    data class Base64(
        val data: String,
        override val mimeType: String? = "image/png",
    ) : ImageGenerationOutput
}

interface ImageGenerationProvider {
    val id: String

    suspend fun getModels(
        apiHost: String,
        apiKey: String,
    ): ApiResult<List<String>>

    suspend fun generate(
        config: ModelConfig,
        request: ImageGenerationRequest,
    ): ApiResult<ImageGenerationOutput>
}

class ImageGenerationProviderRegistry(
    providers: List<ImageGenerationProvider>,
) {
    private val providersById = providers.associateBy(ImageGenerationProvider::id)

    fun find(providerId: String): ImageGenerationProvider? = providersById[providerId]
}

object ImageGenerationProviderIds {
    const val OpenAiCompatible = "openai-compatible-image"
    const val Ali = "ali-image"
}
