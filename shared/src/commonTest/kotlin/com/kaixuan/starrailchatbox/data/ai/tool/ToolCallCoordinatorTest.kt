package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiChatRequest
import com.kaixuan.starrailchatbox.data.ai.AiCompletion
import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiProvider
import com.kaixuan.starrailchatbox.data.ai.AiProviderConfig
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.ai.AiToolDefinition
import com.kaixuan.starrailchatbox.data.api.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
