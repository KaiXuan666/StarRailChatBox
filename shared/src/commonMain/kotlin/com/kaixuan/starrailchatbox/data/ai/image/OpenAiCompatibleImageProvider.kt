package com.kaixuan.starrailchatbox.data.ai.image

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

class OpenAiCompatibleImageProvider(
    private val httpClient: HttpClient,
) : ImageGenerationProvider {
    override val id: String = ImageGenerationProviderIds.OpenAiCompatible

    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
    ): ApiResult<List<String>> = imageApiCall {
        httpClient.get("${apiHost.normalizedBaseUrl()}models") {
            header(HttpHeaders.Authorization, "Bearer ${apiKey.trim()}")
        }.body<OpenAiImageModelsResponse>().data
            .map(OpenAiImageModel::id)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .sorted()
    }

    override suspend fun generate(
        config: ModelConfig,
        request: ImageGenerationRequest,
    ): ApiResult<ImageGenerationOutput> = imageApiCall {
        val response = httpClient.post("${config.baseUrl.normalizedBaseUrl()}images/generations") {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey.trim()}")
            contentType(ContentType.Application.Json)
            setBody(
                OpenAiImageGenerationRequest(
                    model = config.modelName,
                    prompt = request.prompt,
                    size = request.aspectRatio.toOpenAiSize(),
                ),
            )
        }.body<OpenAiImageGenerationResponse>()

        val image = response.data.firstOrNull()
            ?: throw InvalidImageResponseException("Image generation returned no images.")
        when {
            !image.url.isNullOrBlank() -> ImageGenerationOutput.Url(image.url)
            !image.base64Json.isNullOrBlank() -> ImageGenerationOutput.Base64(image.base64Json)
            else -> throw InvalidImageResponseException("Image generation returned no usable image data.")
        }
    }
}

@Serializable
private data class OpenAiImageModelsResponse(
    val data: List<OpenAiImageModel> = emptyList(),
)

@Serializable
private data class OpenAiImageModel(
    val id: String,
)

@Serializable
private data class OpenAiImageGenerationRequest(
    val model: String,
    val prompt: String,
    val size: String,
    val n: Int = 1,
)

@Serializable
private data class OpenAiImageGenerationResponse(
    val data: List<OpenAiGeneratedImage> = emptyList(),
)

@Serializable
private data class OpenAiGeneratedImage(
    val url: String? = null,
    @SerialName("b64_json")
    val base64Json: String? = null,
)

private fun String.toOpenAiSize(): String = when (this) {
    "4:3", "16:9" -> "1536x1024"
    "3:4", "9:16" -> "1024x1536"
    else -> "1024x1024"
}

internal fun String.normalizedBaseUrl(): String = "${trim().trimEnd('/')}/"

internal class InvalidImageResponseException(message: String) : IllegalStateException(message)

internal suspend inline fun <T> imageApiCall(crossinline block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: ResponseException) {
        ApiResult.HttpError(
            statusCode = error.response.status.value,
            message = error.message,
        )
    } catch (error: InvalidImageResponseException) {
        ApiResult.UnexpectedError(error.message)
    } catch (error: SerializationException) {
        ApiResult.UnexpectedError("Image generation returned invalid JSON.")
    } catch (error: Throwable) {
        ApiResult.NetworkError(error.message)
    }
}
