package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.AgentRoleEntity
import com.kaixuan.starrailchatbox.data.database.entity.AgentRoleSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentRoleDao {
    @Upsert
    suspend fun upsert(role: AgentRoleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(roles: List<AgentRoleEntity>)

    @Query("""
        SELECT *, (SELECT MAX(last_message_at) FROM chat_session WHERE agent_id = agent_role.id AND deleted_at IS NULL) as last_message_at 
        FROM agent_role 
        WHERE deleted_at IS NULL 
        ORDER BY sort_order, created_at
    """)
    suspend fun findAll(): List<AgentRoleSummaryEntity>

    @Query("""
        SELECT *, (SELECT MAX(last_message_at) FROM chat_session WHERE agent_id = agent_role.id AND deleted_at IS NULL) as last_message_at
        FROM agent_role 
        WHERE deleted_at IS NULL 
        ORDER BY sort_order, created_at
    """)
    fun observeAll(): Flow<List<AgentRoleSummaryEntity>>

    @Query("SELECT * FROM agent_role WHERE id = :id AND deleted_at IS NULL")
    suspend fun findById(id: String): AgentRoleEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM agent_role WHERE id = :id)")
    suspend fun containsId(id: String): Boolean

    @Query("SELECT MAX(sort_order) FROM agent_role WHERE deleted_at IS NULL")
    suspend fun findMaxSortOrder(): Int?

    @Query("UPDATE agent_role SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: Long): Int

    @Query("UPDATE agent_role SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long): Int
}
