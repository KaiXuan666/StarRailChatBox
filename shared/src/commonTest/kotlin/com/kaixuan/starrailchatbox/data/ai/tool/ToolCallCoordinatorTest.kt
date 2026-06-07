package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiChatRequest
import com.kaixuan.starrailchatbox.data.ai.AiCompletion
import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiProvider
import com.kaixuan.starrailchatbox.data.ai.AiProviderConfig
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.ai.AiToolDefinition
import com.kaixuan.starrailchatbox.data.ai.AiUsage
import com.kaixuan.starrailchatbox.data.api.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ToolCallCoordinatorTest {
    @Test
    fun executesToolsInOrderAndReturnsTerminalOutput() = runTest {
        val events = mutableListOf<String>()
        val executable = FakeTool("first", ToolExecutionType.Executable) {
            events += "first"
            ToolResult.Continue("""{"ok":true}""")
        }
        val terminal = FakeTool("finish", ToolExecutionType.TerminalOutput) {
            events += "finish"
            ToolResult.Terminal("done", listOf("next"))
        }
        val provider = QueueProvider(
            listOf(
                AiCompletion(
                    AiMessage(
                        role = "assistant",
                        toolCalls = listOf(AiToolCall("1", "first", "{}")),
                    ),
                ),
                AiCompletion(
                    AiMessage(
                        role = "assistant",
                        toolCalls = listOf(AiToolCall("2", "finish", "{}")),
                    ),
                ),
            ),
        )
        val coordinator = ToolCallCoordinator(
            ToolRegistry(listOf(executable, terminal)),
            RiskBasedToolApprovalGateway,
        )

        val result = coordinator.complete(
            provider = provider,
            providerConfig = providerConfig(),
            request = AiChatRequest("model", listOf(AiMessage("user", "go"))),
            toolNames = listOf("first", "finish"),
            context = ToolContext("角色"),
            supportsToolCalls = true,
        )

        val success = assertIs<ApiResult.Success<CoordinatedCompletion>>(result)
        assertEquals("done", success.value.content)
        assertEquals(listOf("first", "finish"), events)
        assertEquals("tool", provider.requests[1].messages.last().role)
    }

    @Test
    fun rejectsRepeatedCalls() = runTest {
        val call = AiToolCall("1", "repeat", "{}")
        val provider = QueueProvider(
            listOf(
                AiCompletion(AiMessage("assistant", toolCalls = listOf(call))),
                AiCompletion(AiMessage("assistant", toolCalls = listOf(call.copy(id = "2")))),
            ),
        )
        val coordinator = ToolCallCoordinator(
            ToolRegistry(
                listOf(
                    FakeTool("repeat", ToolExecutionType.Executable) {
                        ToolResult.Continue("ok")
                    },
                ),
            ),
            RiskBasedToolApprovalGateway,
        )

        val result = coordinator.complete(
            provider,
            providerConfig(),
            AiChatRequest("model", listOf(AiMessage("user", "go"))),
            listOf("repeat"),
            ToolContext("角色"),
            supportsToolCalls = true,
        )

        assertIs<ApiResult.UnexpectedError>(result)
    }

    @Test
    fun rejectedRiskyToolIsReturnedToModelAsStructuredResult() = runTest {
        val provider = QueueProvider(
            listOf(
                AiCompletion(
                    AiMessage(
                        "assistant",
                        toolCalls = listOf(AiToolCall("1", "location", "{}")),
                    ),
                ),
                AiCompletion(AiMessage("assistant", content = "无法获取位置")),
            ),
        )
        val coordinator = ToolCallCoordinator(
            ToolRegistry(
                listOf(
                    FakeTool(
                        name = "location",
                        executionType = ToolExecutionType.Executable,
                        risk = ToolRisk.SensitiveRead,
                    ) {
                        ToolResult.Continue("should not run")
                    },
                ),
            ),
            approvalGateway = object : ToolApprovalGateway {
                override suspend fun approve(tool: AiTool, call: AiToolCall): Boolean = false
            },
        )

        val result = coordinator.complete(
            provider,
            providerConfig(),
            AiChatRequest("model", listOf(AiMessage("user", "where"))),
            listOf("location"),
            ToolContext("角色"),
            supportsToolCalls = true,
        )

        assertEquals(
            "无法获取位置",
            assertIs<ApiResult.Success<CoordinatedCompletion>>(result).value.content,
        )
        assertEquals(
            """{"error":"tool_rejected","message":"The tool request was not approved."}""",
            provider.requests[1].messages.last().content,
        )
    }

    @Test
    fun stopsAfterConfiguredRoundLimit() = runTest {
        val completions = List(4) { index ->
            AiCompletion(
                AiMessage(
                    "assistant",
                    toolCalls = listOf(AiToolCall("$index", "loop", """{"round":$index}""")),
                ),
            )
        }
        val coordinator = ToolCallCoordinator(
            ToolRegistry(
                listOf(
                    FakeTool("loop", ToolExecutionType.Executable) {
                        ToolResult.Continue("continue")
                    },
                ),
            ),
            RiskBasedToolApprovalGateway,
            maxRounds = 4,
        )

        val result = coordinator.complete(
            QueueProvider(completions),
            providerConfig(),
            AiChatRequest("model", listOf(AiMessage("user", "loop"))),
            listOf("loop"),
            ToolContext("角色"),
            supportsToolCalls = true,
        )

        assertIs<ApiResult.UnexpectedError>(result)
    }

    @Test
    fun requestsStrictQuickRepliesWhenInitialResponseHasNoSuggestions() = runTest {
        val provider = QueueProvider(
            listOf(
                AiCompletion(
                    message = AiMessage("assistant", content = "最终正文"),
                    usage = AiUsage(promptTokens = 20, completionTokens = 5, totalTokens = 25),
                ),
                AiCompletion(
                    message = AiMessage(
                        "assistant",
                        content = """{"suggestions":["🌸 继续聊","🍃 去散步","✨ 讲故事","🌙 看星星"]}""",
                    ),
                    usage = AiUsage(promptTokens = 8, completionTokens = 4, totalTokens = 12),
                    structuredOutput = buildJsonObject {
                        putJsonArray("suggestions") {
                            add(JsonPrimitive("🌸 继续聊"))
                            add(JsonPrimitive("🍃 去散步"))
                            add(JsonPrimitive("✨ 讲故事"))
                            add(JsonPrimitive("🌙 看星星"))
                        }
                    },
                ),
            ),
        )
        val coordinator = ToolCallCoordinator(
            ToolRegistry(listOf(QuickRepliesTool())),
            RiskBasedToolApprovalGateway,
        )
        val messages = listOf(
            AiMessage("system", "角色 prompt"),
            AiMessage("user", "第一轮用户"),
            AiMessage("assistant", "第一轮助手"),
            AiMessage("user", "第二轮用户"),
            AiMessage("assistant", "第二轮助手"),
            AiMessage("user", "第三轮用户"),
        )

        val result = coordinator.complete(
            provider = provider,
            providerConfig = providerConfig(),
            request = AiChatRequest("model", messages),
            toolNames = listOf(QuickRepliesTool.Name),
            context = ToolContext("角色"),
            supportsToolCalls = false,
        )

        val completion = assertIs<ApiResult.Success<CoordinatedCompletion>>(result).value
        assertEquals("最终正文", completion.content)
        assertEquals(4, completion.suggestions.size)
        assertEquals(37, completion.usage.totalTokens)
        assertEquals(2, provider.requests.size)

        val recovery = provider.requests[1]
        assertEquals("quick_reply_suggestions", recovery.responseFormat?.name)
        assertEquals(true, recovery.responseFormat?.strict)
        assertEquals(256, recovery.maxTokens)
        assertEquals(emptyList(), recovery.tools)
        assertEquals(
            listOf("system", "system", "user", "assistant", "user", "assistant"),
            recovery.messages.map(AiMessage::role),
        )
        assertEquals("角色 prompt", recovery.messages.first().content)
        assertEquals(
            listOf("第二轮用户", "第二轮助手", "第三轮用户", "最终正文"),
            recovery.messages.takeLast(4).map(AiMessage::content),
        )
    }

    @Test
    fun keepsInitialCompletionWhenStrictQuickRepliesRequestFails() = runTest {
        val provider = object : AiProvider {
            override val id: String = "failure"
            var requestCount = 0

            override suspend fun getModels(
                config: AiProviderConfig,
            ): ApiResult<List<String>> = ApiResult.Success(emptyList())

            override suspend fun complete(
                config: AiProviderConfig,
                request: AiChatRequest,
            ): ApiResult<AiCompletion> {
                requestCount += 1
                return if (requestCount == 1) {
                    ApiResult.Success(
                        AiCompletion(
                            message = AiMessage("assistant", content = "保留正文"),
                            usage = AiUsage(totalTokens = 9),
                        ),
                    )
                } else {
                    ApiResult.HttpError(400, "Structured outputs unsupported.")
                }
            }

            override suspend fun supportsToolCalls(config: AiProviderConfig): Boolean = false
        }
        val coordinator = ToolCallCoordinator(
            ToolRegistry(listOf(QuickRepliesTool())),
            RiskBasedToolApprovalGateway,
        )

        val result = coordinator.complete(
            provider = provider,
            providerConfig = providerConfig(),
            request = AiChatRequest("model", listOf(AiMessage("user", "你好"))),
            context = ToolContext("角色"),
            supportsToolCalls = false,
        )

        val completion = assertIs<ApiResult.Success<CoordinatedCompletion>>(result).value
        assertEquals("保留正文", completion.content)
        assertEquals(emptyList(), completion.suggestions)
        assertEquals(9, completion.usage.totalTokens)
        assertEquals(2, provider.requestCount)
    }
}

