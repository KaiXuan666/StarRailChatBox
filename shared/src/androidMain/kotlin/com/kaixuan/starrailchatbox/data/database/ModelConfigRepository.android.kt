package com.kaixuan.starrailchatbox.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.character.DefaultCharacterRepository
import com.kaixuan.starrailchatbox.data.character.FileCharacterAvatarStorage
import com.kaixuan.starrailchatbox.data.character.RoomCharacterStorage
import com.kaixuan.starrailchatbox.data.chat.RoomChatSessionRepository
import com.kaixuan.starrailchatbox.data.model.RoomModelConfigRepository

fun createPersistentRepositories(
    context: Context,
    databaseName: String = "starrail_chat_box.db",
): PersistentRepositories {
    val database = Room.databaseBuilder<StarRailDatabase>(
        context = context.applicationContext,
        name = databaseName,
    )
        .setDriver(BundledSQLiteDriver())
//        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // 强制使用 TRUNCATE 模式，避免 WAL 锁死
        .addMigrations(MIGRATION_1_2)
        .build()
    val keyStorePath = context.filesDir.resolve("api_key.key.preferences_pb").absolutePath
    return PersistentRepositories(
        modelConfigRepository = RoomModelConfigRepository(
            dao = database.modelConfigDao(),
            cipher = createApiKeyCipher(keyStorePath),
        ),
        characterRepository = DefaultCharacterRepository(
            RoomCharacterStorage(
                dao = database.agentRoleDao(),
                avatarStorage = FileCharacterAvatarStorage(
                    context.filesDir.resolve("character_avatars"),
                ),
            ),
        ),
        chatSessionRepository = RoomChatSessionRepository(database),
    )
}
