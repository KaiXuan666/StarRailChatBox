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
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class ImageGenerationToolTest {

    private val context = ToolContext("流萤")
    private val fakeFileSystem = FakeFileSystem()
    private val mockFileManager = object : KmpFileManager {
        override val appDataDir: Path = "/app_data".toPath()
        override val fileSystem: FileSystem = fakeFileSystem
        override suspend fun saveImageToGallery(bytes: ByteArray, name: String) {}
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun isAvailableChecksConfigPresence() = runTest {
        val repo = InMemoryModelConfigRepository()
        val engine = MockEngine { respond("") }
        val client = testClient(engine)
        val tool = ImageGenerationTool(repo, client, mockFileManager, coroutineScope = backgroundScope)

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
            // Check either the legacy path or the actual DashScope path used in code
            assertTrue(request.url.encodedPath.contains("/api/v1/services/aigc/multimodal-generation/generation") || 
                       request.url.encodedPath.contains("/api/v1/services/aigc/text2image/chat/completions"))
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
            // Mock image download response
            if (request.url.toString() == "https://example.com/image.png") {
                respond(
                    content = ByteArray(10),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "image/png")
                )
            } else {
                respond(
                    content = """{"output": {"choices": [{"message": {"role": "assistant", "content": [{"image": "https://example.com/image.png"}, {"text": "generated image prompt"}]}}]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val client = testClient(engine)
        val tool = ImageGenerationTool(repo, client, mockFileManager, coroutineScope = backgroundScope)

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
        assertEquals("喏，你要的图片。", terminal.content)
        assertTrue(terminal.imageAttachmentUri.orEmpty().contains("generated_images/gen_"))

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

        val engine = MockEngine { request ->
            if (request.url.toString() == "https://example.com/image.png") {
                respond(
                    content = ByteArray(10),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "image/png")
                )
            } else {
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
        }

        val client = testClient(engine)
        val tool = ImageGenerationTool(repo, client, mockFileManager, coroutineScope = backgroundScope)

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
        assertTrue(terminal.content.contains("好的，为你画好了。"))
        assertTrue(terminal.imageAttachmentUri.orEmpty().contains("generated_images/gen_"))

        client.close()
    }

    private fun testClient(engine: MockEngine) = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}
