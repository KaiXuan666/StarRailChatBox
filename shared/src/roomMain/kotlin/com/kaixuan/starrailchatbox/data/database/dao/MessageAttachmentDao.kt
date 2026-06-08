package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.MessageAttachmentEntity

@Dao
interface MessageAttachmentDao {
    @Upsert
    suspend fun upsert(attachment: MessageAttachmentEntity)

    @Upsert
    suspend fun insertAll(attachments: List<MessageAttachmentEntity>)
}
