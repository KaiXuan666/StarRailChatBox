package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chat_session_hidden_message",
    primaryKeys = ["owner_session_id", "message_id"],
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["owner_session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["owner_session_id", "message_id"]),
        Index(value = ["message_id"]),
    ],
)
data class ChatSessionHiddenMessageEntity(
    @ColumnInfo(name = "owner_session_id")
    val ownerSessionId: String,
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "hidden_at")
    val hiddenAt: Long,
    val reason: String,
)
