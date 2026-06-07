package com.kaixuan.starrailchatbox.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

actual fun createProfileStore(path: String?): ProfileStore {
    if (path == null) return InMemoryProfileStore()
    return DataStoreProfileStore(
        dataStore = PreferenceDataStoreFactory.createWithPath { path.toPath() }
    )
}

private class DataStoreProfileStore(
    private val dataStore: DataStore<Preferences>
) : ProfileStore {
    override suspend fun load(): UserProfile? {
        val preferences = dataStore.data.first()
        val nickname = preferences[NicknameKey] ?: return null
        val customAvatar = preferences[CustomAvatarKey]
        return UserProfile(nickname, customAvatar)
    }

    override suspend fun save(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[NicknameKey] = profile.nickname
            if (profile.customAvatarBase64 != null) {
                preferences[CustomAvatarKey] = profile.customAvatarBase64
            } else {
                preferences.remove(CustomAvatarKey)
            }
        }
    }
}

private val NicknameKey = stringPreferencesKey("profile_nickname")
private val CustomAvatarKey = stringPreferencesKey("profile_custom_avatar_base64")
