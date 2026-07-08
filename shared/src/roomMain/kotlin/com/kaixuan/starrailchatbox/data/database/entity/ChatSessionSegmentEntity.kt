package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 描述某个会话可见历史路径中的一个消息片段。
 *
 * 普通会话只有一个开放片段，owner 和 source 都是自己。分支会话会复制父会话的历史片段，
 * 并在分叉点处截断目标片段，然后追加一个属于分支自己的开放片段，用于承载分支中新产生
 * 的消息。
 *
 * 片段行只保存消息序号范围，不复制消息正文或附件记录。这样多个分支可以共享不可变历史，
 * 同时各自独立追加新消息。
 */
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
    /**
     * 拥有该可见历史片段的会话 ID。
     */
    @ColumnInfo(name = "owner_session_id")
    val ownerSessionId: String,
    /**
     * 片段在 owner 会话中的顺序，从旧到新递增。
     */
    @ColumnInfo(name = "segment_index")
    val segmentIndex: Int,
    /**
     * 被引用消息实际存储所在的源会话 ID。
     */
    @ColumnInfo(name = "source_session_id")
    val sourceSessionId: String,
    /**
     * 源会话中该片段包含的起始消息序号，闭区间。
     */
    @ColumnInfo(name = "from_seq")
    val fromSeq: Long,
    /**
     * 源会话中该片段包含的结束消息序号，闭区间。
     * 为 null 表示这是 owner 会话自己的开放片段，会随着新消息继续增长。
     */
    @ColumnInfo(name = "to_seq")
    val toSeq: Long? = null,
)
