package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ChatMessageWithAttachments(
    @Embedded
    val message: ChatMessageEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "message_id"
    )
    val attachments: List<MessageAttachmentEntity>
)
