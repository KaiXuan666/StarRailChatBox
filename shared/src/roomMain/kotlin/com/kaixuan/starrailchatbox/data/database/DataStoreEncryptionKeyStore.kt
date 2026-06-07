package com.kaixuan.starrailchatbox.data.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kaixuan.starrailchatbox.data.settings.ApiKeyCipher
import com.kaixuan.starrailchatbox.data.settings.EncryptionKeyStore
import com.kaixuan.starrailchatbox.data.settings.decodeBase64
import com.kaixuan.starrailchatbox.data.settings.encodeBase64
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

internal fun createApiKeyCipher(keyStorePath: String): ApiKeyCipher {
    val dataStore = PreferenceDataStoreFactory.createWithPath {
        keyStorePath.toPath()
    }
    return ApiKeyCipher(DataStoreEncryptionKeyStore(dataStore))
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

private val EncryptionKey = stringPreferencesKey("encryption_key_v1")
