package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiResponseFormatType
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QuickRepliesToolTest {
    private val tool = QuickRepliesTool()
    private val context = ToolContext("流萤")

    @Test
    fun parsesTerminalToolOutput() = runTest {
        val result = tool.execute(
            AiToolCall(
                id = "call-1",
                name = QuickRepliesTool.Name,
                arguments = """
                    {
                      "ai_response": "你好，我在。",
                      "suggestions": ["🌸 坐一会儿", "🍃 去散步", "✨ 讲故事", "🌙 看星星"]
                    }
                """.trimIndent(),
            ),
            context,
        )

        val terminal = assertIs<ToolResult.Terminal>(result)
        assertEquals("你好，我在。", terminal.content)
        assertEquals(4, terminal.suggestions.size)
    }

    @Test
    fun definitionSupportsStrictStructuredToolCalls() {
        val definition = tool.definition(context)

        assertTrue(definition.strict)
        assertEquals(
            false,
            definition.parameters["additionalProperties"]?.jsonPrimitive?.content?.toBoolean(),
        )
        assertEquals(
            setOf("ai_response", "suggestions"),
            definition.parameters["required"]
                ?.jsonArray
                ?.map { it.jsonPrimitive.content }
                ?.toSet(),
        )
    }

    @Test
    fun invalidArgumentsReturnSafeError() = runTest {
        val result = tool.execute(
            AiToolCall("call-1", QuickRepliesTool.Name, "{broken"),
            context,
        )

        assertEquals("invalid_tool_arguments", assertIs<ToolResult.Error>(result).code)
    }

    @Test
    fun fallbackFormattingAndParsingAreOwnedByTool() {
        val messages = tool.prepareFallbackMessages(
            listOf(
                AiMessage("system", "保持人设"),
                AiMessage("user", "你好"),
            ),
            context,
        )

        assertTrue(messages.first().content.orEmpty().contains("<quick_replies_output_contract>"))
        assertTrue(messages.first().content.orEmpty().contains("\"suggestions\""))
        assertTrue(messages.last().content.orEmpty().contains("<quick_replies>"))

        val parsed = requireNotNull(
            tool.parseFallback(
                """
                    你好，我在。
                    <suggestions>
                    🌸 坐一会儿
                    🍃 去散步
                    ✨ 讲故事
                    🌙 看星星
                    </suggestions>
                """.trimIndent(),
                context,
            ),
        )
        assertEquals("你好，我在。", parsed.content)
        assertEquals(4, parsed.suggestions.size)
    }

    @Test
    fun parsesPreferredJsonMetadataBlock() {
        val parsed = requireNotNull(
            tool.parseFallback(
                """
                    好呀，我们现在就出发。
                    <quick_replies>{"suggestions":["🌸 走吧","🍃 带上点心","✨ 你来带路","🌙 晚点再去"]}</quick_replies>
                """.trimIndent(),
                context,
            ),
        )

        assertEquals("好呀，我们现在就出发。", parsed.content)
        assertEquals(listOf("🌸 走吧", "🍃 带上点心", "✨ 你来带路", "🌙 晚点再去"), parsed.suggestions)
    }

    @Test
    fun parsesToolStyleJsonWrappedInMarkdownFence() {
        val parsed = requireNotNull(
            tool.parseFallback(
                """
                    ```json
                    {
                      "ai_response": "我会在这里陪你。",
                      "suggestions": ["🌸 聊聊今天", "🍃 一起散步", "✨ 讲个故事", "🌙 看看星星"]
                    }
                    ```
                """.trimIndent(),
                context,
            ),
        )

        assertEquals("我会在这里陪你。", parsed.content)
        assertEquals(4, parsed.suggestions.size)
    }

    @Test
    fun parsesLegacyTagsCaseInsensitivelyAndNormalizesLists() {
        val parsed = requireNotNull(
            tool.parseFallback(
                """
                    我听着呢。
                    <SUGGESTIONS>
                    1. 🌸 继续说
                    2) 🍃 换个话题
                    - ✨ 猜猜看
                    * 🌙 安静坐会儿
                    </SUGGESTIONS>
                """.trimIndent(),
                context,
            ),
        )

        assertEquals("我听着呢。", parsed.content)
        assertEquals(
            listOf("🌸 继续说", "🍃 换个话题", "✨ 猜猜看", "🌙 安静坐会儿"),
            parsed.suggestions,
        )
    }

    @Test
    fun recoversSuggestionsFromUnclosedMetadataBlock() {
        val parsed = requireNotNull(
            tool.parseFallback(
                """
                    没关系，我们慢慢来。
                    <quick_replies>{"suggestions":["🌸 再试一次","🍃 休息一下","✨ 换个办法","🌙 陪我聊聊"]}
                """.trimIndent(),
                context,
            ),
        )

        assertEquals("没关系，我们慢慢来。", parsed.content)
        assertEquals(4, parsed.suggestions.size)
    }

    @Test
    fun structuredFallbackUsesStrictSuggestionsOnlySchema() {
        val fallback = tool.prepareStructuredFallback(
            messages = listOf(
                AiMessage("system", "角色 prompt"),
                AiMessage("user", "你好"),
            ),
            assistantContent = "你好，我在。",
            context = context,
        )

        assertEquals("quick_reply_suggestions", fallback.responseFormat.name)
        assertEquals(AiResponseFormatType.JsonObject, fallback.responseFormat.type)
        assertEquals(false, fallback.responseFormat.strict)
        assertTrue(fallback.messages[1].content.orEmpty().contains("JSON"))
        assertEquals(
            setOf("suggestions"),
            fallback.responseFormat.schema["required"]
                ?.jsonArray
                ?.map { it.jsonPrimitive.content }
                ?.toSet(),
        )
        assertEquals(
            false,
            fallback.responseFormat.schema["additionalProperties"]
                ?.jsonPrimitive
                ?.content
                ?.toBoolean(),
        )
    }
}
