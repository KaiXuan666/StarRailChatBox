package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.LocalModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalModelDao {
    @Query("SELECT * FROM local_model ORDER BY created_at")
    fun observeAll(): Flow<List<LocalModelEntity>>

    @Query("SELECT * FROM local_model WHERE id = :id")
    suspend fun findById(id: String): LocalModelEntity?

    @Upsert
    suspend fun upsert(model: LocalModelEntity)

    @Query("DELETE FROM local_model WHERE id = :id")
    suspend fun deleteById(id: String)
}
