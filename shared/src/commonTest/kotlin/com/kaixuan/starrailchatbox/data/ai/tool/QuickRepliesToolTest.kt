package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import kotlinx.coroutines.test.runTest
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

        assertTrue(messages.first().content.orEmpty().contains("重要输出格式规范"))
        assertTrue(messages.last().content.orEmpty().contains("<suggestions>"))

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
}
