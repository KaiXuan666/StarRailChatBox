package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agent_role",
    indices = [
        Index(value = ["name"]),
        Index(value = ["sort_order", "created_at"]),
        Index(value = ["deleted_at"]),
    ],
)
data class AgentRoleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String,
    val description: String,
    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String,
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
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
)
