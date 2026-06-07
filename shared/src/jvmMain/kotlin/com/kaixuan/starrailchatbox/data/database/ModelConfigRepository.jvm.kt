package com.kaixuan.starrailchatbox.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.RoomModelConfigRepository
import java.io.File

fun createModelConfigRepository(
    databasePath: String? = null,
): ModelConfigRepository {
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
    return RoomModelConfigRepository(
        dao = database.modelConfigDao(),
        cipher = createApiKeyCipher(
            databaseFile.resolveSibling("api_key.key.preferences_pb").absolutePath,
        ),
    )
}
