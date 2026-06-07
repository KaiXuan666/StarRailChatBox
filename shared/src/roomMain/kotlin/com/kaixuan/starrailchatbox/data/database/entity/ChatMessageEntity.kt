package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_message",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_message_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ModelConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_config_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["session_id", "seq"], unique = true),
        Index(value = ["parent_message_id"]),
        Index(value = ["model_config_id"]),
        Index(value = ["session_id", "deleted_at", "seq"]),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "parent_message_id")
    val parentMessageId: String? = null,
    val seq: Long,
    val role: String,
    val content: String,
    @ColumnInfo(name = "reasoning_content")
    val reasoningContent: String? = null,
    val status: String,
    @ColumnInfo(name = "error_code")
    val errorCode: String? = null,
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    @ColumnInfo(name = "model_config_id")
    val modelConfigId: String? = null,
    @ColumnInfo(name = "model_name_snapshot")
    val modelNameSnapshot: String? = null,
    @ColumnInfo(name = "prompt_tokens")
    val promptTokens: Int,
    @ColumnInfo(name = "completion_tokens")
    val completionTokens: Int,
    @ColumnInfo(name = "total_tokens")
    val totalTokens: Int,
    @ColumnInfo(name = "estimated_tokens")
    val estimatedTokens: Int,
    @ColumnInfo(name = "is_context_excluded")
    val isContextExcluded: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
)
