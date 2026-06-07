package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    @Upsert
    suspend fun upsert(session: ChatSessionEntity)

    @Query(
        "SELECT * FROM chat_session " +
            "WHERE archived = 0 AND deleted_at IS NULL " +
            "ORDER BY pinned DESC, last_message_at DESC",
    )
    fun observeActive(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_session WHERE id = :id AND deleted_at IS NULL")
    suspend fun findById(id: String): ChatSessionEntity?

    @Query("UPDATE chat_session SET archived = :archived, updated_at = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long): Int

    @Query("UPDATE chat_session SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long): Int
}
