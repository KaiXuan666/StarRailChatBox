package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.ai.AiToolDefinition
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
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
import kotlinx.coroutines.flow.collectLatest
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

/**
 * 联网搜索工具。
 * 对接 Bocha AI Search API，支持工具调用模式和 XML 降级模式。
 */
class BochaSearchTool(
    private val profileStore: ProfileStore,
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
    coroutineScope: CoroutineScope? = null,
) : AiTool {
    override val name: String = Name
    override val executionType: ToolExecutionType = ToolExecutionType.Executable
    override val risk: ToolRisk = ToolRisk.ReadOnly

    private val activeScope = coroutineScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cachedIsAvailable: Boolean = false

    init {
        activeScope.launch {
            profileStore.profile.collectLatest { profile ->
                cachedIsAvailable = profile?.enableWebSearch ?: false
            }
        }
    }

    override fun isAvailable(): Boolean = cachedIsAvailable

    override fun definition(context: ToolContext): AiToolDefinition {
        return AiToolDefinition(
            name = name,
            description = "当用户询问实时信息、新闻、世界知识或你无法确定答案时，使用此工具在互联网上搜索。搜索结果将以结构化格式返回。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "要搜索的查询词或自然语言问题。")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("query"))
                }
                put("additionalProperties", false)
            }
        )
    }

    override suspend fun execute(call: AiToolCall, context: ToolContext): ToolResult {
        return try {
            val arguments = json.parseToJsonElement(call.arguments).jsonObject
            val query = arguments["query"]?.jsonPrimitive?.content ?: ""
            if (query.isBlank()) {
                return ToolResult.Error("invalid_tool_arguments", "Search query is empty.")
            }
            
            val resultText = performSearch(query)
            ToolResult.Continue(resultText)
        } catch (t: Throwable) {
            Napier.e(tag = "BochaSearchTool", throwable = t) { "Search failed" }
            ToolResult.Error("search_failed", t.message ?: "Unknown search error")
        }
    }

    override fun prepareFallbackMessages(
        messages: List<AiMessage>,
        context: ToolContext,
    ): List<AiMessage> {
        val format = """
            <search_output_contract>
            联网搜索元数据块格式：
            <search>{"query":"具体的搜索关键词"}</search>

            特定规则：
            - 当你需要获取外部知识或实时信息时，必须在回复中包含此元数据块。
            - 如果你选择使用此块，请确保查询词精准。

            正确示例：
            让我帮你查一下上海今天的天气。
            <search>{"query":"上海今天天气"}</search>
            </search_output_contract>
        """.trimIndent()

        return messages.injectFallbackInstructions(
            systemFormat = format,
            controlSignal = "如需联网，请遵守 <search_output_contract>，并以完整的 <search> 元数据块结束回复。"
        )
    }

    override suspend fun parseFallback(
        content: String,
        context: ToolContext,
    ): ToolResult.Terminal? {
        val match = SearchRegex.find(content) ?: return null
        val payloadStr = match.groupValues[1].trim()
        val plainText = content.removeRange(match.range).trim()

        return try {
            val payload = json.parseToJsonElement(payloadStr).jsonObject
            val query = payload["query"]?.jsonPrimitive?.content ?: ""
            
            if (query.isBlank()) {
                ToolResult.Terminal(content = plainText)
            } else {
                val searchResult = performSearch(query)
                ToolResult.Terminal(
                    content = "$plainText\n\n[联网搜索结果]\n$searchResult"
                )
            }
        } catch (t: Throwable) {
            Napier.e(tag = "BochaSearchTool", throwable = t) { "Fallback search failed" }
            ToolResult.Terminal(content = plainText)
        }
    }

    private suspend fun performSearch(query: String): String {
        Napier.d(tag = "BochaSearchTool") { "Performing search: $query" }
        val response = httpClient.post("https://api.bochaai.com/v1/web-search") {
            header(HttpHeaders.Authorization, "Bearer $ApiKey")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("query", query)
                put("freshness", "noLimit")
                put("summary", true)
                put("count", 5)
            }.toString())
        }

        val responseText = response.bodyAsText()
        val jsonObject = json.parseToJsonElement(responseText).jsonObject
        val data = jsonObject["data"]?.jsonObject
        val webPages = data?.get("webPages")?.jsonObject?.get("value")?.jsonArray

        return if (webPages != null && webPages.isNotEmpty()) {
            webPages.joinToString("\n\n") { element ->
                val item = element.jsonObject
                val title = item["name"]?.jsonPrimitive?.content ?: "No Title"
                val url = item["url"]?.jsonPrimitive?.content ?: ""
                val snippet = item["summary"]?.jsonPrimitive?.content 
                    ?: item["snippet"]?.jsonPrimitive?.content 
                    ?: ""
                "标题: $title\n链接: $url\n摘要: $snippet"
            }
        } else {
            "未找到相关搜索结果。"
        }
    }

    companion object {
        const val Name = "web_search"
        private const val ApiKey = "sk-829bdb64bbef408185b24139749d462d"
        private val SearchRegex = Regex(
            pattern = "<search\\s*>([\\s\\S]*?)(?:</search\\s*>|$)",
            option = RegexOption.IGNORE_CASE,
        )
    }
}
