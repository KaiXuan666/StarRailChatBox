package com.kaixuan.starrailchatbox.data.ai.image

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.ImageGenerationModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfig
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

class ImageGenerationProviderTest {
    @Test
    fun openAiProviderParsesUrlResponse() = runTest {
        val client = testClient(
            MockEngine { request ->
                assertEquals("/v1/images/generations", request.url.encodedPath)
                assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
                respond(
                    """{"data":[{"url":"https://example.com/generated.webp"}]}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        val result = OpenAiCompatibleImageProvider(client).generate(
            config(ImageGenerationProviderIds.OpenAiCompatible),
            ImageGenerationRequest("a city at night", "16:9"),
        )

        val output = assertIs<ImageGenerationOutput.Url>(assertIs<ApiResult.Success<*>>(result).value)
        assertEquals("https://example.com/generated.webp", output.url)
        client.close()
    }

    @Test
    fun openAiProviderParsesBase64Response() = runTest {
        val client = testClient(
            MockEngine {
                respond(
                    """{"data":[{"b64_json":"AQID"}]}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        val result = OpenAiCompatibleImageProvider(client).generate(
            config(ImageGenerationProviderIds.OpenAiCompatible),
            ImageGenerationRequest("a city at night", "1:1"),
        )

        val output = assertIs<ImageGenerationOutput.Base64>(assertIs<ApiResult.Success<*>>(result).value)
        assertEquals("AQID", output.data)
        client.close()
    }

    @Test
    fun aliProviderUsesCompatibleModeForModelsAndNativeEndpointForGeneration() = runTest {
        var requestedModels = false
        var requestedGeneration = false
        val client = testClient(
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/compatible-mode/v1/models" -> {
                        requestedModels = true
                        respond(
                            """{"data":[{"id":"z-image-turbo"}]}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                    "/api/v1/services/aigc/multimodal-generation/generation" -> {
                        requestedGeneration = true
                        assertEquals("test-key", request.headers["api-key"])
                        respond(
                            """{"output":{"choices":[{"message":{"content":[{"image":"https://example.com/a.png"}]}}]}}""",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                    else -> error("Unexpected request: ${request.url}")
                }
            },
        )
        val provider = AliImageProvider(client)

        val models = provider.getModels("https://ignored.example/v1", "test-key")
        val generated = provider.generate(
            config(ImageGenerationProviderIds.Ali),
            ImageGenerationRequest("a snowy forest", "3:4"),
        )

        assertEquals(listOf("z-image-turbo"), assertIs<ApiResult.Success<List<String>>>(models).value)
        assertIs<ImageGenerationOutput.Url>(assertIs<ApiResult.Success<*>>(generated).value)
        assertTrue(requestedModels)
        assertTrue(requestedGeneration)
        client.close()
    }

    @Test
    fun registryDoesNotFallbackForUnknownProvider() {
        val registry = ImageGenerationProviderRegistry(emptyList())
        assertEquals(null, registry.find("unknown"))
    }

    private fun config(provider: String) = ModelConfig(
        id = ImageGenerationModelConfig.Id,
        provider = provider,
        name = ImageGenerationModelConfig.Name,
        baseUrl = "https://api.example.com/v1",
        apiKey = "test-key",
        modelName = "image-model",
        contextWindow = 1_000,
        maxOutputTokens = 100,
        supportVision = false,
        supportToolCall = false,
        supportReasoning = false,
        temperature = 0.7,
        topP = 1.0,
        enabled = true,
    )

    private fun testClient(engine: MockEngine) = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}
