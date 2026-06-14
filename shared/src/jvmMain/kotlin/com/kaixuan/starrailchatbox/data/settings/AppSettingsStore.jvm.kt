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
import com.kaixuan.starrailchatbox.data.database.createApiKeyCipher

actual fun createAppSettingsStore(path: String?, context: Any?): AppSettingsStore {
    if (path == null) return InMemoryAppSettingsStore()
    return DataStoreAppSettingsStore(
        dataStore = PreferenceDataStoreFactory.createWithPath { path.toPath() },
        adminKeyCipher = createApiKeyCipher("$path.catalog_admin_key.preferences_pb"),
    )
}

private class DataStoreAppSettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val adminKeyCipher: ApiKeyCipher,
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

    override suspend fun getCatalogAdminKey(): String? {
        val encrypted = dataStore.data.first()[CatalogAdminKey] ?: return null
        return adminKeyCipher.decrypt(encrypted).takeIf(String::isNotBlank)
    }

    override suspend fun setCatalogAdminKey(key: String?) {
        dataStore.edit { preferences ->
            if (key.isNullOrBlank()) {
                preferences.remove(CatalogAdminKey)
            } else {
                preferences[CatalogAdminKey] = adminKeyCipher.encrypt(key)
            }
        }
    }

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
private val CatalogAdminKey = stringPreferencesKey("catalog_admin_key")
private fun characterUpdateTokenKey(characterKey: String) =
    stringPreferencesKey("character_update_token_$characterKey")
