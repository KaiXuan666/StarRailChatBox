package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.ChatSummaryEntity

@Dao
interface ChatSummaryDao {
    @Upsert
    suspend fun upsert(summary: ChatSummaryEntity)

    @Delete
    suspend fun delete(summary: ChatSummaryEntity)

    @Query(
        "SELECT summary.* FROM chat_summary summary " +
            "INNER JOIN chat_session session ON session.active_summary_id = summary.id " +
            "WHERE session.id = :sessionId AND session.deleted_at IS NULL LIMIT 1",
    )
    suspend fun findActive(sessionId: String): ChatSummaryEntity?
}
