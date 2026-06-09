package com.kaixuan.starrailchatbox.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
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
}

private val DarkThemeKey = booleanPreferencesKey("dark_theme_override")
