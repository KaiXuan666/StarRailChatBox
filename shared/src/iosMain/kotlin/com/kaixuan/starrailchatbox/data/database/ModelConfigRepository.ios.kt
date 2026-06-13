@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kaixuan.starrailchatbox.data.database

import androidx.room.Room
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.character.DefaultCharacterRepository
import com.kaixuan.starrailchatbox.data.character.RoomCharacterStorage
import com.kaixuan.starrailchatbox.data.chat.RoomChatSessionRepository
import com.kaixuan.starrailchatbox.data.model.RoomModelConfigRepository
import com.kaixuan.starrailchatbox.data.settings.createAppSettingsStore
import com.kaixuan.starrailchatbox.data.settings.createProfileStore
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import okio.Path.Companion.toPath

fun createPersistentRepositories(): PersistentRepositories {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val directoryPath = requireNotNull(directory?.path)
    val databasePath = "$directoryPath/starrail_chat_box.db"
    val database = Room.databaseBuilder<StarRailDatabase>(
        name = databasePath,
        factory = StarRailDatabaseConstructor::initialize,
    )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .build()
    return PersistentRepositories(
        modelConfigRepository = RoomModelConfigRepository(
            dao = database.modelConfigDao(),
            cipher = createApiKeyCipher("$directoryPath/api_key.key.preferences_pb"),
        ),
        characterRepository = DefaultCharacterRepository(
            RoomCharacterStorage(
                dao = database.agentRoleDao(),
            ),
        ),
        chatSessionRepository = RoomChatSessionRepository(database),
        profileStore = createProfileStore("$directoryPath/profile_settings.preferences_pb"),
        appSettingsStore = createAppSettingsStore("$directoryPath/app_settings.preferences_pb"),
        databaseManager = RoomDatabaseManager(database, databasePath),
    )
}
