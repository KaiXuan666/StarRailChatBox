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
        UserProfile(
            customAvatarUri = preferences[CustomAvatarKey],
            summaryThreshold = preferences[SummaryThresholdKey] ?: 20,
            saveMultimodalToken = preferences[SaveMultimodalTokenKey] ?: false,
            enableWebSearch = preferences[EnableWebSearchKey] ?: false
        )
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
                val targetPath = avatarDirPath / "user_avatar_${platform.Foundation.NSUUID().UUIDString}.png"
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
            if (finalAvatarUri != null) {
                preferences[CustomAvatarKey] = finalAvatarUri
            } else {
                preferences.remove(CustomAvatarKey)
            }
            preferences[SummaryThresholdKey] = profile.summaryThreshold
            preferences[SaveMultimodalTokenKey] = profile.saveMultimodalToken
            preferences[EnableWebSearchKey] = profile.enableWebSearch
        }
    }
}

private val CustomAvatarKey = stringPreferencesKey("profile_custom_avatar_uri")
private val SummaryThresholdKey = androidx.datastore.preferences.core.intPreferencesKey("summary_threshold")
private val SaveMultimodalTokenKey = androidx.datastore.preferences.core.booleanPreferencesKey("save_multimodal_token")
private val EnableWebSearchKey = androidx.datastore.preferences.core.booleanPreferencesKey("enable_web_search")
