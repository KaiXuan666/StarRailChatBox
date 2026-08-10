package com.kaixuan.starrailchatbox.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_model")
data class LocalModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    val sha256: String,
    val source: String,
    @ColumnInfo(name = "source_url") val sourceUrl: String?,
    val license: String,
    @ColumnInfo(name = "context_window") val contextWindow: Int,
    @ColumnInfo(name = "max_output_tokens") val maxOutputTokens: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
