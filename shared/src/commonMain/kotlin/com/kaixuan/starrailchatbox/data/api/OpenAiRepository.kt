package com.kaixuan.starrailchatbox.data.api

import de.jensklingenberg.ktorfit.ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException

interface OpenAiRepository {
    suspend fun getModels(
        apiHost: String,
        apiKey: String,
    ): ApiResult<List<String>>
}

class KtorfitOpenAiRepository(
    private val httpClient: HttpClient,
) : OpenAiRepository {
    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
    ): ApiResult<List<String>> {
        return try {
            val api = ktorfit {
                baseUrl(apiHost.normalizedBaseUrl())
                httpClient(httpClient)
            }.createOpenAiApi()

            val models = api.getModels(
                authorization = "Bearer ${apiKey.trim()}",
            ).data
                .map { it.id.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()

            ApiResult.Success(models)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: ResponseException) {
            ApiResult.HttpError(
                statusCode = error.response.status.value,
                message = error.message,
            )
        } catch (error: Throwable) {
            ApiResult.NetworkError(error.message)
        }
    }
}

internal fun String.normalizedBaseUrl(): String {
    val normalized = trim().trimEnd('/')
    return "$normalized/"
}
