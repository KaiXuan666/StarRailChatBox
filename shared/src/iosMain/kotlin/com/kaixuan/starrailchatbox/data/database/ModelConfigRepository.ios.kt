@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kaixuan.starrailchatbox.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.RoomModelConfigRepository
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun createModelConfigRepository(): ModelConfigRepository {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val directoryPath = requireNotNull(directory?.path)
    val database = Room.databaseBuilder<StarRailDatabase>(
        name = "$directoryPath/starrail_chat_box.db",
        factory = StarRailDatabaseConstructor::initialize,
    )
        .setDriver(BundledSQLiteDriver())
        .build()
    return RoomModelConfigRepository(
        dao = database.modelConfigDao(),
        cipher = createApiKeyCipher("$directoryPath/api_key.key.preferences_pb"),
    )
}
