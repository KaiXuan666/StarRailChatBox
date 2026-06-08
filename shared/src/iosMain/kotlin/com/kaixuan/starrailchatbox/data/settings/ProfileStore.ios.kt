@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kaixuan.starrailchatbox.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun createProfileStore(path: String?, context: Any?): ProfileStore {
    val resolvedPath = path ?: run {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        "${requireNotNull(directory?.path)}/profile_settings.preferences_pb"
    }
    return DataStoreProfileStore(
        dataStore = PreferenceDataStoreFactory.createWithPath { resolvedPath.toPath() },
        resolvedPath = resolvedPath
    )
}

private class DataStoreProfileStore(
    private val dataStore: DataStore<Preferences>,
    private val resolvedPath: String
) : ProfileStore {
    private val fileSystem = okio.FileSystem.SYSTEM

    override val profile: Flow<UserProfile?> = dataStore.data.map { preferences ->
        val nickname = preferences[NicknameKey] ?: return@map null
        val customAvatar = preferences[CustomAvatarKey]
        UserProfile(nickname, customAvatar)
    }

    override suspend fun load(): UserProfile? = profile.first()

    override suspend fun save(profile: UserProfile) {
        val finalAvatarUri = if (!profile.customAvatarUri.isNullOrBlank()) {
            val avatarDirStr = "${resolvedPath.substringBeforeLast("/")}/user_avatars"
            val avatarDirPath = avatarDirStr.toPath()
            if (profile.customAvatarUri.contains("user_avatars/")) {
                profile.customAvatarUri
            } else {
                fileSystem.createDirectories(avatarDirPath)
                if (fileSystem.exists(avatarDirPath)) {
                    fileSystem.list(avatarDirPath).forEach {
                        fileSystem.delete(it, mustExist = false)
                    }
                }
                val targetPath = avatarDirPath / "user_avatar_${System.currentTimeMillis()}.png"
                val sourcePath = profile.customAvatarUri.removePrefix("file://").toPath()
                fileSystem.copy(sourcePath, targetPath)
                "file://$targetPath"
            }
        } else {
            val avatarDirStr = "${resolvedPath.substringBeforeLast("/")}/user_avatars"
            val avatarDirPath = avatarDirStr.toPath()
            if (fileSystem.exists(avatarDirPath)) {
                fileSystem.list(avatarDirPath).forEach {
                    fileSystem.delete(it, mustExist = false)
                }
            }
            null
        }

        dataStore.edit { preferences ->
            preferences[NicknameKey] = profile.nickname
            if (finalAvatarUri != null) {
                preferences[CustomAvatarKey] = finalAvatarUri
            } else {
                preferences.remove(CustomAvatarKey)
            }
        }
    }
}

private val NicknameKey = stringPreferencesKey("profile_nickname")
private val CustomAvatarKey = stringPreferencesKey("profile_custom_avatar_uri")
