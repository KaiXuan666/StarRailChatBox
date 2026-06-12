package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo

data class AgentRoleSummaryEntity(
    val id: String,
    val name: String,
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "last_message_at")
    val lastMessageAt: Long? = null,
)
