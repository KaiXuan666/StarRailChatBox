package com.kaixuan.starrailchatbox.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.RoomModelConfigRepository

fun createModelConfigRepository(
    context: Context,
    databaseName: String = "starrail_chat_box.db",
): ModelConfigRepository {
    val database = Room.databaseBuilder<StarRailDatabase>(
        context = context.applicationContext,
        name = databaseName,
    )
        .setDriver(BundledSQLiteDriver())
        .build()
    val keyStorePath = context.filesDir.resolve("api_key.key.preferences_pb").absolutePath
    return RoomModelConfigRepository(
        dao = database.modelConfigDao(),
        cipher = createApiKeyCipher(keyStorePath),
    )
}
