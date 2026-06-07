package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.AgentRoleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentRoleDao {
    @Upsert
    suspend fun upsert(role: AgentRoleEntity)

    @Query("SELECT * FROM agent_role WHERE deleted_at IS NULL ORDER BY sort_order, created_at")
    fun observeAll(): Flow<List<AgentRoleEntity>>

    @Query("SELECT * FROM agent_role WHERE id = :id AND deleted_at IS NULL")
    suspend fun findById(id: String): AgentRoleEntity?

    @Query("UPDATE agent_role SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long): Int
}
