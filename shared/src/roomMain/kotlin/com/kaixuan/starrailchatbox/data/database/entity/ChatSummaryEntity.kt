package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_summary",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ModelConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_config_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["session_id", "to_seq"], unique = true),
        Index(value = ["model_config_id"]),
    ],
)
data class ChatSummaryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "from_seq")
    val fromSeq: Long,
    @ColumnInfo(name = "to_seq")
    val toSeq: Long,
    val content: String,
    @ColumnInfo(name = "source_message_count")
    val sourceMessageCount: Int,
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
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
