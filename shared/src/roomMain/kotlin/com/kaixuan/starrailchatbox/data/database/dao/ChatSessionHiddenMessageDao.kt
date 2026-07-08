package com.kaixuan.starrailchatbox.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionHiddenMessageEntity

@Dao
interface ChatSessionHiddenMessageDao {
    @Upsert
    suspend fun upsert(hiddenMessage: ChatSessionHiddenMessageEntity)

    @Query("SELECT COUNT(*) FROM chat_session_hidden_message WHERE owner_session_id = :ownerSessionId")
    suspend fun countByOwner(ownerSessionId: String): Int
}
