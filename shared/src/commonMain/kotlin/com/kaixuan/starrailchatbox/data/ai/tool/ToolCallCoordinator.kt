package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiChatRequest
import com.kaixuan.starrailchatbox.data.ai.AiCompletion
import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiProvider
import com.kaixuan.starrailchatbox.data.ai.AiProviderConfig
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.ai.AiUsage
import com.kaixuan.starrailchatbox.data.ai.ToolChoice
import com.kaixuan.starrailchatbox.data.api.ApiResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class CoordinatedCompletion(
    val content: String,
    val suggestions: List<String>,
    val finishReason: String?,
    val usage: AiUsage,
    val voiceAttachmentUri: String? = null,
    val voiceDurationMs: Long? = null,
    val imageAttachmentUri: String? = null,
)

/**
 * 持续编排 Provider 与工具交互，直到得到最终助手输出。
 *
 * 工具调用按模型返回顺序处理：可执行结果追加为 tool 消息，终止型结果直接结束；
 * 重复调用或达到 [maxRounds] 时终止异常循环。
 */
class ToolCallCoordinator(
    private val toolRegistry: ToolRegistry,
    private val approvalGateway: ToolApprovalGateway,
    private val maxRounds: Int = 4,
) {
    suspend fun complete(
        provider: AiProvider,
        providerConfig: AiProviderConfig,
        request: AiChatRequest,
        toolNames: List<String>? = null,
        context: ToolContext,
        supportsToolCalls: Boolean,
    ): ApiResult<CoordinatedCompletion> {
        Napier.d { "ToolCallCoordinator toolNames=$toolNames" }
        val tools = toolNames
            ?.mapNotNull(toolRegistry::find)
            ?.filter(AiTool::isAvailable)
            ?: toolRegistry.availableTools()
        Napier.d { "ToolCallCoordinator available tools: ${tools.map { it.name }}" }
        val initialResult = if (!supportsToolCalls) {
            completeWithFallback(provider, providerConfig, request, tools, context)
        } else {
            completeWithTools(provider, providerConfig, request, tools, context)
        }
        return recoverMissingStructuredOutput(
            provider = provider,
            providerConfig = providerConfig,
            request = request,
            tools = tools,
            context = context,
            initialResult = initialResult,
        )
    }

    private suspend fun completeWithTools(
        provider: AiProvider,
        providerConfig: AiProviderConfig,
        request: AiChatRequest,
        tools: List<AiTool>,
        context: ToolContext,
    ): ApiResult<CoordinatedCompletion> {
        var messages = request.messages
        var usage = AiUsage()
        val seenCalls = mutableSetOf<String>()

        repeat(maxRounds) {
            val providerResult = provider.complete(
                providerConfig,
                request.copy(
                    messages = messages,
                    tools = tools.map { tool -> tool.definition(context) },
                    toolChoice = if (tools.isEmpty()) ToolChoice.None else ToolChoice.Required,
                ),
            )
            val completion = when (providerResult) {
                is ApiResult.Success -> providerResult.value
                is ApiResult.HttpError -> return providerResult
                is ApiResult.NetworkError -> return providerResult
                is ApiResult.UnexpectedError -> return providerResult
            }
            usage += completion.usage
            val calls = completion.message.toolCalls
            if (calls.isEmpty()) {
                return completion.toResult(usage)
            }

            val toolMessages = mutableListOf<AiMessage>()
            var terminalContent: String? = null
            var terminalSuggestions = mutableListOf<String>()
            var terminalVoiceUri: String? = null
            var terminalVoiceDuration: Long? = null
            var terminalImageUri: String? = null
            var hasTerminal = false

            for (call in calls) {
                val signature = "${call.name}:${call.arguments}"
                if (!seenCalls.add(signature)) {
                    return ApiResult.UnexpectedError("Repeated tool call detected.")
                }
                val tool = toolRegistry.find(call.name)
                val result = when {
                    tool == null -> ToolResult.Error("unknown_tool", "The requested tool is not registered.")
                    !tool.isAvailable() -> ToolResult.Error("tool_unavailable", "The requested tool is unavailable.")
                    else -> executeTool(tool, call, context)
                }
                when (result) {
                    is ToolResult.Terminal -> {
                        terminalContent = result.content
                        terminalSuggestions.addAll(result.suggestions)
                        if (result.voiceAttachmentUri != null) {
                            terminalVoiceUri = result.voiceAttachmentUri
                            terminalVoiceDuration = result.voiceDurationMs
                        }
                        if (result.imageAttachmentUri != null) {
                            terminalImageUri = result.imageAttachmentUri
                        }
                        hasTerminal = true
                    }
                    is ToolResult.Continue -> toolMessages += AiMessage(
                        role = "tool",
                        content = result.content,
                        toolCallId = call.id,
                    )
                    is ToolResult.Error -> toolMessages += AiMessage(
                        role = "tool",
                        content = buildJsonObject {
                            put("error", result.code)
                            put("message", result.message)
                        }.toString(),
                        toolCallId = call.id,
                    )
                }
            }

            if (hasTerminal) {
                return ApiResult.Success(
                    CoordinatedCompletion(
                        content = terminalContent.orEmpty(),
                        suggestions = terminalSuggestions.distinct(),
                        finishReason = completion.finishReason,
                        usage = usage,
                        voiceAttachmentUri = terminalVoiceUri,
                        voiceDurationMs = terminalVoiceDuration,
                        imageAttachmentUri = terminalImageUri,
                    ),
                )
            }
            messages = messages + completion.message + toolMessages
        }
        return ApiResult.UnexpectedError("Tool call round limit exceeded.")
    }

    private suspend fun recoverMissingStructuredOutput(
        provider: AiProvider,
        providerConfig: AiProviderConfig,
        request: AiChatRequest,
        tools: List<AiTool>,
        context: ToolContext,
        initialResult: ApiResult<CoordinatedCompletion>,
    ): ApiResult<CoordinatedCompletion> {
        val initial = (initialResult as? ApiResult.Success)?.value ?: return initialResult
        if (initial.content.isBlank() || initial.suggestions.isNotEmpty()) {
            return initialResult
        }
        val fallback = tools.firstNotNullOfOrNull { tool ->
            tool.prepareStructuredFallback(
                messages = request.messages,
                assistantContent = initial.content,
                context = context,
            )?.let { fallbackRequest -> tool to fallbackRequest }
        } ?: return initialResult
        val (tool, fallbackRequest) = fallback
        val recoveryResult = provider.complete(
            config = providerConfig,
            request = request.copy(
                messages = fallbackRequest.messages,
                temperature = fallbackRequest.temperature,
                topP = fallbackRequest.topP,
                maxTokens = fallbackRequest.maxTokens,
                tools = emptyList(),
                toolChoice = ToolChoice.None,
                responseFormat = fallbackRequest.responseFormat,
            ),
        )
        val recovery = (recoveryResult as? ApiResult.Success)?.value ?: return initialResult
        val structuredOutput = recovery.structuredOutput ?: return initialResult
        val suggestions = tool.parseStructuredFallback(structuredOutput, context)
        if (suggestions.isEmpty()) {
            return initialResult
        }
        return ApiResult.Success(
            initial.copy(
                suggestions = suggestions,
                usage = initial.usage + recovery.usage,
            ),
        )
    }

    private suspend fun executeTool(
        tool: AiTool,
        call: AiToolCall,
        context: ToolContext,
    ): ToolResult {
        return try {
            if (tool.risk != ToolRisk.ReadOnly && !approvalGateway.approve(tool, call)) {
                ToolResult.Error("tool_rejected", "The tool request was not approved.")
            } else {
                tool.execute(call, context)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            ToolResult.Error(
                code = "tool_execution_failed",
                message = "The tool could not be executed.",
            )
        }
    }

    private suspend fun completeWithFallback(
        provider: AiProvider,
        providerConfig: AiProviderConfig,
        request: AiChatRequest,
        tools: List<AiTool>,
        context: ToolContext,
    ): ApiResult<CoordinatedCompletion> {
        var messages = request.messages
        if (tools.isNotEmpty()) {
            val baseMessages = request.messages.injectFallbackInstructions(
                systemFormat = CommonFallbackInstruction,
                controlSignal = "请遵守 system 消息中的 <metadata_block_contract>，并按需以完整的元数据块结束回复。"
            )
            messages = tools.fold(baseMessages) { current, tool ->
                tool.prepareFallbackMessages(current, context)
            }
        }
        return when (
            val result = provider.complete(
                providerConfig,
                request.copy(messages = messages, tools = emptyList(), toolChoice = ToolChoice.None),
            )
        ) {
            is ApiResult.Success -> {
                val completion = result.value
                val content = completion.message.content.orEmpty()
                
                var currentContent = content
                var finalSuggestions = mutableListOf<String>()
                var finalVoiceUri: String? = null
                var finalVoiceDuration: Long? = null
                var finalImageUri: String? = null
                var hasTerminal = false

                for (tool in tools) {
                    val res = tool.parseFallback(currentContent, context)
                    if (res != null) {
                        currentContent = res.content
                        finalSuggestions.addAll(res.suggestions)
                        if (res.voiceAttachmentUri != null) {
                            finalVoiceUri = res.voiceAttachmentUri
                            finalVoiceDuration = res.voiceDurationMs
                        }
                        if (res.imageAttachmentUri != null) {
                            finalImageUri = res.imageAttachmentUri
                        }
                        hasTerminal = true
                    }
                }

                if (hasTerminal) {
                    ApiResult.Success(
                        CoordinatedCompletion(
                            content = currentContent,
                            suggestions = finalSuggestions.distinct(),
                            finishReason = completion.finishReason,
                            usage = completion.usage,
                            voiceAttachmentUri = finalVoiceUri,
                            voiceDurationMs = finalVoiceDuration,
                            imageAttachmentUri = finalImageUri,
                        ),
                    )
                } else {
                    completion.toResult(completion.usage)
                }
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnexpectedError -> result
        }
    }

    private fun AiCompletion.toResult(totalUsage: AiUsage) = ApiResult.Success(
        CoordinatedCompletion(
            content = message.content.orEmpty(),
            suggestions = emptyList(),
            finishReason = finishReason,
            usage = totalUsage,
        ),
    )

    companion object {
        private val CommonFallbackInstruction = """
            <metadata_block_contract>
            你需要在回复的正文结束后另起一行，输出一个或多个特定工具的元数据块。
            你可以同时输出多个工具的元数据块。

            通用强制规则：
            - 每个元数据块必须是整条回复的最后一部分，不能省略、改名或放进 Markdown 代码块。
            - 标签内部必须是合法 JSON；格式必须完全符合工具要求的 schema。
            - 正文中不要提及元数据块、格式要求、JSON 或标签本身。
            - 输出前自行检查：正文非空、标签完整、JSON 合法。
            </metadata_block_contract>
        """.trimIndent()
    }
}
