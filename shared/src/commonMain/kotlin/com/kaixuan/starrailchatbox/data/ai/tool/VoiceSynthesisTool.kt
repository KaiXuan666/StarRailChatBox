package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.ai.AiToolDefinition
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.platform.readUriAsBytes
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * 语音合成工具。
 * 对接小米 MiMo TTS 音色设计 API，实现支持工具调用模型和不支持工具调用模型下的语音自动生成。
 */
class VoiceSynthesisTool(
    private val modelConfigRepository: ModelConfigRepository,
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
    coroutineScope: CoroutineScope? = null,
) : AiTool {
    override val name: String = Name
    override val executionType: ToolExecutionType = ToolExecutionType.TerminalOutput
    override val risk: ToolRisk = ToolRisk.ReadOnly

    private val activeScope = coroutineScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cachedIsAvailable: Boolean = false

    init {
        activeScope.launch {
            while (isActive) {
                try {
                    val voiceConfig = modelConfigRepository.getVoice()
                    val voiceCloneConfig = modelConfigRepository.getVoiceClone()
                    cachedIsAvailable = (voiceConfig != null && voiceConfig.baseUrl.isNotBlank() && voiceConfig.apiKey.isNotBlank()) ||
                            (voiceCloneConfig != null && voiceCloneConfig.baseUrl.isNotBlank() && voiceCloneConfig.apiKey.isNotBlank())
                } catch (t: Throwable) {
                    cachedIsAvailable = false
                }
                delay(5000)
            }
        }
    }

    override fun isAvailable(): Boolean = cachedIsAvailable


    override fun definition(context: ToolContext): AiToolDefinition {
        return AiToolDefinition(
            name = name,
            description = "当用户要求你说话、发语音、听声音，或用户自身发送了语音消息（或者任何你想对用户表达声音/配音的场景）时，必须触发该工具。该工具将指定的文本及音色设计合成配音。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("voice_design") {
                        put("type", "string")
                        put("description", "描述合成音色以及朗读风格的Prompt，包括角色的人设（如开朗少女、冷酷少年）、说话风格（如温柔地、兴奋地）、场景描写（如安静的茶馆、嘈杂的集市）等。")
                    }
                    putJsonObject("ai_response") {
                        put("type", "string")
                        put("description", "待合成配音的文本回复内容。内容应当直接是角色要说的话，不要包含任何旁白、神态描写。")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("voice_design"))
                    add(JsonPrimitive("ai_response"))
                }
                put("additionalProperties", false)
            }
        )
    }

    override suspend fun execute(call: AiToolCall, context: ToolContext): ToolResult {
        return try {
            val arguments = json.parseToJsonElement(call.arguments).jsonObject
            val voiceDesign = arguments["voice_design"]?.jsonPrimitive?.content.orEmpty()
            val aiResponse = arguments["ai_response"]?.jsonPrimitive?.content.orEmpty()
            
            synthesizeVoice(voiceDesign, aiResponse, context)
        } catch (t: Throwable) {
            t.printStackTrace()
            ToolResult.Error("invalid_tool_arguments", "Failed to parse arguments: ${t.message}")
        }
    }

    override fun prepareFallbackMessages(
        messages: List<AiMessage>,
        context: ToolContext,
    ): List<AiMessage> {
        val format = """
            <voice_output_contract>
            如果用户要求你说话、发语音、听声音，或用户自身发送了语音消息（或者任何你想对用户表达声音/配音的场景），你必须在回复的最后另起一行，输出且只输出一个语音合成元数据块，指定你要说的话（正文）以及你想呈现的配音音色与语境设计。
            格式为：
            <voice_synthesis>{"voice_design":"角色的人设（如开朗少女）、说话风格（如温柔地）和场景描写（如安静的房间）","ai_response":"待合成配音的纯文本回复内容，内容直接是你要说的话，不要有任何旁白或动作描写"}</voice_synthesis>

            强制规则：
            - 元数据块必须是整条回复的最后一部分，不能省略、改名或放进 Markdown 代码块。
            - 标签内部必须是合法 JSON；只能包含 voice_design 和 ai_response 字段。
            - ai_response 内只包含你要说的话，不要有括号神态或旁白，正文部分和元数据块内的 ai_response 保持一致。
            - 正文中不要提及语音合成、格式要求、JSON 或标签本身。
            
            正确示例：
            既然你这么想听，那我就说给你听吧。
            <voice_synthesis>{"voice_design":"傲娇的少女，略带害羞地，在安静的走廊上","ai_response":"既然你这么想听，那我就说给你听吧"}</voice_synthesis>
            </voice_output_contract>
        """.trimIndent()

        return messages.injectFallbackInstructions(
            systemFormat = format,
            controlSignal = "如果本次回复需要发声/发语音，请遵守 system 消息中的 <voice_output_contract>，并以完整的 <voice_synthesis> JSON 元数据块结束回复。"
        )
    }

    override suspend fun parseFallback(
        content: String,
        context: ToolContext,
    ): ToolResult.Terminal? {
        val match = VoiceSynthesisRegex.find(content) ?: return null
        val payloadStr = match.groupValues[1].trim()
        val plainText = content.removeRange(match.range).trim()

        return try {
            val payload = json.parseToJsonElement(payloadStr).jsonObject
            val voiceDesign = payload["voice_design"]?.jsonPrimitive?.content.orEmpty()
            val aiResponse = payload["ai_response"]?.jsonPrimitive?.content.orEmpty()

            if (aiResponse.isBlank()) {
                ToolResult.Terminal(content = plainText)
            } else {
                synthesizeVoice(voiceDesign, aiResponse, context)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            ToolResult.Terminal(content = plainText)
        }
    }

    private suspend fun synthesizeVoice(
        voiceDesign: String,
        aiResponse: String,
        context: ToolContext
    ): ToolResult.Terminal {
        val voiceCloneConfig = modelConfigRepository.getVoiceClone()
        val voiceSampleUri = context.voiceSampleUri

        Napier.d { "synthesizeVoice voiceSampleUri=$voiceSampleUri, voiceCloneConfig=$voiceCloneConfig" }

        val (voiceConfig, isClone) = if (voiceCloneConfig != null && !voiceSampleUri.isNullOrBlank()) {
            voiceCloneConfig to true
        } else {
            (modelConfigRepository.getVoice() ?: return ToolResult.Terminal(content = aiResponse)) to false
        }

        if (voiceConfig.baseUrl.isBlank() || voiceConfig.apiKey.isBlank()) {
            return ToolResult.Terminal(content = aiResponse)
        }

        return try {
            val requestBody = if (isClone && !voiceSampleUri.isNullOrBlank()) {
                val audioBytes = readUriAsBytes(voiceSampleUri)
                @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
                val audioBase64 = kotlin.io.encoding.Base64.Default.encode(audioBytes)
                val mimeType = when {
                    voiceSampleUri.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                    voiceSampleUri.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
                    voiceSampleUri.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
                    voiceSampleUri.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
                    else -> "audio/wav" // 默认 wav
                }

                buildJsonObject {
                    put("model", voiceConfig.modelName.takeIf(String::isNotBlank) ?: "mimo-v2.5-tts-voiceclone")
                    putJsonArray("messages") {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", "")
                        })
                        add(buildJsonObject {
                            put("role", "assistant")
                            put("content", aiResponse)
                        })
                    }
                    putJsonObject("audio") {
                        put("format", "wav")
                        put("voice", "data:$mimeType;base64,$audioBase64")
                    }
                }
            } else {
                buildJsonObject {
                    put("model", voiceConfig.modelName.takeIf(String::isNotBlank) ?: "mimo-v2.5-tts-voicedesign")
                    putJsonArray("messages") {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", voiceDesign)
                        })
                        add(buildJsonObject {
                            put("role", "assistant")
                            put("content", aiResponse)
                        })
                    }
                }
            }

            val response = httpClient.post("${voiceConfig.baseUrl.trimEnd('/')}/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer ${voiceConfig.apiKey.trim()}")
                header("api-key", voiceConfig.apiKey.trim())
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            val jsonObject = json.parseToJsonElement(responseText).jsonObject
            val base64Data = jsonObject["choices"]?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("audio")?.jsonObject
                ?.get("data")?.jsonPrimitive?.content

            if (!base64Data.isNullOrBlank()) {
                @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
                val audioBytes = kotlin.io.encoding.Base64.Default.decode(base64Data.trim())
                val randomFileName = "tts_${kotlin.random.Random.nextInt(10000000)}.wav"
                val uri = com.kaixuan.starrailchatbox.platform.writeAudioBytesToCache(audioBytes, randomFileName)
                val durationMs = minOf(30000L, maxOf(1000L, (audioBytes.size.toLong() / 32000L) * 1000L))
                ToolResult.Terminal(
                    content = aiResponse,
                    voiceAttachmentUri = uri,
                    voiceDurationMs = durationMs
                )
            } else {
                ToolResult.Terminal(content = aiResponse)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            ToolResult.Terminal(content = aiResponse)
        }
    }

    companion object {
        const val Name = "synthesize_voice"
        private val VoiceSynthesisRegex = Regex(
            pattern = "<voice_synthesis\\s*>([\\s\\S]*?)</voice_synthesis\\s*>",
            option = RegexOption.IGNORE_CASE,
        )
    }
}
