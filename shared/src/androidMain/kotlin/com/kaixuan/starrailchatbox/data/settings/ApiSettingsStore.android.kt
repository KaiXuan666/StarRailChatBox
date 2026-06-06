package com.kaixuan.starrailchatbox.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

actual fun createApiSettingsStore(path: String?): ApiSettingsStore {
    if (path == null) return InMemoryApiSettingsStore()
    val keyDataStore = PreferenceDataStoreFactory.createWithPath {
        "$path.encryption_key".toPath()
    }
    return DataStoreApiSettingsStore(
        dataStore = PreferenceDataStoreFactory.createWithPath { path.toPath() },
        cipher = ApiKeyCipher(DataStoreEncryptionKeyStore(keyDataStore)),
    )
}

private class DataStoreApiSettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: ApiKeyCipher,
) : ApiSettingsStore {
    override suspend fun load(): StoredApiSettings? {
        val preferences = dataStore.data.first()
        val host = preferences[ApiHostKey] ?: return null
        val encryptedApiKey = preferences[EncryptedApiKeyKey]
        return StoredApiSettings(
            apiHost = host,
            apiKey = encryptedApiKey?.let { cipher.decrypt(it) }.orEmpty(),
            selectedModel = preferences[SelectedModelKey].orEmpty(),
        )
    }

    override suspend fun save(settings: StoredApiSettings) {
        val encryptedApiKey = cipher.encrypt(settings.apiKey)
        dataStore.edit { preferences ->
            preferences[ApiHostKey] = settings.apiHost
            preferences[EncryptedApiKeyKey] = encryptedApiKey
            preferences[SelectedModelKey] = settings.selectedModel
        }
    }
}

private class DataStoreEncryptionKeyStore(
    private val dataStore: DataStore<Preferences>,
) : EncryptionKeyStore {
    override suspend fun load(): ByteArray? {
        return dataStore.data.first()[EncryptionKey]?.decodeBase64()
    }

    override suspend fun save(key: ByteArray) {
        dataStore.edit { preferences ->
            preferences[EncryptionKey] = key.encodeBase64()
        }
    }
}

private val ApiHostKey = stringPreferencesKey("api_host")
private val EncryptedApiKeyKey = stringPreferencesKey("encrypted_api_key_v1")
private val SelectedModelKey = stringPreferencesKey("selected_model")
private val EncryptionKey = stringPreferencesKey("encryption_key_v1")
