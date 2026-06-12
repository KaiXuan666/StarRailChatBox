package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation

data class ChatMessagePageRow(
    @Embedded
    val message: ChatMessageEntity,
    @ColumnInfo(name = "has_failed_response")
    val hasFailedResponse: Boolean,
    @Relation(
        parentColumn = "id",
        entityColumn = "message_id",
    )
    val attachments: List<MessageAttachmentEntity>,
)
