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

class PublicCharacterCatalogRepositoryTest {
    @Test
    fun catalogSupportsConditionalRequests() = runTest {
        val engine = MockEngine { request ->
            assertEquals("\"catalog-v1\"", request.headers[HttpHeaders.IfNoneMatch])
            respond("", HttpStatusCode.NotModified)
        }
        val repository = DefaultPublicCharacterCatalogRepository(testClient(engine), "https://cards.example")

        val result = repository.getCatalog("\"catalog-v1\"")

        val fetch = assertIs<ApiResult.Success<PublicCatalogFetch>>(result).value
        assertTrue(fetch.notModified)
        assertEquals("\"catalog-v1\"", fetch.etag)
    }

    @Test
    fun categoryAndDetailUrlsResolveAgainstConfiguredHost() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "schemaVersion": 1,
                      "catalogVersion": "v1",
                      "categories": []
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = DefaultPublicCharacterCatalogRepository(testClient(engine), "https://cards.example/")

        val result = repository.getCategories("/api/v1/releases/v1/categories.json")

        assertIs<ApiResult.Success<PublicCategories>>(result)
        assertEquals(
            "https://cards.example/api/v1/releases/v1/categories.json",
            engine.requestHistory.single().url.toString(),
        )
    }

    @Test
    fun newerSchemaReturnsUpgradeError() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "schemaVersion": 2,
                      "catalogVersion": "v2",
                      "generatedAt": "2026-06-14T00:00:00Z",
                      "categoriesUrl": "/categories.json",
                      "tags": []
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = DefaultPublicCharacterCatalogRepository(testClient(engine))

        val result = repository.getCatalog()

        assertEquals(
            "catalog_schema_unsupported",
            assertIs<ApiResult.UnexpectedError>(result).message,
        )
    }
}

private fun testClient(engine: MockEngine) = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
