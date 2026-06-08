package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.model.InMemoryModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.VoiceModelConfig
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

class VoiceSynthesisToolTest {

    private val context = ToolContext("流萤")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun isAvailableChecksConfigPresence() = runTest {
        val repo = InMemoryModelConfigRepository()
        val engine = MockEngine { respond("") }
        val client = testClient(engine)
        val tool = VoiceSynthesisTool(repo, client, coroutineScope = backgroundScope)

        // 执行初始化的获取逻辑
        runCurrent()
        assertFalse(tool.isAvailable())

        // 设定配置
        repo.saveVoice(
            ModelConfig(
                id = VoiceModelConfig.Id,
                provider = VoiceModelConfig.Provider,
                name = VoiceModelConfig.Name,
                baseUrl = "https://api.xiaomimimo.com/v1",
                apiKey = "test-key",
                modelName = "mimo-v2.5-tts-voicedesign",
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

        // 等待轮询周期刷新 (虚拟时间)
        delay(5100)
        assertTrue(tool.isAvailable())

        client.close()
    }

    @Test
    fun executesSynthesizeVoiceSuccessfully() = runTest {
        val repo = InMemoryModelConfigRepository(
            initialVoice = ModelConfig(
                id = VoiceModelConfig.Id,
                provider = VoiceModelConfig.Provider,
                name = VoiceModelConfig.Name,
                baseUrl = "https://api.xiaomimimo.com/v1",
                apiKey = "test-key",
                modelName = "mimo-v2.5-tts-voicedesign",
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

        // 模拟 mimo API 的返回：包含 audio base64 数据
        // "test-audio-bytes" 对应的 base64 是 "dGVzdC1hdWRpby1ieXRlcw=="
        val engine = MockEngine { request ->
            assertEquals("/v1/chat/completions", request.url.encodedPath)
            assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
            assertEquals("test-key", request.headers["api-key"])
            respond(
                content = """
                    {
                      "choices": [{
                        "message": {
                          "role": "assistant",
                          "content": null,
                          "audio": {
                            "data": "dGVzdC1hdWRpby1ieXRlcw==",
                            "id": "audio-1"
                          }
                        }
                      }]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = testClient(engine)
        val tool = VoiceSynthesisTool(repo, client, coroutineScope = backgroundScope)

        val result = tool.execute(
            AiToolCall(
                id = "call-1",
                name = VoiceSynthesisTool.Name,
                arguments = """
                    {
                      "voice_design": "活泼可爱的少女",
                      "ai_response": "你好呀，很高兴见到你！"
                    }
                """.trimIndent()
            ),
            context
        )

        val terminal = assertIs<ToolResult.Terminal>(result)
        assertEquals("你好呀，很高兴见到你！", terminal.content)
        assertNotNull(terminal.voiceAttachmentUri)
        // test-audio-bytes 的长度为 16 字节，(16 / 32000) * 1000 = 0，被 maxOf(1000L) 限制为 1000 毫秒
        assertEquals(1000L, terminal.voiceDurationMs)

        client.close()
    }

    @Test
    fun fallbackInjectsPromptAndParsesMetadata() = runTest {
        val repo = InMemoryModelConfigRepository(
            initialVoice = ModelConfig(
                id = VoiceModelConfig.Id,
                provider = VoiceModelConfig.Provider,
                name = VoiceModelConfig.Name,
                baseUrl = "https://api.xiaomimimo.com/v1",
                apiKey = "test-key",
                modelName = "mimo-v2.5-tts-voicedesign",
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
                      "choices": [{
                        "message": {
                          "role": "assistant",
                          "content": null,
                          "audio": {
                            "data": "dGVzdC1hdWRpby1ieXRlcw==",
                            "id": "audio-1"
                          }
                        }
                      }]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = testClient(engine)
        val tool = VoiceSynthesisTool(repo, client, coroutineScope = backgroundScope)

        val messages = tool.prepareFallbackMessages(
            listOf(AiMessage("system", "保持人设"), AiMessage("user", "说话")),
            context
        )

        // 验证系统提示词和用户输入被增强
        assertTrue(messages.first().content.orEmpty().contains("<voice_output_contract>"))
        assertTrue(messages.last().content.orEmpty().contains("<control_signals>"))

        // 测试降级解析提取
        val parsed = tool.parseFallback(
            """
                我会一直陪着你的。
                <voice_synthesis>{"voice_design":"温柔的引路人","ai_response":"我会一直陪着你的。"}</voice_synthesis>
            """.trimIndent(),
            context
        )

        assertNotNull(parsed)
        val terminal = assertIs<ToolResult.Terminal>(parsed)
        assertEquals("我会一直陪着你的。", terminal.content)
        assertNotNull(terminal.voiceAttachmentUri)

        client.close()
    }

    private fun testClient(engine: MockEngine) = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}
