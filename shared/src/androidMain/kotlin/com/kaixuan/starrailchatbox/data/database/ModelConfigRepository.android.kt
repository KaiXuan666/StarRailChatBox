package com.kaixuan.starrailchatbox.data.database

import android.content.Context
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.character.DefaultCharacterRepository
import com.kaixuan.starrailchatbox.data.character.RoomCharacterStorage
import com.kaixuan.starrailchatbox.data.chat.RoomChatSessionRepository
import com.kaixuan.starrailchatbox.data.model.RoomModelConfigRepository
import com.kaixuan.starrailchatbox.data.settings.createAppSettingsStore
import com.kaixuan.starrailchatbox.data.settings.createProfileStore

object AndroidContextHolder {
    var context: android.content.Context? = null
}

fun createPersistentRepositories(
    context: Context,
    databaseName: String = "starrail_chat_box.db",
): PersistentRepositories {
    AndroidContextHolder.context = context.applicationContext
    val database = Room.databaseBuilder<StarRailDatabase>(
        context = context.applicationContext,
        name = databaseName,
    )
        .setDriver(BundledSQLiteDriver())
        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // 强制使用 TRUNCATE 模式，避免 WAL 锁死
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .build()
    val keyStorePath = context.filesDir.resolve("api_key.key.preferences_pb").absolutePath
    val databaseFile = context.getDatabasePath(databaseName)
    return PersistentRepositories(
        modelConfigRepository = RoomModelConfigRepository(
            dao = database.modelConfigDao(),
            cipher = createApiKeyCipher(keyStorePath),
        ),
        characterRepository = DefaultCharacterRepository(
            RoomCharacterStorage(
                dao = database.agentRoleDao(),
            ),
        ),
        chatSessionRepository = RoomChatSessionRepository(database),
        profileStore = createProfileStore(
            path = context.filesDir.resolve("profile_settings.preferences_pb").absolutePath,
            context = context,
        ),
        appSettingsStore = createAppSettingsStore(
            path = context.filesDir.resolve("app_settings.preferences_pb").absolutePath,
            context = context,
        ),
        databaseManager = RoomDatabaseManager(database, databaseFile.absolutePath),
    )
}
