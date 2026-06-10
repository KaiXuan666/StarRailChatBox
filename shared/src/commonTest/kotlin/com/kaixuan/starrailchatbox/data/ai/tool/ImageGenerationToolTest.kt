package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.model.InMemoryModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.ImageGenerationModelConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageGenerationToolTest {

    private val context = ToolContext("流萤")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun isAvailableChecksConfigPresence() = runTest {
        val repo = InMemoryModelConfigRepository()
        val engine = MockEngine { respond("") }
        val client = testClient(engine)
        val tool = ImageGenerationTool(repo, client, coroutineScope = backgroundScope)

        runCurrent()
        assertFalse(tool.isAvailable())

        repo.saveImageGeneration(
            ModelConfig(
                id = ImageGenerationModelConfig.Id,
                provider = ImageGenerationModelConfig.Provider,
                name = ImageGenerationModelConfig.Name,
                baseUrl = "https://api.dashscope.aliyuncs.com/api/v1/services/aigc/text2image",
                apiKey = "test-key",
                modelName = "z-image-turbo",
                contextWindow = 1000,
                maxOutputTokens = 100,
                supportVision = false,
                supportToolCall = false,
                supportReasoning = false,
                temperature = 0.7,
                topP = 1.0,
                enabled = true
            )
        )

        delay(10100)
        assertTrue(tool.isAvailable())

        client.close()
    }

    @Test
    fun executesImageGenerationSuccessfully() = runTest {
        val repo = InMemoryModelConfigRepository(
            initial = null // use the one saved below
        )
        repo.saveImageGeneration(
            ModelConfig(
                id = ImageGenerationModelConfig.Id,
                provider = ImageGenerationModelConfig.Provider,
                name = ImageGenerationModelConfig.Name,
                baseUrl = "https://api.dashscope.aliyuncs.com/api/v1/services/aigc/text2image",
                apiKey = "test-key",
                modelName = "z-image-turbo",
                contextWindow = 1000,
                maxOutputTokens = 100,
                supportVision = false,
                supportToolCall = false,
                supportReasoning = false,
                temperature = 0.7,
                topP = 1.0,
                enabled = true
            )
        )

        val engine = MockEngine { request ->
            assertEquals("/api/v1/services/aigc/text2image/chat/completions", request.url.encodedPath)
            assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
            respond(
                content = """
                    {
                      "output": {
                        "choices": [{
                          "message": {
                            "role": "assistant",
                            "content": [
                              { "image": "https://example.com/image.png" },
                              { "text": "generated image prompt" }
                            ]
                          }
                        }]
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = testClient(engine)
        val tool = ImageGenerationTool(repo, client, coroutineScope = backgroundScope)

        val result = tool.execute(
            AiToolCall(
                id = "call-1",
                name = ImageGenerationTool.NAME,
                arguments = """
                    {
                      "prompt": "a cute cat in the snow",
                      "aspect_ratio": "16:9"
                    }
                """.trimIndent()
            ),
            context
        )

        val terminal = assertIs<ToolResult.Terminal>(result)
        assertEquals("图片已生成。", terminal.content)
        assertEquals("https://example.com/image.png", terminal.imageAttachmentUri)

        client.close()
    }

    @Test
    fun fallbackInjectsPromptAndParsesMetadata() = runTest {
        val repo = InMemoryModelConfigRepository()
        repo.saveImageGeneration(
             ModelConfig(
                id = ImageGenerationModelConfig.Id,
                provider = ImageGenerationModelConfig.Provider,
                name = ImageGenerationModelConfig.Name,
                baseUrl = "https://api.dashscope.aliyuncs.com/api/v1/services/aigc/text2image",
                apiKey = "test-key",
                modelName = "z-image-turbo",
                contextWindow = 1000,
                maxOutputTokens = 100,
                supportVision = false,
                supportToolCall = false,
                supportReasoning = false,
                temperature = 0.7,
                topP = 1.0,
                enabled = true
            )
        )

        val engine = MockEngine {
            respond(
                content = """
                    {
                      "output": {
                        "choices": [{
                          "message": {
                            "role": "assistant",
                            "content": [
                              { "image": "https://example.com/image.png" }
                            ]
                          }
                        }]
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = testClient(engine)
        val tool = ImageGenerationTool(repo, client, coroutineScope = backgroundScope)

        val messages = tool.prepareFallbackMessages(
            listOf(AiMessage("system", "保持人设"), AiMessage("user", "画个图")),
            context
        )

        assertTrue(messages.first().content.orEmpty().contains("<image_generation_contract>"))
        assertTrue(messages.last().content.orEmpty().contains("<control_signals>"))

        val parsed = tool.parseFallback(
            """
                好的，为你画好了。
                <image_generation>{"prompt":"a snowy forest","aspect_ratio":"1:1"}</image_generation>
            """.trimIndent(),
            context
        )

        assertNotNull(parsed)
        val terminal = assertIs<ToolResult.Terminal>(parsed)
        assertEquals("好的，为你画好了。\n\n图片已生成。", terminal.content)
        assertEquals("https://example.com/image.png", terminal.imageAttachmentUri)

        client.close()
    }

    private fun testClient(engine: MockEngine) = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}
