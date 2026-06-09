package com.kaixuan.starrailchatbox.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
