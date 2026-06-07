package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.ai.AiToolDefinition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * 返回角色回复正文和可点击建议的终止型工具。
 *
 * 对不支持工具调用的模型，此工具同时负责提示词注入和 XML 降级解析，
 * 从而让共享聊天上下文构造器保持与具体工具无关。
 */
class QuickRepliesTool(
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AiTool {
    override val name: String = Name
    override val executionType: ToolExecutionType = ToolExecutionType.TerminalOutput
    override val risk: ToolRisk = ToolRisk.ReadOnly

    override fun definition(context: ToolContext): AiToolDefinition {
        return AiToolDefinition(
            name = name,
            description = "生成${context.characterName}的角色扮演回复，并生成4个可供用户点击的快捷回复选项。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("ai_response") {
                        put("type", "string")
                        put("description", "${context.characterName}的角色扮演文本回复内容。")
                    }
                    putJsonObject("suggestions") {
                        put("type", "array")
                        put("description", "4个简短快捷回复，每项以一个符合语境的 Emoji 开头。")
                        putJsonObject("items") {
                            put("type", "string")
                        }
                        put("minItems", 4)
                        put("maxItems", 4)
                    }
                }
                putJsonArray("required") {
                    add(kotlinx.serialization.json.JsonPrimitive("ai_response"))
                    add(kotlinx.serialization.json.JsonPrimitive("suggestions"))
                }
                put("additionalProperties", false)
            },
        )
    }

    override suspend fun execute(call: AiToolCall, context: ToolContext): ToolResult {
        return try {
            val arguments = json.decodeFromString<QuickRepliesArguments>(call.arguments)
            val content = arguments.aiResponse.trim()
            if (content.isEmpty()) {
                ToolResult.Error("invalid_tool_arguments", "Quick replies content is empty.")
            } else {
                ToolResult.Terminal(
                    content = content,
                    suggestions = arguments.suggestions
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                        .take(MaxSuggestions),
                )
            }
        } catch (_: SerializationException) {
            ToolResult.Error(
                code = "invalid_tool_arguments",
                message = "Quick replies arguments are invalid.",
            )
        }
    }

    override fun prepareFallbackMessages(
        messages: List<AiMessage>,
        context: ToolContext,
    ): List<AiMessage> {
        val format = """
            【重要输出格式规范】
            回复必须包含角色聊天正文，以及 4 个快捷回复选项：
            <suggestions>
            [Emoji] [快捷回复1]
            [Emoji] [快捷回复2]
            [Emoji] [快捷回复3]
            [Emoji] [快捷回复4]
            </suggestions>
            每个选项必须以一个符合语境的 Emoji 开头，内容保持简短。
        """.trimIndent()

        val systemIndex = messages.indexOfFirst { it.role == "system" }
        val prepared = messages.toMutableList()
        if (systemIndex >= 0) {
            val message = prepared[systemIndex]
            prepared[systemIndex] = message.copy(
                content = listOfNotNull(message.content?.trim(), format)
                    .filter(String::isNotEmpty)
                    .joinToString("\n\n"),
            )
        } else {
            prepared.add(0, AiMessage(role = "system", content = format))
        }

        val userIndex = prepared.indexOfLast { it.role == "user" }
        if (userIndex >= 0) {
            val message = prepared[userIndex]
            prepared[userIndex] = message.copy(
                content = message.content.orEmpty().trim() +
                    "\n\n(请严格使用 <suggestions> 标签提供 4 个快捷回复选项。)",
            )
        }
        return prepared
    }

    override fun parseFallback(
        content: String,
        context: ToolContext,
    ): ToolResult.Terminal? {
        val match = SuggestionsRegex.find(content) ?: return null
        val suggestions = match.groupValues[1]
            .lines()
            .map { it.trim().removePrefix("-").removePrefix("*").trim() }
            .filter(String::isNotEmpty)
            .take(MaxSuggestions)
        return ToolResult.Terminal(
            content = content.replace(SuggestionsRegex, "").trim(),
            suggestions = suggestions,
        )
    }

    companion object {
        const val Name = "respond_with_quick_replies"
        private const val MaxSuggestions = 4
        private val SuggestionsRegex = Regex("<suggestions>([\\s\\S]*?)</suggestions>")
    }
}

@Serializable
private data class QuickRepliesArguments(
    @SerialName("ai_response")
    val aiResponse: String,
    val suggestions: List<String>,
)
