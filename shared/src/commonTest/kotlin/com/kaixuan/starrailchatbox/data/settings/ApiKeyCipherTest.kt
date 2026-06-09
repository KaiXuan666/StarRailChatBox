package com.kaixuan.starrailchatbox.data.settings

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiKeyCipherTest {

    @Test
    fun encryptsAndDecryptsApiKey() = runTest {
        val cipher = ApiKeyCipher(InMemoryEncryptionKeyStore())

        val encrypted = cipher.encrypt("sk-secret-value")

        assertTrue(encrypted.startsWith("v1:"))
        assertFalse(encrypted.contains("sk-secret-value"))
        assertEquals("sk-secret-value", cipher.decrypt(encrypted))
    }

    @Test
    fun persistedKeyDecryptsAcrossCipherInstances() = runTest {
        val keyStore = InMemoryEncryptionKeyStore()
        val encrypted = ApiKeyCipher(keyStore).encrypt("sk-persisted")

        val decrypted = ApiKeyCipher(keyStore).decrypt(encrypted)

        assertEquals("sk-persisted", decrypted)
    }

    @Test
    fun differentKeyCannotDecryptCiphertext() = runTest {
        val encrypted = ApiKeyCipher(InMemoryEncryptionKeyStore()).encrypt("sk-private")
        val otherCipher = ApiKeyCipher(InMemoryEncryptionKeyStore())
        otherCipher.encrypt("initialize-different-key")

        assertEquals("", otherCipher.decrypt(encrypted))
    }

    @Test
    fun returnsEmptyWhenDecryptionFails() = runTest {
        val cipher = ApiKeyCipher(InMemoryEncryptionKeyStore())
        // Invalid format
        assertEquals("", cipher.decrypt("v1:"))
        // Invalid version
        assertEquals("", cipher.decrypt("v2:some-data"))
        // Invalid base64
        assertEquals("", cipher.decrypt("v1:???"))
    }

    @Test
    fun emptyApiKeyRoundTripsWithoutCreatingKey() = runTest {
        val keyStore = InMemoryEncryptionKeyStore()
        val cipher = ApiKeyCipher(keyStore)

        assertEquals("", cipher.encrypt(""))
        assertEquals("", cipher.decrypt(""))
        assertEquals(null, keyStore.load())
    }
}

private class InMemoryEncryptionKeyStore : EncryptionKeyStore {
    private var key: ByteArray? = null

    override suspend fun load(): ByteArray? = key?.copyOf()

    override suspend fun save(key: ByteArray) {
        this.key = key.copyOf()
    }
}
