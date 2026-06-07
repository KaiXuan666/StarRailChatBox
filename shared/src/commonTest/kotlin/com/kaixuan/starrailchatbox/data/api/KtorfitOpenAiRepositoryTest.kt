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
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readText

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
            val body = request.body.readText()
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
            characterName = "流萤",
        )

        val success = result as? ApiResult.Success
            ?: error("Expected success, got $result")
        assertEquals("response", success.value.content)
        assertEquals(13, success.value.totalTokens)
        client.close()
    }

    @Test
    fun createChatCompletionWithToolCallParsesSuggestions() = runTest {
        val engine = MockEngine { request ->
            val body = request.body.readText()
            assertTrue(body.contains("\"tools\":["))
            assertTrue(body.contains("\"tool_choice\":\"required\""))

            respond(
                content = """
                    {
                      "choices": [{
                        "message": {
                          "role": "assistant",
                          "content": null,
                          "tool_calls": [{
                            "id": "call_1",
                            "type": "function",
                            "function": {
                              "name": "respond_with_quick_replies",
                              "arguments": "{\n  \"ai_response\": \"你好，我一直都在这里。\",\n  \"suggestions\": [\n    \"🌸 陪我坐一会儿\",\n    \"🍃 只是想吹吹风\",\n    \"✨ 听讲个故事\"\n  ]\n}"
                            }
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
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val result = KtorfitOpenAiRepository(client).createChatCompletion(
            config = apiTestConfig().copy(supportToolCall = true),
            messages = listOf(ChatMessage("system", "prompt")),
            characterName = "流萤",
        )

        val success = result as? ApiResult.Success
            ?: error("Expected success, got $result")
        assertEquals("你好，我一直都在这里。", success.value.content)
        assertEquals(listOf("🌸 陪我坐一会儿", "🍃 只是想吹吹风", "✨ 听讲个故事"), success.value.suggestions)
        client.close()
    }

    @Test
    fun testToolCallSupportRequiresAnActualToolCall() = runTest {
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount += 1
            val body = request.body.readText()
            assertTrue(body.contains("\"tool_choice\":\"required\""))
            assertTrue(body.contains("\"name\":\"test_tool_call\""))

            respond(
                content = if (requestCount == 1) {
                    """
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
                    """.trimIndent()
                } else {
                    """
                        {
                          "choices": [{
                            "message": {
                              "role": "assistant",
                              "content": null,
                              "tool_calls": [{
                                "id": "call_test",
                                "type": "function",
                                "function": {
                                  "name": "test_tool_call",
                                  "arguments": "{\"answer\":\"2\"}"
                                }
                              }]
                            },
                            "finish_reason": "tool_calls"
                          }]
                        }
                    """.trimIndent()
                },
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
        val repository = KtorfitOpenAiRepository(client)

        assertEquals(
            false,
            repository.testToolCallSupport("https://example.com/v1", "test-key", "test-model"),
        )
        assertEquals(
            true,
            repository.testToolCallSupport("https://example.com/v1", "test-key", "test-model"),
        )
        client.close()
    }

    @Test
    fun createChatCompletionWithoutToolCallParsesSuggestionsFromTags() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """
                    {
                      "choices": [{
                        "message": {
                          "role": "assistant",
                          "content": "你好，我一直都在这里。\n<suggestions>\n🌸 陪我坐一会儿\n🍃 只是想吹吹风\n✨ 听讲个故事\n</suggestions>"
                        },
                        "finish_reason": "stop"
                      }]
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
            config = apiTestConfig().copy(supportToolCall = false),
            messages = listOf(ChatMessage("system", "prompt")),
            characterName = "流萤",
        )

        val success = result as? ApiResult.Success
            ?: error("Expected success, got $result")
        assertEquals("你好，我一直都在这里。", success.value.content)
        assertEquals(listOf("🌸 陪我坐一会儿", "🍃 只是想吹吹风", "✨ 听讲个故事"), success.value.suggestions)
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
