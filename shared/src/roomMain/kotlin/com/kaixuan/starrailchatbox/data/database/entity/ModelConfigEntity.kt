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
data class ModelConfigEntity(
    @PrimaryKey
    val id: String,
    val provider: String,
    val name: String,
    @ColumnInfo(name = "base_url")
    val baseUrl: String,
    @ColumnInfo(name = "api_key_encrypted")
    val apiKeyEncrypted: String? = null,
    @ColumnInfo(name = "model_name")
    val modelName: String,
    @ColumnInfo(name = "context_window")
    val contextWindow: Int,
    @ColumnInfo(name = "max_output_tokens")
    val maxOutputTokens: Int,
    @ColumnInfo(name = "support_vision")
    val supportVision: Boolean,
    @ColumnInfo(name = "support_tool_call")
    val supportToolCall: Boolean,
    @ColumnInfo(name = "support_reasoning")
    val supportReasoning: Boolean,
    val temperature: Double,
    @ColumnInfo(name = "top_p")
    val topP: Double,
    val enabled: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
)
