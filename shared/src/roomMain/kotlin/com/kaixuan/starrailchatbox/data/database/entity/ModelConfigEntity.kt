package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "model_config",
    indices = [
        Index(value = ["provider", "model_name"]),
        Index(value = ["enabled", "deleted_at"]),
    ],
)
/**
 * 模型配置的数据库实体类，保存用户配置的 AI 模型接口参数。
 */
data class ModelConfigEntity(
    /** 配置的唯一标识符（例如，默认配置 ID 为 "default"） */
    @PrimaryKey
    val id: String,

    /** 服务提供商标识符（例如 "custom", "openai", "deepseek" 等） */
    val provider: String,

    /** 配置显示的友好名称（例如 "默认模型"） */
    val name: String,

    /** API 请求的基础 URL 地址 */
    @ColumnInfo(name = "base_url")
    val baseUrl: String,

    /** 经过加密的 API 密钥密文，为 null 表示无需密钥或未配置 */
    @ColumnInfo(name = "api_key_encrypted")
    val apiKeyEncrypted: String? = null,

    /** 模型在服务端对应的真实名称（例如 "gpt-4o", "deepseek-chat" 等） */
    @ColumnInfo(name = "model_name")
    val modelName: String,

    /** 模型支持的上下文窗口大小（最大输入 Token 数） */
    @ColumnInfo(name = "context_window")
    val contextWindow: Int,

    /** 模型支持的单次最大输出 Token 数 */
    @ColumnInfo(name = "max_output_tokens")
    val maxOutputTokens: Int,

    /** 是否支持视觉功能（例如是否可以输入图片） */
    @ColumnInfo(name = "support_vision")
    val supportVision: Boolean,

    /** 是否支持工具调用（函数调用） */
    @ColumnInfo(name = "support_tool_call")
    val supportToolCall: Boolean,

    /** 是否支持推理思维过程（例如一些推理模型） */
    @ColumnInfo(name = "support_reasoning")
    val supportReasoning: Boolean,

    /** 采样温度（通常在 0.0 到 2.0 之间），用于控制生成文本的随机性与创造力 */
    val temperature: Double,

    /** 核采样阈值（通常在 0.0 到 1.0 之间），控制生成候选词的累积概率范围 */
    @ColumnInfo(name = "top_p")
    val topP: Double,

    /** 该配置是否已启用 */
    val enabled: Boolean,

    /** 记录的创建时间戳（毫秒） */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** 记录的最后修改时间戳（毫秒） */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    /** 记录的软删除时间戳（毫秒），为 null 表示未被删除 */
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
)
