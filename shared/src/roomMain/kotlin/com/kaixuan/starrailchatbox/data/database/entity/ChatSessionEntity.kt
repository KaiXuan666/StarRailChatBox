package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_session",
    foreignKeys = [
        ForeignKey(
            entity = AgentRoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["agent_id"],
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
        Index(value = ["agent_id"]),
        Index(value = ["model_config_id"]),
        Index(value = ["pinned", "archived", "deleted_at", "last_message_at"]),
    ],
)
data class ChatSessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    @ColumnInfo(name = "agent_id")
    val agentId: String? = null,
    @ColumnInfo(name = "model_config_id")
    val modelConfigId: String? = null,
    @ColumnInfo(name = "system_prompt_snapshot")
    val systemPromptSnapshot: String,
    @ColumnInfo(name = "custom_system_prompt")
    val customSystemPrompt: String? = null,
    @ColumnInfo(name = "max_context_message_count")
    val maxContextMessageCount: Int,
    @ColumnInfo(name = "enable_summary")
    val enableSummary: Boolean,
    @ColumnInfo(name = "summary_threshold_tokens")
    val summaryThresholdTokens: Int,
    @ColumnInfo(name = "summary_threshold_message_count", defaultValue = "20")
    val summaryThresholdMessageCount: Int,
    @ColumnInfo(name = "summary_retained_message_count", defaultValue = "8")
    val summaryRetainedMessageCount: Int,
    @ColumnInfo(name = "active_summary_id")
    val activeSummaryId: String? = null,
    @ColumnInfo(name = "compaction_seq")
    val compactionSeq: Long,
    @ColumnInfo(name = "last_message_id")
    val lastMessageId: String? = null,
    @ColumnInfo(name = "last_message_at")
    val lastMessageAt: Long,
    val pinned: Boolean,
    val archived: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
)
