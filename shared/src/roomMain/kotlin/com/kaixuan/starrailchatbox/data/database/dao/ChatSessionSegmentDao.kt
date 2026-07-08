package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionSegmentEntity

@Dao
interface ChatSessionSegmentDao {
    @Upsert
    suspend fun upsertAll(segments: List<ChatSessionSegmentEntity>)

    @Query(
        """
        SELECT * FROM chat_session_segment
        WHERE owner_session_id = :ownerSessionId
        ORDER BY segment_index ASC
        """,
    )
    suspend fun findByOwner(ownerSessionId: String): List<ChatSessionSegmentEntity>
}
