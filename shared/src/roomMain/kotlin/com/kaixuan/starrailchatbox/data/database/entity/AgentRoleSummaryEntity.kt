package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo

data class AgentRoleSummaryEntity(
    val id: String,
    val name: String,
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String,
    val description: String,
    @ColumnInfo(name = "opening_message")
    val openingMessage: String,
    val temperature: Double,
    @ColumnInfo(name = "top_p")
    val topP: Double,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "is_builtin")
    val isBuiltin: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "voice_sample_uri")
    val voiceSampleUri: String? = null,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "last_message_at")
    val lastMessageAt: Long? = null,
)
