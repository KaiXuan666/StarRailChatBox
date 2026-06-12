package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class ChatSessionSummaryRow(
    @Embedded
    val session: ChatSessionEntity,
    @ColumnInfo(name = "last_message_preview")
    val lastMessagePreview: String,
    @ColumnInfo(name = "message_count")
    val messageCount: Int,
)
