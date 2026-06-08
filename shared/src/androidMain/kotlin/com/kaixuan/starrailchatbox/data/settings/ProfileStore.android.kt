package com.kaixuan.starrailchatbox.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

actual fun createProfileStore(path: String?, context: Any?): ProfileStore {
    if (path == null) return InMemoryProfileStore()
    return DataStoreProfileStore(
        dataStore = PreferenceDataStoreFactory.createWithPath { path.toPath() },
        resolvedPath = path,
        context = context as? android.content.Context
    )
}

private class DataStoreProfileStore(
    private val dataStore: DataStore<Preferences>,
    private val resolvedPath: String,
    private val context: android.content.Context?
) : ProfileStore {
    override suspend fun load(): UserProfile? {
        val preferences = dataStore.data.first()
        val nickname = preferences[NicknameKey] ?: return null
        val customAvatar = preferences[CustomAvatarKey]
        return UserProfile(nickname, customAvatar)
    }

    override suspend fun save(profile: UserProfile) {
        val finalAvatarUri = if (!profile.customAvatarUri.isNullOrBlank()) {
            val preferencesFile = java.io.File(resolvedPath)
            val avatarDir = java.io.File(preferencesFile.parentFile, "user_avatars")
            if (profile.customAvatarUri.contains("user_avatars/")) {
                profile.customAvatarUri
            } else {
                avatarDir.listFiles()?.forEach { it.delete() }
                avatarDir.mkdirs()
                val targetFile = java.io.File(avatarDir, "user_avatar_${System.currentTimeMillis()}.png")
                val sourceUri = profile.customAvatarUri
                if (sourceUri.startsWith("content://")) {
                    val resolver = requireNotNull(context) { "Android context is required to copy content URI." }.contentResolver
                    resolver.openInputStream(android.net.Uri.parse(sourceUri)).use { input ->
                        requireNotNull(input) { "Unable to open source: $sourceUri" }
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } else {
                    val sourcePath = sourceUri.removePrefix("file://")
                    java.io.File(sourcePath).copyTo(targetFile, overwrite = true)
                }
                "file://${targetFile.absolutePath}"
            }
        } else {
            val preferencesFile = java.io.File(resolvedPath)
            val avatarDir = java.io.File(preferencesFile.parentFile, "user_avatars")
            avatarDir.listFiles()?.forEach { it.delete() }
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
