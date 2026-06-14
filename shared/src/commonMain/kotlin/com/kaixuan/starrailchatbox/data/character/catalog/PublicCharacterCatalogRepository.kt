package com.kaixuan.starrailchatbox.data.character.catalog

import com.kaixuan.starrailchatbox.data.api.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
data class PublicCatalog(
    val schemaVersion: Int,
    val catalogVersion: String,
    val generatedAt: String,
    val categoriesUrl: String,
    val tags: List<PublicTag> = emptyList(),
)

@Serializable
data class PublicTag(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val firstPageUrl: String,
)

@Serializable
data class PublicCategories(
    val schemaVersion: Int,
    val catalogVersion: String,
    val categories: List<PublicCategory>,
)

@Serializable
data class PublicCategory(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val characterCount: Int,
    val firstPageUrl: String,
)

@Serializable
data class PublicCharacterPage(
    val schemaVersion: Int,
    val catalogVersion: String,
    val categoryId: String? = null,
    val tagId: String? = null,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
    val items: List<PublicCharacterSummary>,
)

@Serializable
data class PublicCharacterSummary(
    val characterKey: String,
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val primaryCategoryId: String,
    val tagIds: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val updatedAt: String,
    val revision: String,
    val detailUrl: String,
)

@Serializable
data class PublicCharacterDetail(
    val schemaVersion: Int,
    val characterKey: String,
    val revision: String,
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val systemPrompt: String,
    val openingMessage: String,
    val temperature: Double,
    val topP: Double,
    val primaryCategoryId: String,
    val tagIds: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val voiceSampleUrl: String? = null,
    val updatedAt: String,
)

data class PublicCatalogFetch(
    val catalog: PublicCatalog?,
    val etag: String?,
    val notModified: Boolean,
)

interface PublicCharacterCatalogRepository {
    suspend fun getCatalog(etag: String? = null): ApiResult<PublicCatalogFetch>
    suspend fun getCategories(url: String): ApiResult<PublicCategories>
    suspend fun getCharacterPage(url: String): ApiResult<PublicCharacterPage>
    suspend fun getCharacterDetail(url: String): ApiResult<PublicCharacterDetail>
    fun resolveUrl(url: String): String
}

class DefaultPublicCharacterCatalogRepository(
    private val httpClient: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
) : PublicCharacterCatalogRepository {
    override suspend fun getCatalog(etag: String?): ApiResult<PublicCatalogFetch> {
        return request {
            val response = httpClient.get(resolveUrl("/api/v1/catalog.json")) {
                expectSuccess = false
                if (!etag.isNullOrBlank()) {
                    header(HttpHeaders.IfNoneMatch, etag)
                }
            }
            if (response.status == HttpStatusCode.NotModified) {
                PublicCatalogFetch(catalog = null, etag = etag, notModified = true)
            } else if (response.status.value in 200..299) {
                val catalog = response.body<PublicCatalog>()
                requireSupportedSchema(catalog.schemaVersion)
                PublicCatalogFetch(
                    catalog = catalog,
                    etag = response.headers[HttpHeaders.ETag],
                    notModified = false,
                )
            } else {
                throw CatalogHttpException(response.status.value, response.status.description)
            }
        }
    }

    override suspend fun getCategories(url: String): ApiResult<PublicCategories> =
        request {
            httpClient.get(resolveUrl(url)).body<PublicCategories>().also {
                requireSupportedSchema(it.schemaVersion)
            }
        }

    override suspend fun getCharacterPage(url: String): ApiResult<PublicCharacterPage> =
        request {
            httpClient.get(resolveUrl(url)).body<PublicCharacterPage>().also {
                requireSupportedSchema(it.schemaVersion)
            }
        }

    override suspend fun getCharacterDetail(url: String): ApiResult<PublicCharacterDetail> =
        request {
            httpClient.get(resolveUrl(url)).body<PublicCharacterDetail>().also {
                requireSupportedSchema(it.schemaVersion)
            }
        }

    override fun resolveUrl(url: String): String {
        if (url.startsWith("https://") || url.startsWith("http://")) return url
        return "${baseUrl.trimEnd('/')}/${url.trimStart('/')}"
    }

    private suspend fun <T> request(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: CatalogHttpException) {
            ApiResult.HttpError(error.statusCode, error.message)
        } catch (error: IllegalStateException) {
            ApiResult.UnexpectedError(error.message)
        } catch (error: Throwable) {
            ApiResult.NetworkError(error.message)
        }
    }

    private fun requireSupportedSchema(schemaVersion: Int) {
        if (schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            throw IllegalStateException("catalog_schema_unsupported")
        }
    }

    private class CatalogHttpException(
        val statusCode: Int,
        message: String,
    ) : IllegalStateException(message)

    companion object {
        const val DEFAULT_BASE_URL = "https://cards.qyaichat.com"
        const val SUPPORTED_SCHEMA_VERSION = 1
    }
}
