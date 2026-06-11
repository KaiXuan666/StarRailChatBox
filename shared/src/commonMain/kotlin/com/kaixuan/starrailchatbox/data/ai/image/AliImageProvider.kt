package com.kaixuan.starrailchatbox.data.ai.image

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.plugins.timeout
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class AliImageProvider(
    private val httpClient: HttpClient,
) : ImageGenerationProvider {
    override val id: String = ImageGenerationProviderIds.Ali

    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
    ): ApiResult<List<String>> = imageApiCall {
        httpClient.get("${CompatibleModeBaseUrl}models") {
            header(HttpHeaders.Authorization, "Bearer ${apiKey.trim()}")
        }.body<AliModelsResponse>().data
            .map(AliModel::id)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .sorted()
    }

    override suspend fun generate(
        config: ModelConfig,
        request: ImageGenerationRequest,
    ): ApiResult<ImageGenerationOutput> = imageApiCall {
        val response = httpClient.post(GenerationEndpoint) {
            timeout {
                requestTimeoutMillis = ImageGenerationTimeoutMillis
                socketTimeoutMillis = ImageGenerationTimeoutMillis
            }
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey.trim()}")
            header("api-key", config.apiKey.trim())
            contentType(ContentType.Application.Json)
            setBody(
                AliImageGenerationRequest(
                    model = config.modelName,
                    input = AliImageInput(
                        messages = listOf(
                            AliImageMessage(
                                content = listOf(AliImageContent(text = request.prompt)),
                            ),
                        ),
                    ),
                    parameters = AliImageParameters(
                        size = request.aspectRatio.toAliSize(),
                    ),
                ),
            )
        }.body<AliImageGenerationResponse>()

        val imageUrl = response.output?.choices
            .orEmpty()
            .firstOrNull()
            ?.message
            ?.content
            .orEmpty()
            .firstNotNullOfOrNull(AliImageContent::image)
        if (imageUrl.isNullOrBlank()) {
            throw InvalidImageResponseException("Ali image generation returned no usable image data.")
        }
        ImageGenerationOutput.Url(imageUrl)
    }

    companion object {
        const val CompatibleModeBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/"
        const val GenerationEndpoint =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
    }
}

@Serializable
private data class AliModelsResponse(
    val data: List<AliModel> = emptyList(),
)

@Serializable
private data class AliModel(
    val id: String,
)

@Serializable
private data class AliImageGenerationRequest(
    val model: String,
    val input: AliImageInput,
    val parameters: AliImageParameters,
)

@Serializable
private data class AliImageInput(
    val messages: List<AliImageMessage>,
)

@Serializable
private data class AliImageMessage(
    val role: String = "user",
    val content: List<AliImageContent>,
)

@Serializable
private data class AliImageContent(
    val text: String? = null,
    val image: String? = null,
)

@Serializable
private data class AliImageParameters(
    val size: String,
    @SerialName("prompt_extend")
    val promptExtend: Boolean = false,
)

@Serializable
private data class AliImageGenerationResponse(
    val output: AliImageOutput? = null,
)

@Serializable
private data class AliImageOutput(
    val choices: List<AliImageChoice> = emptyList(),
)

@Serializable
private data class AliImageChoice(
    val message: AliImageResponseMessage? = null,
)

@Serializable
private data class AliImageResponseMessage(
    val content: List<AliImageContent> = emptyList(),
)

private fun String.toAliSize(): String = when (this) {
    "4:3" -> "1280*960"
    "3:4" -> "960*1280"
    "16:9" -> "1440*810"
    "9:16" -> "810*1440"
    else -> "1024*1024"
}
