package com.kaixuan.starrailchatbox.data.database

import androidx.room.Room
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.character.DefaultCharacterRepository
import com.kaixuan.starrailchatbox.data.character.FileCharacterAvatarStorage
import com.kaixuan.starrailchatbox.data.character.FileCharacterVoiceSampleStorage
import com.kaixuan.starrailchatbox.data.character.RoomCharacterStorage
import com.kaixuan.starrailchatbox.data.chat.RoomChatSessionRepository
import com.kaixuan.starrailchatbox.data.model.RoomModelConfigRepository
import com.kaixuan.starrailchatbox.data.settings.createAppSettingsStore
import com.kaixuan.starrailchatbox.data.settings.createProfileStore
import java.io.File

fun createPersistentRepositories(
    databasePath: String? = null,
): PersistentRepositories {
    val databaseFile = databasePath?.let(::File) ?: File(
        System.getProperty("user.home"),
        ".starrailchatbox/starrail_chat_box.db",
    )
    databaseFile.parentFile?.mkdirs()
    val database = Room.databaseBuilder<StarRailDatabase>(
        name = databaseFile.absolutePath,
        factory = StarRailDatabaseConstructor::initialize,
    )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()
    return PersistentRepositories(
        modelConfigRepository = RoomModelConfigRepository(
            dao = database.modelConfigDao(),
            cipher = createApiKeyCipher(
                databaseFile.resolveSibling("api_key.key.preferences_pb").absolutePath,
            ),
        ),
        characterRepository = DefaultCharacterRepository(
            RoomCharacterStorage(
                dao = database.agentRoleDao(),
                avatarStorage = FileCharacterAvatarStorage(
                    databaseFile.resolveSibling("character_avatars"),
                ),
                voiceSampleStorage = FileCharacterVoiceSampleStorage(
                    databaseFile.resolveSibling("character_voice_samples"),
                ),
            ),
        ),
        chatSessionRepository = RoomChatSessionRepository(database),
        profileStore = createProfileStore(
            path = databaseFile.resolveSibling("profile_settings.preferences_pb").absolutePath,
        ),
        appSettingsStore = createAppSettingsStore(
            path = databaseFile.resolveSibling("app_settings.preferences_pb").absolutePath,
        ),
        databaseManager = RoomDatabaseManager(database, databaseFile.absolutePath),
    )
}
