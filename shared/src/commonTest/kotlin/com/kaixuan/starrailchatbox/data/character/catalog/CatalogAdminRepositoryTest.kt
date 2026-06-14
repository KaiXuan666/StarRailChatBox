package com.kaixuan.starrailchatbox.data.character.catalog

import com.kaixuan.starrailchatbox.data.api.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CatalogAdminRepositoryTest {
    @Test
    fun verifySendsBearerCredentialAndParsesResponse() = runTest {
        val engine = MockEngine { request ->
            respond(
                content = """{"schemaVersion":1,"success":true,"admin":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val repository = repository(engine)

        val result = repository.verify("private-key")

        assertTrue(assertIs<ApiResult.Success<CatalogAdminVerifyResponse>>(result).value.admin)
        assertEquals(
            "Bearer private-key",
            engine.requestHistory.single().headers[HttpHeaders.Authorization],
        )
        assertEquals("/v1/admin/verify", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun createOperationSendsIdempotencyKeyAndParsesPendingReview() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "schemaVersion":1,
                      "success":true,
                      "operationId":"op_0123456789abcdef0123456789abcdef",
                      "type":"DELETE_CHARACTER",
                      "status":"PENDING_REVIEW",
                      "createdAt":"2026-06-14T00:00:00Z",
                      "updatedAt":"2026-06-14T00:00:00Z"
                    }
                """.trimIndent(),
                headers = jsonHeaders(),
            )
        }
        val repository = repository(engine)

        val result = repository.createOperation(
            adminKey = "private-key",
            request = CatalogAdminOperationRequest(
                type = CatalogAdminOperationType.DeleteCharacter,
                payload = CatalogAdminOperationPayload(characterKey = "a".repeat(64)),
            ),
            idempotencyKey = "delete-test",
        )

        assertEquals(
            "PENDING_REVIEW",
            assertIs<ApiResult.Success<CatalogAdminOperation>>(result).value.status,
        )
        assertEquals("delete-test", engine.requestHistory.single().headers["Idempotency-Key"])
    }

    private fun repository(engine: MockEngine): CatalogAdminRepository {
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return DefaultCatalogAdminRepository(client, "https://api.example.test")
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")
}
