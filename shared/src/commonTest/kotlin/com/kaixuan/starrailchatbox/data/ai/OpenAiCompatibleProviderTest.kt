package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.api.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenAiCompatibleProviderTest {
    @Test
    fun mapsRequestAndResponse() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v1/chat/completions", request.url.encodedPath)
            assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
            val body = request.body.readText()
            assertTrue(body.contains("\"tool_choice\":\"required\""))
            assertTrue(body.contains("\"name\":\"lookup\""))
            assertTrue(body.contains("\"additionalProperties\":false"))

            respond(
                content = """
                    {
                      "choices": [{
                        "message": {
                          "role": "assistant",
                          "content": null,
                          "tool_calls": [{
                            "id": "call-1",
                            "type": "function",
                            "function": {"name": "lookup", "arguments": "{\"query\":\"x\"}"}
                          }]
                        },
                        "finish_reason": "tool_calls"
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
        val client = testClient(engine)
        val provider = OpenAiCompatibleProvider(client)

        val result = provider.complete(
            providerConfig(),
            AiChatRequest(
                model = "test-model",
                messages = listOf(AiMessage("user", "hello")),
                tools = listOf(
                    AiToolDefinition(
                        name = "lookup",
                        description = "Lookup",
                        parameters = buildJsonObject {
                            put("type", "object")
                            put("additionalProperties", false)
                        },
                    ),
                ),
                toolChoice = ToolChoice.Required,
            ),
        )

        val success = assertIs<ApiResult.Success<AiCompletion>>(result)
        assertEquals("lookup", success.value.message.toolCalls.single().name)
        assertEquals(13, success.value.usage.totalTokens)
        client.close()
    }

    @Test
    fun mapsSpecificToolChoice() = runTest {
        val engine = MockEngine { request ->
            val body = request.body.readText()
            assertTrue(
                body.contains(
                    "\"tool_choice\":{\"type\":\"function\",\"function\":{\"name\":\"lookup\"}}",
                ),
            )
            respond(
                content = """{"choices":[{"message":{"role":"assistant","content":"done"}}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = testClient(engine)

        OpenAiCompatibleProvider(client).complete(
            providerConfig(),
            AiChatRequest(
                model = "test-model",
                messages = listOf(AiMessage("user", "hello")),
                toolChoice = ToolChoice.Specific("lookup"),
            ),
        )

        client.close()
    }

    @Test
    fun toolSupportRequiresActualToolCall() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "choices": [{
                        "message": {
                          "role": "assistant",
                          "content": "2",
                          "tool_calls": null
                        },
                        "finish_reason": "stop"
                      }]
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = testClient(engine)

        assertEquals(
            false,
            OpenAiCompatibleProvider(client).supportsToolCalls(providerConfig()),
        )
        client.close()
    }

    @Test
    fun getModelsUsesCompatibleEndpoint() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v1/models", request.url.encodedPath)
            respond(
                content = """{"data":[{"id":"b"},{"id":"a"},{"id":"a"}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = testClient(engine)

        val result = OpenAiCompatibleProvider(client).getModels(providerConfig())

        assertEquals(
            listOf("a", "b"),
            assertIs<ApiResult.Success<List<String>>>(result).value,
        )
        client.close()
    }
}

private fun providerConfig() = AiProviderConfig(
    providerId = OpenAiCompatibleProvider.Id,
    apiHost = "https://example.com/v1",
    apiKey = "test-key",
    model = "test-model",
)

private fun testClient(engine: MockEngine) = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

private suspend fun OutgoingContent.readText(): String {
    return when (this) {
        is TextContent -> text
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        is OutgoingContent.WriteChannelContent -> {
            val channel = io.ktor.utils.io.ByteChannel(true)
            writeTo(channel)
            channel.close()
            channel.readRemaining().readText()
        }
        else -> ""
    }
}
