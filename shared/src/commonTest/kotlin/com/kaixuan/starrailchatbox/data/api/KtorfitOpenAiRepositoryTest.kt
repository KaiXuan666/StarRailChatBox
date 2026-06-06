package com.kaixuan.starrailchatbox.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorfitOpenAiRepositoryTest {

    @Test
    fun getModelsUsesOpenAiCompatibleEndpointAndBearerToken() = runTest {
        val engine = MockEngine { request ->
            assertEquals("example.com", request.url.host)
            assertEquals("/v1/models", request.url.encodedPath)
            assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])

            respond(
                content = """
                    {
                      "object": "list",
                      "data": [
                        {"id": "model-b"},
                        {"id": "model-a"},
                        {"id": "model-a"}
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val result = KtorfitOpenAiRepository(client).getModels(
            apiHost = "https://example.com/v1/",
            apiKey = " test-key ",
        )

        val success = result as? ApiResult.Success
            ?: error("Expected success, got $result")
        assertEquals(listOf("model-a", "model-b"), success.value)
        client.close()
    }
}
