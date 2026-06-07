package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Upsert
    suspend fun upsert(message: ChatMessageEntity)

    @Query(
        "SELECT * FROM chat_message " +
            "WHERE session_id = :sessionId AND deleted_at IS NULL ORDER BY seq",
    )
    fun observeBySession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query(
        "SELECT * FROM chat_message " +
            "WHERE session_id = :sessionId AND is_context_excluded = 0 " +
            "AND deleted_at IS NULL ORDER BY seq DESC LIMIT :limit",
    )
    suspend fun findRecentContext(sessionId: String, limit: Int): List<ChatMessageEntity>

    @Query("SELECT COALESCE(MAX(seq), 0) + 1 FROM chat_message WHERE session_id = :sessionId")
    suspend fun nextSeq(sessionId: String): Long

    @Query("UPDATE chat_message SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long): Int
}
