package com.kaixuan.starrailchatbox.data.localmodel

import com.kaixuan.starrailchatbox.data.database.dao.LocalModelDao
import com.kaixuan.starrailchatbox.data.database.entity.LocalModelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLocalModelRepository(
    private val dao: LocalModelDao,
) : LocalModelRepository {
    override fun observeModels(): Flow<List<LocalModel>> = dao.observeAll().map { rows ->
        rows.map(LocalModelEntity::toDomain)
    }

    override suspend fun getById(id: String): LocalModel? = dao.findById(id)?.toDomain()

    override suspend fun upsert(model: LocalModel) = dao.upsert(model.toEntity())

    override suspend fun delete(id: String) = dao.deleteById(id)
}

private fun LocalModelEntity.toDomain() = LocalModel(
    id, name, filePath, sizeBytes, sha256, LocalModelSource.valueOf(source), sourceUrl,
    license, contextWindow, maxOutputTokens, createdAt, updatedAt,
)

private fun LocalModel.toEntity() = LocalModelEntity(
    id, name, filePath, sizeBytes, sha256, source.name, sourceUrl,
    license, contextWindow, maxOutputTokens, createdAt, updatedAt,
)
