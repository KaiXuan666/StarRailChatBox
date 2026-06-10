package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.ai.AiToolDefinition
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random

/**
 * 图片生成工具。
 * 对接阿里云 DashScope 或兼容 API，实现根据描述词生成图片。
 */
class ImageGenerationTool(
    private val modelConfigRepository: ModelConfigRepository,
    private val httpClient: HttpClient,
    private val fileManager: KmpFileManager,
    private val json: Json = Json { ignoreUnknownKeys = true },
    coroutineScope: CoroutineScope? = null,
) : AiTool {
    override val name: String = NAME
    override val executionType: ToolExecutionType = ToolExecutionType.TerminalOutput
    override val risk: ToolRisk = ToolRisk.ReadOnly

    private val activeScope = coroutineScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cachedIsAvailable: Boolean = false

    init {
        activeScope.launch {
            while (isActive) {
                try {
                    val config = modelConfigRepository.getImageGeneration()
                    cachedIsAvailable = config != null && config.baseUrl.isNotBlank() && config.apiKey.isNotBlank()
                } catch (_: Throwable) {
                    cachedIsAvailable = false
                }
                delay(10000)
            }
        }
    }

    override fun isAvailable(): Boolean = cachedIsAvailable

    override fun definition(context: ToolContext): AiToolDefinition {
        return AiToolDefinition(
            name = name,
            description = "当用户要求你画画、生成图片、展示图像或描述某个视觉场景时，触发该工具。该工具根据用户的描述生成对应的图片。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("prompt") {
                        put("type", "string")
                        put("description", "详细的图片生成描述词。应当包含主体、环境、风格、光影等细节，推荐使用英文。")
                    }
                    putJsonObject("aspect_ratio") {
                        put("type", "string")
                        put("description", "图片的纵横比。可选值: 1:1, 4:3, 3:4, 16:9, 9:16。默认为 1:1。")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("prompt"))
                }
                put("additionalProperties", false)
            }
        )
    }

    override suspend fun execute(call: AiToolCall, context: ToolContext): ToolResult {
        return try {
            val arguments = json.parseToJsonElement(call.arguments).jsonObject
            val prompt = arguments["prompt"]?.jsonPrimitive?.content.orEmpty()
            val aspectRatio = arguments["aspect_ratio"]?.jsonPrimitive?.content ?: "1:1"

            generateImage(prompt, aspectRatio)
        } catch (t: Throwable) {
            t.printStackTrace()
            ToolResult.Error("invalid_tool_arguments", "Failed to parse arguments: ${t.message}")
        }
    }

    override fun prepareFallbackMessages(
        messages: List<AiMessage>,
        context: ToolContext,
    ): List<AiMessage> {
        Napier.d { "ImageGenerationTool prepareFallbackMessages" }
        val format = """
            <image_generation_contract>
            图片生成元数据块格式：
            <image_generation>{"prompt":"详细的图片生成描述词","aspect_ratio":"1:1"}</image_generation>

            特定规则：
            - 如果用户要求你画图、展示图片，必须在回复末尾附带此元数据块。
            - aspect_ratio 可选 1:1, 4:3, 3:4, 16:9, 9:16。
            </image_generation_contract>
        """.trimIndent()

        return messages.injectFallbackInstructions(
            systemFormat = format,
            controlSignal = "如果需要绘图，请遵守 <image_generation_contract>，并以完整的 <image_generation> 元数据块结束回复。"
        )
    }

    override suspend fun parseFallback(
        content: String,
        context: ToolContext,
    ): ToolResult.Terminal? {
        val match = ImageGenerationRegex.find(content) ?: return null
        val payloadStr = match.groupValues[1].trim()
        val plainText = content.removeRange(match.range).trim()

        return try {
            val payload = json.parseToJsonElement(payloadStr).jsonObject
            val prompt = payload["prompt"]?.jsonPrimitive?.content.orEmpty()
            val aspectRatio = payload["aspect_ratio"]?.jsonPrimitive?.content ?: "1:1"

            if (prompt.isBlank()) {
                ToolResult.Terminal(content = plainText)
            } else {
                val result = generateImage(prompt, aspectRatio)
                if (result is ToolResult.Terminal) {
                    result.copy(content = if (plainText.isEmpty()) result.content else "$plainText\n\n${result.content}")
                } else {
                    ToolResult.Terminal(content = plainText)
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            ToolResult.Terminal(content = plainText)
        }
    }

    private suspend fun generateImage(
        prompt: String,
        aspectRatio: String
    ): ToolResult {
        val config = modelConfigRepository.getImageGeneration() ?: return ToolResult.Error("no_config", "Image generation config not found")
        if (config.baseUrl.isBlank() || config.apiKey.isBlank()) {
            return ToolResult.Error("invalid_config", "Image generation config is incomplete")
        }

        return try {
            val size = when (aspectRatio) {
                "4:3" -> "1280*960"
                "3:4" -> "960*1280"
                "16:9" -> "1440*810"
                "9:16" -> "810*1440"
                else -> "1024*1024"
            }

            val requestBody = buildJsonObject {
                put("model", config.modelName.takeIf(String::isNotBlank) ?: "z-image-turbo")
                putJsonObject("input") {
                    putJsonArray("messages") {
                        add(buildJsonObject {
                            put("role", "user")
                            putJsonArray("content") {
                                add(buildJsonObject {
                                    put("text", prompt)
                                })
                            }
                        })
                    }
                }
                putJsonObject("parameters") {
                    put("size", size)
                    put("prompt_extend", false)
                }
            }
//https://dashscope.aliyuncs.com/compatible-mode/v1
            //https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
            Napier.d { "config.baseUrl=${config.baseUrl}" }
//            val response = httpClient.post("${config.baseUrl.trimEnd('/')}/images/generations") {
            val response = httpClient.post("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation") {
                header(HttpHeaders.Authorization, "Bearer ${config.apiKey.trim()}")
                header("api-key", config.apiKey.trim())
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            val jsonObject = json.parseToJsonElement(responseText).jsonObject
            
            // 解析 DashScope 格式: output.choices[0].message.content[0].image
            val imageUrl = jsonObject["output"]?.jsonObject
                ?.get("choices")?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("image")?.jsonPrimitive?.content

            if (!imageUrl.isNullOrBlank()) {
                // 下载并保存图片到本地私有目录
                val imageBytes = httpClient.get(imageUrl).body<ByteArray>()
                val randomSuffix = (Random.nextLong() and 0x7FFFFFFFFFFFFFFF).toString(36)
                
                // 优先从 URL 提取后缀名
                val extension = imageUrl.substringAfterLast('.', "png").let { ext ->
                    if (ext.contains('/') || ext.length > 5) "png" else ext
                }
                
                val fileName = "gen_$randomSuffix.$extension"
                val relativePath = "generated_images/$fileName"
                fileManager.writeBytes(relativePath, imageBytes)
                
                val localPath = (fileManager.appDataDir / relativePath).toString()
                Napier.d { "Image saved to: $localPath" }

                ToolResult.Terminal(
                    content = "图片已生成并保存到本地。",
                    imageAttachmentUri = localPath
                )
            } else {
                ToolResult.Error("generation_failed", "Failed to extract image URL from response: $responseText")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            ToolResult.Error("execution_error", "Error during image generation: ${t.message}")
        }
    }

    companion object {
        const val NAME = "generate_image"
        private val ImageGenerationRegex = Regex(
            pattern = "<image_generation\\s*>([\\s\\S]*?)(?:</image_generation\\s*>|$)",
            option = RegexOption.IGNORE_CASE,
        )
    }
}
