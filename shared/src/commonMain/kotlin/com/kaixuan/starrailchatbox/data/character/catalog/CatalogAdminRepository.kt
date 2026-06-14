package com.kaixuan.starrailchatbox.data.character.catalog

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.api.SuppressNetworkLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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

@Serializable
data class CatalogAdminVerifyResponse(
    val schemaVersion: Int,
    val success: Boolean,
    val admin: Boolean = false,
)

@Serializable
enum class CatalogAdminOperationType {
    @SerialName("CREATE_CATEGORY")
    CreateCategory,
    @SerialName("MOVE_CHARACTER")
    MoveCharacter,
    @SerialName("DELETE_CHARACTER")
    DeleteCharacter,
    @SerialName("REBUILD_CATALOG")
    RebuildCatalog,
}

@Serializable
data class CatalogAdminOperationPayload(
    val name: String? = null,
    val characterKey: String? = null,
    val primaryCategoryId: String? = null,
)

@Serializable
data class CatalogAdminOperationRequest(
    val type: CatalogAdminOperationType,
    val payload: CatalogAdminOperationPayload,
)

@Serializable
data class CatalogAdminOperationResult(
    val categoryId: String? = null,
    val characterKey: String? = null,
    val revision: String? = null,
    val catalogVersion: String? = null,
    val generatedAt: String? = null,
)

@Serializable
data class CatalogAdminOperation(
    val schemaVersion: Int,
    val success: Boolean,
    val operationId: String,
    val type: CatalogAdminOperationType,
    val status: String,
    val message: String? = null,
    val result: CatalogAdminOperationResult? = null,
    val createdAt: String,
    val updatedAt: String,
)

interface CatalogAdminRepository {
    suspend fun verify(adminKey: String): ApiResult<CatalogAdminVerifyResponse>
    suspend fun createOperation(
        adminKey: String,
        request: CatalogAdminOperationRequest,
        idempotencyKey: String,
    ): ApiResult<CatalogAdminOperation>
    suspend fun getOperation(
        adminKey: String,
        operationId: String,
    ): ApiResult<CatalogAdminOperation>
}

class DefaultCatalogAdminRepository(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://api.qyaichat.com",
) : CatalogAdminRepository {
    override suspend fun verify(adminKey: String): ApiResult<CatalogAdminVerifyResponse> = request {
        httpClient.post("$baseUrl/v1/admin/verify") {
            attributes.put(SuppressNetworkLogging, true)
            header(HttpHeaders.Authorization, "Bearer $adminKey")
        }.body()
    }

    override suspend fun createOperation(
        adminKey: String,
        request: CatalogAdminOperationRequest,
        idempotencyKey: String,
    ): ApiResult<CatalogAdminOperation> = request {
        httpClient.post("$baseUrl/v1/admin/operations") {
            attributes.put(SuppressNetworkLogging, true)
            header(HttpHeaders.Authorization, "Bearer $adminKey")
            header("Idempotency-Key", idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getOperation(
        adminKey: String,
        operationId: String,
    ): ApiResult<CatalogAdminOperation> = request {
        httpClient.get("$baseUrl/v1/admin/operations/$operationId") {
            attributes.put(SuppressNetworkLogging, true)
            header(HttpHeaders.Authorization, "Bearer $adminKey")
        }.body()
    }

    private suspend fun <T> request(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: io.ktor.client.plugins.ClientRequestException) {
            ApiResult.HttpError(error.response.status.value, error.message)
        } catch (error: io.ktor.client.plugins.ServerResponseException) {
            ApiResult.HttpError(error.response.status.value, error.message)
        } catch (error: Throwable) {
            ApiResult.NetworkError(error.message)
        }
    }
}