private class FakeTool(
    override val name: String,
    override val executionType: ToolExecutionType,
    override val risk: ToolRisk = ToolRisk.ReadOnly,
    private val handler: suspend () -> ToolResult,
) : AiTool {
    override fun definition(context: ToolContext) = AiToolDefinition(
        name = name,
        description = name,
        parameters = buildJsonObject { put("type", "object") },
    )

    override suspend fun execute(
        call: AiToolCall,
        context: ToolContext,
    ): ToolResult = handler()
}

private class QueueProvider(
    completions: List<AiCompletion>,
) : AiProvider {
    override val id: String = "fake"
    private val queue = completions.toMutableList()
    val requests = mutableListOf<AiChatRequest>()

    override suspend fun getModels(config: AiProviderConfig): ApiResult<List<String>> {
        return ApiResult.Success(emptyList())
    }

    override suspend fun complete(
        config: AiProviderConfig,
        request: AiChatRequest,
    ): ApiResult<AiCompletion> {
        requests += request
        return ApiResult.Success(queue.removeAt(0))
    }

    override suspend fun supportsToolCalls(config: AiProviderConfig): Boolean = true
}

private fun providerConfig() = AiProviderConfig(
    providerId = "fake",
    apiHost = "https://example.com",
    apiKey = "key",
    model = "model",
)
