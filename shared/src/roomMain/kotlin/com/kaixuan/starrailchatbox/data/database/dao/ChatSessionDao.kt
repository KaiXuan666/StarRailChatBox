package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionSummaryRow
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

    @Query(
        """
        SELECT s.*,
            COALESCE((
                SELECT m.content FROM chat_message m
                WHERE m.session_id = s.id
                    AND m.status = 'completed'
                    AND m.deleted_at IS NULL
                    AND m.content != ''
                ORDER BY m.seq DESC
                LIMIT 1
            ), '') AS last_message_preview,
            (
                SELECT COUNT(*) FROM chat_message m
                WHERE m.session_id = s.id
                    AND m.status = 'completed'
                    AND m.deleted_at IS NULL
            ) AS message_count
        FROM chat_session s
        WHERE s.agent_id = :agentId
            AND s.archived = 0
            AND s.deleted_at IS NULL
        ORDER BY s.pinned DESC, s.last_message_at DESC
        """,
    )
    fun observeSummariesByAgent(agentId: String): Flow<List<ChatSessionSummaryRow>>

    @Query(
        "SELECT * FROM chat_session " +
            "WHERE agent_id = :agentId AND archived = 0 AND deleted_at IS NULL " +
            "ORDER BY last_message_at DESC LIMIT 1",
    )
    suspend fun findLatestByAgent(agentId: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_session WHERE id = :id AND deleted_at IS NULL")
    suspend fun findById(id: String): ChatSessionEntity?

    @Query(
        "UPDATE chat_session SET last_message_id = :messageId, last_message_at = :messageAt, " +
            "updated_at = :messageAt WHERE id = :sessionId",
    )
    suspend fun updateLastMessage(
        sessionId: String,
        messageId: String,
        messageAt: Long,
    ): Int

    @Query(
        "UPDATE chat_session SET active_summary_id = :summaryId, compaction_seq = :toSeq, " +
            "updated_at = :updatedAt WHERE id = :sessionId AND compaction_seq < :toSeq",
    )
    suspend fun activateSummary(
        sessionId: String,
        summaryId: String,
        toSeq: Long,
        updatedAt: Long,
    ): Int

    @Query("UPDATE chat_session SET archived = :archived, updated_at = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long): Int

    @Query("UPDATE chat_session SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long): Int

    @Query("UPDATE chat_session SET title = :title, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long): Int
}
