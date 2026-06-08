package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.ModelConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {
    @Upsert
    suspend fun upsert(config: ModelConfigEntity)

    @Query(
        "SELECT * FROM model_config " +
            "WHERE enabled = 1 AND deleted_at IS NULL ORDER BY name",
    )
    fun observeEnabled(): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_config WHERE id = :id AND deleted_at IS NULL")
    suspend fun findById(id: String): ModelConfigEntity?

    @Query("UPDATE model_config SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long): Int

    @Query("DELETE FROM model_config WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
