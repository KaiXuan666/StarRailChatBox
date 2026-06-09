package com.kaixuan.starrailchatbox.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.kaixuan.starrailchatbox.data.database.dao.AgentRoleDao
import com.kaixuan.starrailchatbox.data.database.dao.ChatMessageDao
import com.kaixuan.starrailchatbox.data.database.dao.ChatSessionDao
import com.kaixuan.starrailchatbox.data.database.dao.ChatSummaryDao
import com.kaixuan.starrailchatbox.data.database.dao.ModelConfigDao
import com.kaixuan.starrailchatbox.data.database.dao.MessageAttachmentDao
import com.kaixuan.starrailchatbox.data.database.entity.AgentRoleEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatMessageEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionEntity
import com.kaixuan.starrailchatbox.data.database.entity.ChatSummaryEntity
import com.kaixuan.starrailchatbox.data.database.entity.ModelConfigEntity
import com.kaixuan.starrailchatbox.data.database.entity.MessageAttachmentEntity

@Database(
    entities = [
        AgentRoleEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        ChatSummaryEntity::class,
        ModelConfigEntity::class,
        MessageAttachmentEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@ConstructedBy(StarRailDatabaseConstructor::class)
abstract class StarRailDatabase : RoomDatabase() {
    abstract fun agentRoleDao(): AgentRoleDao

    abstract fun chatSessionDao(): ChatSessionDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun chatSummaryDao(): ChatSummaryDao

    abstract fun modelConfigDao(): ModelConfigDao

    abstract fun messageAttachmentDao(): MessageAttachmentDao
}

@Suppress("KotlinNoActualForExpect")
expect object StarRailDatabaseConstructor : RoomDatabaseConstructor<StarRailDatabase> {
    override fun initialize(): StarRailDatabase
}
