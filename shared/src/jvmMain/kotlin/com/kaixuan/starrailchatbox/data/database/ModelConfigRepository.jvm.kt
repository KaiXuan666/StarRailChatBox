package com.kaixuan.starrailchatbox.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.character.DefaultCharacterRepository
import com.kaixuan.starrailchatbox.data.character.FileCharacterAvatarStorage
import com.kaixuan.starrailchatbox.data.character.RoomCharacterStorage
import com.kaixuan.starrailchatbox.data.chat.RoomChatSessionRepository
import com.kaixuan.starrailchatbox.data.model.RoomModelConfigRepository
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
            ),
        ),
        chatSessionRepository = RoomChatSessionRepository(database),
    )
}
