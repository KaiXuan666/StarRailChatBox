package com.kaixuan.starrailchatbox.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

actual fun createAppSettingsStore(path: String?, context: Any?): AppSettingsStore {
    if (path == null) return InMemoryAppSettingsStore()
    return DataStoreAppSettingsStore(
        dataStore = PreferenceDataStoreFactory.createWithPath { path.toPath() }
    )
}

private class DataStoreAppSettingsStore(
    private val dataStore: DataStore<Preferences>
) : AppSettingsStore {
    override val darkThemeOverride: Flow<Boolean?> = dataStore.data.map { preferences ->
        if (preferences.contains(DarkThemeKey)) {
            preferences[DarkThemeKey]
        } else {
            null
        }
    }

    override suspend fun setDarkThemeOverride(darkThemeOverride: Boolean?) {
        dataStore.edit { preferences ->
            if (darkThemeOverride != null) {
                preferences[DarkThemeKey] = darkThemeOverride
            } else {
                preferences.remove(DarkThemeKey)
            }
        }
    }

    override suspend fun getCharacterUpdateToken(characterKey: String): String? {
        return dataStore.data.first()[characterUpdateTokenKey(characterKey)]
    }

    override suspend fun setCharacterUpdateToken(characterKey: String, token: String) {
        dataStore.edit { preferences ->
            preferences[characterUpdateTokenKey(characterKey)] = token
        }
    }

    override suspend fun getCatalogAdminKey(): String? = null

    override suspend fun setCatalogAdminKey(key: String?) = Unit

    override val userNickname: Flow<String> = dataStore.data.map { preferences ->
        preferences[UserNicknameKey] ?: ""
    }

    override suspend fun setUserNickname(nickname: String) {
        dataStore.edit { preferences ->
            preferences[UserNicknameKey] = nickname
        }
    }
}

private val DarkThemeKey = booleanPreferencesKey("dark_theme_override")
private val UserNicknameKey = stringPreferencesKey("user_nickname")
private fun characterUpdateTokenKey(characterKey: String) =
    stringPreferencesKey("character_update_token_$characterKey")
