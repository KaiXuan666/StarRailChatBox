@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.kaixuan.starrailchatbox.data.settings

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import io.github.aakira.napier.Napier
import kotlin.io.encoding.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface EncryptionKeyStore {
    suspend fun load(): ByteArray?

    suspend fun save(key: ByteArray)
}

class ApiKeyCipher(
    private val keyStore: EncryptionKeyStore,
) {
    private val keyMutex = Mutex()

    suspend fun encrypt(apiKey: String): String {
        if (apiKey.isEmpty()) return ""
        val key = loadOrCreateKey()
        val ciphertext = key.cipher().encrypt(
            plaintext = apiKey.encodeToByteArray(),
            associatedData = AssociatedData,
        )
        return "$CiphertextVersion:${Base64.encode(ciphertext)}"
    }

    suspend fun decrypt(value: String): String {
        if (value.isEmpty()) return ""
        try {
            val encodedCiphertext = value.substringAfter(
                delimiter = "$CiphertextVersion:",
                missingDelimiterValue = "",
            )
            require(encodedCiphertext.isNotEmpty()) {
                "Unsupported encrypted API key format"
            }

            val rawKey = requireNotNull(keyStore.load()) {
                "API key encryption key is unavailable"
            }
            val key = CryptographyProvider.Default
                .get(AES.GCM)
                .keyDecoder()
                .decodeFromByteArray(AES.Key.Format.RAW, rawKey)
            val plaintext = key.cipher().decrypt(
                ciphertext = Base64.decode(encodedCiphertext),
                associatedData = AssociatedData,
            )
            Napier.e { "ApiKeyCipher plaintext=${plaintext} decodeToString=${plaintext.decodeToString()}" }
            return plaintext.decodeToString()
        } catch (e: Exception) {
            Napier.e { "ApiKeyCipher e=${e.message}" }
            e.printStackTrace()
        }
        return ""
    }

    private suspend fun loadOrCreateKey(): AES.GCM.Key = keyMutex.withLock {
        val aes = CryptographyProvider.Default.get(AES.GCM)
        keyStore.load()?.let { storedKey ->
            return@withLock aes.keyDecoder()
                .decodeFromByteArray(AES.Key.Format.RAW, storedKey)
        }

        val key = aes.keyGenerator().generateKey()
        keyStore.save(key.encodeToByteArray(AES.Key.Format.RAW))
        key
    }

    private companion object {
        const val CiphertextVersion = "v1"
        val AssociatedData = "StarRailChatBox.ApiKey.v1".encodeToByteArray()
    }
}

internal fun ByteArray.encodeBase64(): String = Base64.encode(this)

internal fun String.decodeBase64(): ByteArray = Base64.decode(this)
