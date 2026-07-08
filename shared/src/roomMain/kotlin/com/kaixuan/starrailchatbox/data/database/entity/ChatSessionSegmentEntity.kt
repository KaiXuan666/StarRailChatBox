package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chat_session_segment",
    primaryKeys = ["owner_session_id", "segment_index"],
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["owner_session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_session_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["owner_session_id", "segment_index"]),
        Index(value = ["source_session_id"]),
    ],
)
data class ChatSessionSegmentEntity(
    @ColumnInfo(name = "owner_session_id")
    val ownerSessionId: String,
    @ColumnInfo(name = "segment_index")
    val segmentIndex: Int,
    @ColumnInfo(name = "source_session_id")
    val sourceSessionId: String,
    @ColumnInfo(name = "from_seq")
    val fromSeq: Long,
    @ColumnInfo(name = "to_seq")
    val toSeq: Long? = null,
)
