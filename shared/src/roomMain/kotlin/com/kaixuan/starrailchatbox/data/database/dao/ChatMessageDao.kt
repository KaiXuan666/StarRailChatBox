package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.paging.PagingSource
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessageEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessagePageRow
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessageWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Upsert
    suspend fun upsert(message: ChatMessageEntity)

    @Upsert
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    @Transaction
    @Query(
        """
        SELECT m.*,
            EXISTS(
                SELECT 1 FROM chat_message failed
                WHERE failed.session_id = m.session_id
                    AND failed.seq = m.seq + 1
                    AND failed.role = 'assistant'
                    AND failed.status = 'failed'
                    AND failed.deleted_at IS NULL
            ) AS has_failed_response
        FROM chat_message m
        WHERE m.session_id = :sessionId
            AND m.deleted_at IS NULL
            AND NOT (m.role = 'assistant' AND m.status = 'failed')
        ORDER BY m.seq DESC
        """,
    )
    fun pagingSourceBySession(sessionId: String): PagingSource<Int, ChatMessagePageRow>

    @Query(
        """
        SELECT COUNT(*) FROM chat_message
        WHERE session_id = :sessionId
            AND deleted_at IS NULL
            AND NOT (role = 'assistant' AND status = 'failed')
        """,
    )
    suspend fun visibleMessageCount(sessionId: String): Int

    @Query(
        """
        SELECT CASE
            WHEN role = 'assistant' AND status = 'completed' THEN suggestions_json
            ELSE NULL
        END
        FROM chat_message
        WHERE session_id = :sessionId
            AND deleted_at IS NULL
        ORDER BY seq DESC
        LIMIT 1
        """,
    )
    fun observeLatestSuggestionsJson(sessionId: String): Flow<String?>

    @Transaction
    @Query("SELECT * FROM chat_message WHERE id = :messageId AND deleted_at IS NULL")
    suspend fun findById(messageId: String): ChatMessageWithAttachments?

    @Query("SELECT EXISTS(SELECT 1 FROM chat_message WHERE id = :messageId)")
    suspend fun exists(messageId: String): Boolean

    @Transaction
    @Query(
        "SELECT * FROM chat_message " +
            "WHERE session_id = :sessionId AND is_context_excluded = 0 " +
            "AND status = 'completed' AND deleted_at IS NULL AND seq > :afterSeq " +
            "ORDER BY seq DESC LIMIT :limit",
    )
    suspend fun findRecentContext(
        sessionId: String,
        afterSeq: Long,
        limit: Int,
    ): List<ChatMessageWithAttachments>

    @Query("SELECT COALESCE(MAX(seq), 0) + 1 FROM chat_message WHERE session_id = :sessionId")
    suspend fun nextSeq(sessionId: String): Long

    @Query("UPDATE chat_message SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long): Int

    @Query("DELETE FROM chat_message WHERE session_id = :sessionId AND status = 'failed'")
    suspend fun deleteFailedMessages(sessionId: String): Int
}
