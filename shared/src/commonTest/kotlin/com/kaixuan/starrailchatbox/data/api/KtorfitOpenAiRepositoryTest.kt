package com.kaixuan.starrailchatbox.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.kaixuan.starrailchatbox.data.model.ModelConfig

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

    @Test
    fun createChatCompletionSendsConfiguredRequestAndParsesUsage() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v1/chat/completions", request.url.encodedPath)
            assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
            val body = when (val content = request.body) {
                is TextContent -> content.text
                is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
                else -> error("Unexpected request body: ${content::class}")
            }
            assertTrue(body.contains("\"model\":\"test-model\""))
            assertTrue(body.contains("\"temperature\":0.7"))
            assertTrue(body.contains("\"top_p\":0.9"))
            assertTrue(body.contains("\"max_tokens\":2048"))
            assertTrue(body.contains("\"role\":\"system\""))

            respond(
                content = """
                    {
                      "choices": [{
                        "message": {"role": "assistant", "content": "response"},
                        "finish_reason": "stop"
                      }],
                      "usage": {
                        "prompt_tokens": 10,
                        "completion_tokens": 3,
                        "total_tokens": 13
                      }
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

        val result = KtorfitOpenAiRepository(client).createChatCompletion(
            config = apiTestConfig(),
            messages = listOf(ChatMessage("system", "prompt")),
        )

        val success = result as? ApiResult.Success
            ?: error("Expected success, got $result")
        assertEquals("response", success.value.content)
        assertEquals(13, success.value.totalTokens)
        client.close()
    }
}

private fun apiTestConfig() = ModelConfig(
    id = "default",
    provider = "custom",
    name = "Test",
    baseUrl = "https://example.com/v1",
    apiKey = "test-key",
    modelName = "test-model",
    contextWindow = 128_000,
    maxOutputTokens = 2_048,
    supportVision = false,
    supportToolCall = false,
    supportReasoning = false,
    temperature = 0.7,
    topP = 0.9,
    enabled = true,
)
