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
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okio.BufferedSink
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
            // 优化工具描述，避免纯文字描述场景误触发
            description = "当用户明确要求画画、生成图片、创作图像、拍照时，要求AI发自拍（发送AI自己的照片）时，或需要将某种视觉场景具象化为图片时，或需要回复图片时触发。该工具根据用户的意图或AI自身的人设设定生成对应的图片。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    // 1. 提示词参数
                    putJsonObject("prompt") {
                        put("type", "string")
                        put("description", """
                            详细的图片生成描述词。应当包含主体（主体描述）+ 场景（场景描述）+ 风格（定义风格）+ 镜头语言 + 氛围词 + 细节修饰。示例：近景镜头，18岁的中国女孩，古代服饰，圆脸，看着镜头，民族优雅的服装，商业摄影，室外，电影级光照，半身特写，精致的淡妆，锐利的边缘。
                            特别注意：如果用户是要求 AI 发送【自拍】、【自己的照片】或要求 AI 参与到画面中，请务必根据当前 AI 的正在扮演谁，角色人设（外貌、性别、年龄、穿搭风格等）来具象化描述主体。如果是自拍且是有一定知名度的角色，应先说明自己是谁，如：崩坏星穹铁道的三月七。
                        """.trimIndent())
                    }
                    // 2. 纵横比参数（增加了 enum 限制）
                    putJsonObject("aspect_ratio") {
                        put("type", "string")
                        put("description", "图片的纵横比。默认为 '1:1'。")
                        putJsonArray("enum") {
                            add(JsonPrimitive("1:1"))
                            add(JsonPrimitive("4:3"))
                            add(JsonPrimitive("3:4"))
                            add(JsonPrimitive("16:9"))
                            add(JsonPrimitive("9:16"))
                        }
                    }
                    // 3. 附带文本参数（修复了结构错误）
                    putJsonObject("attached_text") {
                        put("type", "string")
                        put("description", "在向用户展示图片时，AI 附带的文本消息。请根据当前对话语境拟人化生成，例如：'看，这就是我的照片。' 或 '按照你的要求，图片已经生成好啦！'")
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
            val attachedText = arguments["attached_text"]?.jsonPrimitive?.content ?: "喏，你要的图片。"

            generateImage(prompt, aspectRatio, attachedText)
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
        <image_generation>{"prompt":"近景镜头，18岁的中国女孩，古代服饰，圆脸，看着镜头，商业摄影，半身特写","aspect_ratio":"1:1","attached_text":"看，这就是我的照片。"}</image_generation>

        特定契约规则：
        当用户明确要求画画、生成图片、创作图像、拍照时，要求AI发自拍（发送AI自己的照片）时，或需要将某种视觉场景具象化为图片时，或需要回复图片时，必须在回复末尾附带此元数据块。
        1. prompt: 字符串。详细的图片生成描述词。必须包含：主体 + 场景 + 风格 + 镜头/光照 + 细节修饰。如果是自拍，请务必根据当前 AI 的正在扮演谁，角色人设（外貌、性别、年龄、穿搭风格等）来具象化描述主体。如果是自拍且是有一定知名度的角色，应先说明自己是谁，如：崩坏星穹铁道的三月七。
        2. aspect_ratio: 字符串。可选值: "1:1", "4:3", "3:4", "16:9", "9:16"。默认 "1:1"。
        3. attached_text: 字符串。向用户展示图片时的拟人化前置独白。
        </image_generation_contract>
    """.trimIndent()

        return messages.injectFallbackInstructions(
            systemFormat = format,
            // 这里的控制信号也进行强化，明确强调“结尾”
            controlSignal = "若判定需要回复图片，请严格遵守 <image_generation_contract>，并以完整的<image_generation>...</image_generation>元数据块结束回复。"
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
            val attachedText = payload["attached_text"]?.jsonPrimitive?.content ?: "喏，你要的图片。"

            if (prompt.isBlank()) {
                ToolResult.Terminal(content = plainText)
            } else {
                val result = generateImage(prompt, aspectRatio, attachedText)
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
        aspectRatio: String,
        attachedText: String
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
                val randomSuffix = (Random.nextLong() and 0x7FFFFFFFFFFFFFFF).toString(36)
                // 优先从 URL 提取后缀名
                val extension = imageUrl.substringAfterLast('.', "png").let { ext ->
                    if (ext.contains('/') || ext.length > 5) "png" else ext
                }
                val fileName = "gen_$randomSuffix.$extension"
                val relativePath = "generated_images/$fileName"
                val fullPath = fileManager.appDataDir / relativePath
                
                // 确保父目录存在
                fullPath.parent?.let { fileManager.createDirectories(it) }

                // 流式下载并保存
                httpClient.prepareGet(imageUrl).execute { httpResponse ->
                    val channel: ByteReadChannel = httpResponse.body()
                    fileManager.fileSystem.write(fullPath) {
                        while (!channel.isClosedForRead) {
                            val packet = channel.readRemaining(8192)
                            while (!packet.exhausted()) {
                                val bytes = packet.readByteArray()
                                write(bytes)
                            }
                        }
                    }
                }
                
                val localPath = fullPath.toString()
                Napier.d { "Image saved via stream to: $localPath" }

                ToolResult.Terminal(
                    content = attachedText,
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
