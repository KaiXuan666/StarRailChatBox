package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiResponseFormat
import com.kaixuan.starrailchatbox.data.ai.AiResponseFormatType
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.ai.AiToolDefinition
import io.github.aakira.napier.Napier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
                        put("description", "4个简短快捷回复，每项以一个符合语境的 Emoji 开头，内容在12字以内。")
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
        Napier.d { "QuickRepliesTool prepareFallbackMessages" }
        val format = """
            <quick_replies_output_contract>
            快捷回复元数据块：
            <quick_replies>{"suggestions":["🌸 选项一","🍃 选项二","✨ 选项三","🌙 选项四"]}</quick_replies>

            特定规则：
            - 只能包含 suggestions 字段，且必须恰好有 4 个字符串。
            - 每个选项代表用户下一句可能发送的话，以符合语境的 Emoji 开头，简短、不重复。

            正确示例：
            当然，我会陪你一起去看看。
            <quick_replies>{"suggestions":["🌸 那就出发吧","🍃 先准备一下","✨ 你来带路","🌙 改天再去"]}</quick_replies>
            </quick_replies_output_contract>
        """.trimIndent()

        return messages.injectFallbackInstructions(
            systemFormat = format,
            controlSignal = "请遵守 system 消息中的 <quick_replies_output_contract>，并以完整的 <quick_replies> 元数据块结束回复。"
        )
    }

    override suspend fun parseFallback(
        content: String,
        context: ToolContext,
    ): ToolResult.Terminal? {

        parseJsonResponse(content)?.let { return it }

        val quickRepliesMatch = QuickRepliesRegex.find(content)
        if (quickRepliesMatch != null) {
            val suggestions = parseSuggestionsPayload(quickRepliesMatch.groupValues[1])
            if (suggestions.isNotEmpty()) {
                return terminalResult(
                    content = content.removeRange(quickRepliesMatch.range),
                    suggestions = suggestions,
                )
            }
        }

        val legacyMatch = SuggestionsRegex.find(content)
        if (legacyMatch != null) {
            return terminalResult(
                content = content.removeRange(legacyMatch.range),
                suggestions = parseSuggestionLines(legacyMatch.groupValues[1]),
            )
        }

        return parseUnclosedMetadataBlock(content, QuickRepliesOpenRegex, ::parseSuggestionsPayload)
            ?: parseUnclosedMetadataBlock(content, SuggestionsOpenRegex, ::parseSuggestionLines)
    }

    override fun prepareStructuredFallback(
        messages: List<AiMessage>,
        assistantContent: String,
        context: ToolContext,
    ): StructuredToolFallbackRequest {
        val rolePrompt = messages.firstOrNull { message ->
            message.role == "system" && !message.content.isNullOrBlank()
        }
        val recentConversation = (
            messages.filter { message ->
                message.role in ConversationRoles && !message.content.isNullOrBlank()
            } + AiMessage(role = "assistant", content = assistantContent)
            ).takeLast(RecentConversationMessageCount)
        return StructuredToolFallbackRequest(
            messages = listOfNotNull(rolePrompt) +
                AiMessage(role = "system", content = StructuredFallbackInstruction) +
                recentConversation,
            responseFormat = AiResponseFormat(
                name = StructuredFallbackName,
                description = "生成 4 条用户接下来可能发送的简短快捷回复。",
                schema = buildSuggestionsSchema(),
                strict = false,
                type = AiResponseFormatType.JsonObject,
            ),
        )
    }

    override fun parseStructuredFallback(
        output: JsonElement,
        context: ToolContext,
    ): List<String> {
        return (output as? JsonObject)
            ?.get("suggestions")
            ?.let(::parseSuggestionsElement)
            ?.takeIf { suggestions -> suggestions.size == MaxSuggestions }
            .orEmpty()
    }

    private fun parseUnclosedMetadataBlock(
        content: String,
        openingTag: Regex,
        parseSuggestions: (String) -> List<String>,
    ): ToolResult.Terminal? {
        val match = openingTag.findAll(content).lastOrNull() ?: return null
        val suggestions = parseSuggestions(content.substring(match.range.last + 1))
        return terminalResult(
            content = content.substring(0, match.range.first),
            suggestions = suggestions,
        )
    }

    private fun parseJsonResponse(content: String): ToolResult.Terminal? {
        val candidate = content.trim()
            .removeSurrounding("```json", "```")
            .removeSurrounding("```JSON", "```")
            .removeSurrounding("```", "```")
            .trim()
        val payload = try {
            json.parseToJsonElement(candidate).jsonObject
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: SerializationException) {
            return null
        }
        val suggestions = payload["suggestions"]
            ?.let(::parseSuggestionsElement)
            .orEmpty()
        if (suggestions.isEmpty()) {
            return null
        }
        val response = payload["ai_response"]
            ?.takeIf { it is JsonPrimitive && it.isString }
            ?.jsonPrimitive
            ?.content
            .orEmpty()
        return terminalResult(response, suggestions)
    }

    private fun parseSuggestionsPayload(payload: String): List<String> {
        val trimmed = payload.trim()
        val parsed = try {
            json.parseToJsonElement(trimmed)
        } catch (_: SerializationException) {
            null
        }
        val suggestions = when (parsed) {
            is JsonObject -> parsed["suggestions"]?.let(::parseSuggestionsElement)
            is JsonArray -> parseSuggestionsElement(parsed)
            else -> null
        }
        return suggestions?.takeIf(List<String>::isNotEmpty)
            ?: parseSuggestionLines(trimmed)
    }

    private fun parseSuggestionsElement(element: JsonElement): List<String> {
        return runCatching {
            element.jsonArray
                .mapNotNull { item ->
                    item.takeIf { it is JsonPrimitive && it.isString }
                        ?.jsonPrimitive
                        ?.content
                }
                .normalizeSuggestions()
        }.getOrDefault(emptyList())
    }

    private fun parseSuggestionLines(value: String): List<String> {
        return value
            .lines()
            .flatMap { line -> line.split(CommaRegex) }
            .map { line ->
                line.trim()
                    .removePrefix("-")
                    .removePrefix("*")
                    .replace(NumberedPrefixRegex, "")
                    .trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
            }
            .normalizeSuggestions()
    }

    private fun List<String>.normalizeSuggestions(): List<String> {
        return map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .take(MaxSuggestions)
    }

    private fun terminalResult(
        content: String,
        suggestions: List<String>,
    ): ToolResult.Terminal? {
        val normalizedContent = content.trim()
        if (normalizedContent.isEmpty() || suggestions.isEmpty()) {
            return null
        }
        return ToolResult.Terminal(
            content = normalizedContent,
            suggestions = suggestions,
        )
    }

    companion object {
        const val Name = "respond_with_quick_replies"
        private const val MaxSuggestions = 4
        private const val RecentConversationMessageCount = 4
        private const val StructuredFallbackName = "quick_reply_suggestions"
        private val ConversationRoles = setOf("user", "assistant")
        private val StructuredFallbackInstruction = """
            请严格生成 4 条用户接下来可能自然发送的简短回复。
            只能根据角色设定和最近对话生成，不得引入对话之外的信息。
            每条建议必须使用当前对话的语言，以符合语境的 Emoji 开头，内容互不重复，
            且 Emoji 后的文字不得超过 12 个字。
            只返回合法 JSON，不要使用 Markdown，也不要添加解释或其他字段。
            必须严格使用以下格式：
            {"suggestions":["🌸 快捷回复一","🍃 快捷回复二","✨ 快捷回复三","🌙 快捷回复四"]}
        """.trimIndent()
        private val QuickRepliesRegex = Regex(
            pattern = "<quick_replies\\s*>([\\s\\S]*?)</quick_replies\\s*>",
            option = RegexOption.IGNORE_CASE,
        )
        private val SuggestionsRegex = Regex(
            pattern = "<suggestions\\s*>([\\s\\S]*?)</suggestions\\s*>",
            option = RegexOption.IGNORE_CASE,
        )
        private val QuickRepliesOpenRegex = Regex(
            pattern = "<quick_replies\\s*>",
            option = RegexOption.IGNORE_CASE,
        )
        private val SuggestionsOpenRegex = Regex(
            pattern = "<suggestions\\s*>",
            option = RegexOption.IGNORE_CASE,
        )
        private val NumberedPrefixRegex = Regex("^\\d+[.)、]\\s*")
        private val CommaRegex = Regex("[,，]")

        private fun buildSuggestionsSchema() = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("suggestions") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                    put("minItems", MaxSuggestions)
                    put("maxItems", MaxSuggestions)
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("suggestions"))
            }
            put("additionalProperties", false)
        }
    }
}

@Serializable
private data class QuickRepliesArguments(
    @SerialName("ai_response")
    val aiResponse: String,
    val suggestions: List<String>,
)
