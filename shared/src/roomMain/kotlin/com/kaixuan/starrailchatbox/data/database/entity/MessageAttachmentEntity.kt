package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment

@Entity(
    tableName = "message_attachment",
    foreignKeys = [
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["message_id"]),
    ]
)
data class MessageAttachmentEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "message_id")
    val messageId: String,
    val name: String,
    val size: Long,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    val uri: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

fun MessageAttachmentEntity.toDomain() = MessageAttachment(
    id = id,
    messageId = messageId,
    name = name,
    size = size,
    mimeType = mimeType,
    uri = uri,
    createdAt = createdAt,
)

fun MessageAttachment.toEntity() = MessageAttachmentEntity(
    id = id,
    messageId = messageId,
    name = name,
    size = size,
    mimeType = mimeType,
    uri = uri,
    createdAt = createdAt,
)
