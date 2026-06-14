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
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64

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
        val encrypted = dataStore.data.first()[characterUpdateTokenKey(characterKey)] ?: return null
        return runCatching { decryptUpdateToken(encrypted) }.getOrNull()
    }

    override suspend fun setCharacterUpdateToken(characterKey: String, token: String) {
        dataStore.edit { preferences ->
            preferences[characterUpdateTokenKey(characterKey)] = encryptUpdateToken(token)
        }
    }

    override suspend fun getCatalogAdminKey(): String? {
        val encrypted = dataStore.data.first()[CatalogAdminKey] ?: return null
        return runCatching { decryptUpdateToken(encrypted) }.getOrNull()
    }

    override suspend fun setCatalogAdminKey(key: String?) {
        dataStore.edit { preferences ->
            if (key.isNullOrBlank()) {
                preferences.remove(CatalogAdminKey)
            } else {
                preferences[CatalogAdminKey] = encryptUpdateToken(key)
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

private const val UpdateTokenKeyAlias = "starrail_character_update_tokens"

private fun getOrCreateUpdateTokenKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (keyStore.getKey(UpdateTokenKeyAlias, null) as? SecretKey)?.let { return it }
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
        init(
            KeyGenParameterSpec.Builder(
                UpdateTokenKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        generateKey()
    }
}

private fun encryptUpdateToken(token: String): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateUpdateTokenKey())
    val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    val encrypted = Base64.encodeToString(
        cipher.doFinal(token.encodeToByteArray()),
        Base64.NO_WRAP,
    )
    return "$iv:$encrypted"
}

private fun decryptUpdateToken(value: String): String {
    val parts = value.split(':', limit = 2)
    require(parts.size == 2)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(
        Cipher.DECRYPT_MODE,
        getOrCreateUpdateTokenKey(),
        GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)),
    )
    return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).decodeToString()
}
