package com.kaixuan.starrailchatbox.data.model

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.database.StarRailDatabase
import com.kaixuan.starrailchatbox.data.database.StarRailDatabaseConstructor
import com.kaixuan.starrailchatbox.data.settings.ApiKeyCipher
import com.kaixuan.starrailchatbox.data.settings.EncryptionKeyStore
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RoomModelConfigRepositoryTest {
    @Test
    fun savesEncryptedApiKeyAndRestoresModelConfig() = runTest {
        val databasePath = Files.createTempFile("starrail-model-config", ".db")
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        val repository = RoomModelConfigRepository(
            dao = database.modelConfigDao(),
            cipher = ApiKeyCipher(TestEncryptionKeyStore()),
            currentTimeMillis = { 1_000L },
        )
        val config = ModelConfig(
            id = DefaultModelConfig.Id,
            provider = "openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            apiKey = "sk-secret",
            modelName = "gpt-test",
            contextWindow = 128_000,
            maxOutputTokens = 4_096,
            supportVision = true,
            supportToolCall = true,
            supportReasoning = false,
            temperature = 0.7,
            topP = 1.0,
            enabled = true,
        )

        try {
            repository.saveDefault(config)

            val stored = requireNotNull(database.modelConfigDao().findById(DefaultModelConfig.Id))
            assertNotEquals(config.apiKey, stored.apiKeyEncrypted)
            assertTrue(stored.apiKeyEncrypted.orEmpty().startsWith("v1:"))
            assertEquals(config, repository.getDefault())
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
        }
    }
}

private class TestEncryptionKeyStore : EncryptionKeyStore {
    private var key: ByteArray? = null

    override suspend fun load(): ByteArray? = key?.copyOf()

    override suspend fun save(key: ByteArray) {
        this.key = key.copyOf()
    }
}
