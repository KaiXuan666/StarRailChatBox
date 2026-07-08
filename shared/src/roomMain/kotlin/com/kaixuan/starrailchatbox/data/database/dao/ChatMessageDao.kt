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

    @Transaction
    @Query(
        """
        SELECT m.*,
            CASE WHEN m.role = 'user' AND (
                EXISTS(
                    SELECT 1 FROM chat_message failed
                    WHERE failed.session_id = m.session_id
                        AND failed.seq = m.seq + 1
                        AND failed.role = 'assistant'
                        AND failed.status = 'failed'
                        AND failed.deleted_at IS NULL
                )
                OR m.seq = (
                    SELECT latest.seq FROM chat_message latest
                    WHERE latest.session_id = m.session_id
                        AND latest.deleted_at IS NULL
                        AND NOT (latest.role = 'assistant' AND latest.status = 'failed')
                    ORDER BY latest.seq DESC
                    LIMIT 1
                )
            ) THEN 1 ELSE 0 END AS has_failed_response
        FROM chat_message m
        WHERE m.session_id = :sessionId
            AND m.deleted_at IS NULL
            AND NOT (m.role = 'assistant' AND m.status = 'failed')
        ORDER BY m.seq DESC
        """,
    )
    fun pagingSourceBySession(sessionId: String): PagingSource<Int, ChatMessagePageRow>

    @Transaction
    @Query(
        """
        SELECT m.*,
            CASE WHEN m.role = 'user' AND (
                EXISTS(
                    SELECT 1 FROM chat_session_segment failed_segment
                    INNER JOIN chat_message failed
                        ON failed.session_id = failed_segment.source_session_id
                        AND failed.seq >= failed_segment.from_seq
                        AND (failed_segment.to_seq IS NULL OR failed.seq <= failed_segment.to_seq)
                    WHERE failed_segment.owner_session_id = :sessionId
                        AND failed.session_id = m.session_id
                        AND failed.seq = m.seq + 1
                        AND failed.role = 'assistant'
                        AND failed.status = 'failed'
                        AND failed.deleted_at IS NULL
                        AND NOT EXISTS (
                            SELECT 1 FROM chat_session_hidden_message hidden_failed
                            WHERE hidden_failed.owner_session_id = :sessionId
                                AND hidden_failed.message_id = failed.id
                        )
                )
                OR m.id = (
                    SELECT latest.id FROM chat_session_segment latest_segment
                    INNER JOIN chat_message latest
                        ON latest.session_id = latest_segment.source_session_id
                        AND latest.seq >= latest_segment.from_seq
                        AND (latest_segment.to_seq IS NULL OR latest.seq <= latest_segment.to_seq)
                    WHERE latest_segment.owner_session_id = :sessionId
                        AND latest.deleted_at IS NULL
                        AND NOT (latest.role = 'assistant' AND latest.status = 'failed')
                        AND NOT EXISTS (
                            SELECT 1 FROM chat_session_hidden_message hidden_latest
                            WHERE hidden_latest.owner_session_id = :sessionId
                                AND hidden_latest.message_id = latest.id
                        )
                    ORDER BY latest_segment.segment_index DESC, latest.seq DESC
                    LIMIT 1
                )
            ) THEN 1 ELSE 0 END AS has_failed_response
        FROM chat_session_segment segment
        INNER JOIN chat_message m
            ON m.session_id = segment.source_session_id
            AND m.seq >= segment.from_seq
            AND (segment.to_seq IS NULL OR m.seq <= segment.to_seq)
        WHERE segment.owner_session_id = :sessionId
            AND m.deleted_at IS NULL
            AND NOT (m.role = 'assistant' AND m.status = 'failed')
            AND NOT EXISTS (
                SELECT 1 FROM chat_session_hidden_message hidden
                WHERE hidden.owner_session_id = :sessionId
                    AND hidden.message_id = m.id
            )
        ORDER BY segment.segment_index DESC, m.seq DESC
        """,
    )
    fun pagingSourceByVisibleSession(sessionId: String): PagingSource<Int, ChatMessagePageRow>

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
        SELECT COUNT(*) FROM chat_session_segment segment
        INNER JOIN chat_message m
            ON m.session_id = segment.source_session_id
            AND m.seq >= segment.from_seq
            AND (segment.to_seq IS NULL OR m.seq <= segment.to_seq)
        WHERE segment.owner_session_id = :sessionId
            AND m.deleted_at IS NULL
            AND NOT (m.role = 'assistant' AND m.status = 'failed')
            AND NOT EXISTS (
                SELECT 1 FROM chat_session_hidden_message hidden
                WHERE hidden.owner_session_id = :sessionId
                    AND hidden.message_id = m.id
            )
        """,
    )
    suspend fun visibleMessageCountByVisibleSession(sessionId: String): Int

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

    @Query(
        """
        SELECT CASE
            WHEN latest.role = 'assistant' AND latest.status = 'completed' THEN latest.suggestions_json
            ELSE NULL
        END
        FROM chat_session_segment segment
        INNER JOIN chat_message latest
            ON latest.session_id = segment.source_session_id
            AND latest.seq >= segment.from_seq
            AND (segment.to_seq IS NULL OR latest.seq <= segment.to_seq)
        WHERE segment.owner_session_id = :sessionId
            AND latest.deleted_at IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM chat_session_hidden_message hidden
                WHERE hidden.owner_session_id = :sessionId
                    AND hidden.message_id = latest.id
            )
        ORDER BY segment.segment_index DESC, latest.seq DESC
        LIMIT 1
        """,
    )
    fun observeLatestSuggestionsJsonByVisibleSession(sessionId: String): Flow<String?>

    @Transaction
    @Query("SELECT * FROM chat_message WHERE id = :messageId AND deleted_at IS NULL")
    suspend fun findById(messageId: String): ChatMessageWithAttachments?

    @Transaction
    @Query(
        """
        SELECT m.* FROM chat_session_segment segment
        INNER JOIN chat_message m
            ON m.session_id = segment.source_session_id
            AND m.seq >= segment.from_seq
            AND (segment.to_seq IS NULL OR m.seq <= segment.to_seq)
        WHERE segment.owner_session_id = :sessionId
            AND m.id = :messageId
            AND m.deleted_at IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM chat_session_hidden_message hidden
                WHERE hidden.owner_session_id = :sessionId
                    AND hidden.message_id = m.id
            )
        LIMIT 1
        """,
    )
    suspend fun findVisibleById(
        sessionId: String,
        messageId: String,
    ): ChatMessageWithAttachments?

    @Transaction
    @Query(
        """
        SELECT * FROM chat_message
        WHERE session_id = :sessionId
            AND deleted_at IS NULL
        ORDER BY seq DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestBySession(sessionId: String): ChatMessageWithAttachments?

    @Transaction
    @Query(
        """
        SELECT m.* FROM chat_session_segment segment
        INNER JOIN chat_message m
            ON m.session_id = segment.source_session_id
            AND m.seq >= segment.from_seq
            AND (segment.to_seq IS NULL OR m.seq <= segment.to_seq)
        WHERE segment.owner_session_id = :sessionId
            AND m.deleted_at IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM chat_session_hidden_message hidden
                WHERE hidden.owner_session_id = :sessionId
                    AND hidden.message_id = m.id
            )
        ORDER BY segment.segment_index DESC, m.seq DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestByVisibleSession(sessionId: String): ChatMessageWithAttachments?

    @Transaction
    @Query(
        """
        SELECT m.* FROM chat_session_segment segment
        INNER JOIN chat_message m
            ON m.session_id = segment.source_session_id
            AND m.seq >= segment.from_seq
            AND (segment.to_seq IS NULL OR m.seq <= segment.to_seq)
        WHERE segment.owner_session_id = :sessionId
            AND m.deleted_at IS NULL
            AND NOT (m.role = 'assistant' AND m.status = 'failed')
            AND NOT EXISTS (
                SELECT 1 FROM chat_session_hidden_message hidden
                WHERE hidden.owner_session_id = :sessionId
                    AND hidden.message_id = m.id
            )
        ORDER BY segment.segment_index DESC, m.seq DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestDisplayByVisibleSession(sessionId: String): ChatMessageWithAttachments?

    @Transaction
    @Query(
        "SELECT * FROM chat_message " +
            "WHERE session_id = :sessionId AND is_context_excluded = 0 " +
            "AND status = 'completed' AND deleted_at IS NULL AND seq > :afterSeq " +
            "AND NOT EXISTS (" +
            "SELECT 1 FROM chat_session_hidden_message hidden " +
            "WHERE hidden.owner_session_id = :sessionId AND hidden.message_id = chat_message.id" +
            ") " +
            "ORDER BY seq DESC LIMIT :limit",
    )
    suspend fun findRecentContext(
        sessionId: String,
        afterSeq: Long,
        limit: Int,
    ): List<ChatMessageWithAttachments>

    @Transaction
    @Query(
        """
        SELECT m.* FROM chat_session_segment segment
        INNER JOIN chat_message m
            ON m.session_id = segment.source_session_id
            AND m.seq >= segment.from_seq
            AND (segment.to_seq IS NULL OR m.seq <= segment.to_seq)
        WHERE segment.owner_session_id = :sessionId
            AND m.is_context_excluded = 0
            AND m.status = 'completed'
            AND m.deleted_at IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM chat_session_hidden_message hidden
                WHERE hidden.owner_session_id = :sessionId
                    AND hidden.message_id = m.id
            )
        ORDER BY segment.segment_index DESC, m.seq DESC
        LIMIT :limit
        """,
    )
    suspend fun findRecentVisibleContext(
        sessionId: String,
        limit: Int,
    ): List<ChatMessageWithAttachments>

    @Query("SELECT COALESCE(MAX(seq), 0) + 1 FROM chat_message WHERE session_id = :sessionId")
    suspend fun nextSeq(sessionId: String): Long

    @Query("UPDATE chat_message SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long): Int

    @Query("DELETE FROM chat_message WHERE session_id = :sessionId AND status = 'failed'")
    suspend fun deleteFailedMessages(sessionId: String): Int
}
