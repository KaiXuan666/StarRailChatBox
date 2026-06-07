package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiResponseFormat
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.ai.AiToolDefinition
import kotlinx.serialization.json.JsonElement

enum class ToolExecutionType {
    /** 直接产生最终助手输出，不再继续请求模型。 */
    TerminalOutput,

    /** 产生需要回传给模型的工具结果消息。 */
    Executable,
}

/** 工具副作用执行前由 [ToolApprovalGateway] 判断的风险等级。 */
enum class ToolRisk {
    ReadOnly,
    DeviceState,
    SensitiveRead,
    ExternalWrite,
}

data class ToolContext(
    val characterName: String,
)

data class StructuredToolFallbackRequest(
    val messages: List<AiMessage>,
    val responseFormat: AiResponseFormat,
    val temperature: Double = 0.2,
    val topP: Double = 1.0,
    val maxTokens: Int = 256,
)

sealed interface ToolResult {
    data class Terminal(
        val content: String,
        val suggestions: List<String> = emptyList(),
    ) : ToolResult

    data class Continue(val content: String) : ToolResult

    data class Error(
        val code: String,
        val message: String,
    ) : ToolResult
}

/**
 * 与 Provider 无关的模型可调用能力扩展点。
 *
 * 实现类负责自身 schema、参数校验和结果映射；平台 API 必须委托给
 * [PlatformToolExecutor]。
 */
interface AiTool {
    val name: String
    val executionType: ToolExecutionType
    val risk: ToolRisk

    fun definition(context: ToolContext): AiToolDefinition

    fun isAvailable(): Boolean = true

    suspend fun execute(
        call: AiToolCall,
        context: ToolContext,
    ): ToolResult

    fun prepareFallbackMessages(
        messages: List<AiMessage>,
        context: ToolContext,
    ): List<AiMessage> = messages

    fun parseFallback(
        content: String,
        context: ToolContext,
    ): ToolResult.Terminal? = null

    fun prepareStructuredFallback(
        messages: List<AiMessage>,
        assistantContent: String,
        context: ToolContext,
    ): StructuredToolFallbackRequest? = null

    fun parseStructuredFallback(
        output: JsonElement,
        context: ToolContext,
    ): List<String> = emptyList()
}

/**
 * 已注册工具的只读查找表。
 *
 * 新工具通过依赖注入注册后即可被协调器发现，无需在 Repository 中增加条件分支。
 */
class ToolRegistry(tools: List<AiTool>) {
    private val toolsByName = tools.associateBy(AiTool::name)

    fun find(name: String): AiTool? = toolsByName[name]

    fun availableTools(): List<AiTool> = toolsByName.values.filter(AiTool::isAvailable)
}

/** 根据当前审批策略判断工具调用是否允许继续执行。 */
interface ToolApprovalGateway {
    suspend fun approve(tool: AiTool, call: AiToolCall): Boolean
}

object RiskBasedToolApprovalGateway : ToolApprovalGateway {
    override suspend fun approve(tool: AiTool, call: AiToolCall): Boolean {
        return tool.risk == ToolRisk.ReadOnly
    }
}

/**
 * 公共代码访问设备和操作系统能力的最小接口。
 *
 * 手电筒、振动、位置、日程等具体工具通过此接口执行平台操作。
 */
interface PlatformToolExecutor {
    fun isAvailable(toolName: String): Boolean

    suspend fun execute(toolName: String, arguments: String): ToolResult
}

object UnsupportedPlatformToolExecutor : PlatformToolExecutor {
    override fun isAvailable(toolName: String): Boolean = false

    override suspend fun execute(toolName: String, arguments: String): ToolResult {
        return ToolResult.Error(
            code = "platform_tool_unavailable",
            message = "This tool is not available on the current platform.",
        )
    }
}

/**
 * 创建当前目标平台的设备能力执行边界。
 *
 * 平台实现不得向公共代码暴露 Android、Apple、JVM 或浏览器专属类型。
 */
expect fun createPlatformToolExecutor(): PlatformToolExecutor
