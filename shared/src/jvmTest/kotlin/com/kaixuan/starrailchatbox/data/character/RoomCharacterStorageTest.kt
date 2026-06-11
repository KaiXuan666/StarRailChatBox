package com.kaixuan.starrailchatbox.data.character

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.database.StarRailDatabase
import com.kaixuan.starrailchatbox.data.database.StarRailDatabaseConstructor
import com.kaixuan.starrailchatbox.data.database.entity.ChatSessionEntity
import com.kaixuan.starrailchatbox.platform.JvmFileManager
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import java.nio.file.Files
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class RoomCharacterStorageTest {
    private fun createMockFileManager(appDir: java.nio.file.Path, cacheDir: java.nio.file.Path) = object : KmpFileManager {
        override val appDataDir: Path = appDir.toString().toPath()
        override val cacheDir: Path = cacheDir.toString().toPath()
        override val fileSystem: FileSystem = FileSystem.SYSTEM
        override suspend fun saveImageToGallery(bytes: ByteArray, name: String) {}
    }

    @Test
    fun characterSummaryFlowUpdatesWhenSessionLastMessageChanges() = runTest {
        val databasePath = Files.createTempFile("starrail-agent-role-flow", ".db")
        val appDataDirectory = Files.createTempDirectory("starrail-agent-appdata-flow")
        val cacheDirectory = Files.createTempDirectory("starrail-agent-cache-flow")
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        val storage = RoomCharacterStorage(
            dao = database.agentRoleDao(),
            fileManager = createMockFileManager(appDataDirectory, cacheDirectory),
            currentTimeMillis = { 1_000L },
        )

        try {
            storage.initializeDefaults(listOf(characterFiles("builtin:流萤", "流萤", "first")))
            database.chatSessionDao().upsert(
                ChatSessionEntity(
                    id = "session-1",
                    title = "测试会话",
                    agentId = "builtin:流萤",
                    systemPromptSnapshot = "prompt",
                    maxContextMessageCount = 30,
                    enableSummary = true,
                    summaryThresholdTokens = 4_000,
                    summaryThresholdMessageCount = 30,
                    summaryRetainedMessageCount = 10,
                    compactionSeq = 0L,
                    lastMessageAt = 1_000L,
                    pinned = false,
                    archived = false,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                ),
            )
            val updatedSummary = async {
                storage.observeCharacterSummaries().first { summaries ->
                    summaries.single().lastMessageAt == 2_000L
                }.single()
            }

            database.chatSessionDao().updateLastMessage(
                sessionId = "session-1",
                messageId = "message-1",
                messageAt = 2_000L,
            )

            assertEquals(2_000L, updatedSummary.await().lastMessageAt)
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
            appDataDirectory.toFile().deleteRecursively()
            cacheDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun importsDefaultsIntoAgentRoleWithoutOverwritingExistingRows() = runTest {
        val databasePath = Files.createTempFile("starrail-agent-role", ".db")
        val appDataDirectory = Files.createTempDirectory("starrail-agent-appdata")
        val cacheDirectory = Files.createTempDirectory("starrail-agent-cache")
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        val storage = RoomCharacterStorage(
            dao = database.agentRoleDao(),
            fileManager = createMockFileManager(appDataDirectory, cacheDirectory),
            currentTimeMillis = { 1_000L },
        )
        val initial = characterFiles("builtin:流萤", "流萤", "first")
        val second = characterFiles("builtin:三月七", "三月七", "second")

        try {
            storage.initializeDefaults(listOf(initial, second))

            val stored = requireNotNull(database.agentRoleDao().findById(initial.id))
            assertTrue(stored.isBuiltin)
            assertEquals("first", stored.systemPrompt)
            assertEquals("今天要聊点什么呢？", stored.openingMessage)
            assertTrue(stored.avatarUri.contains(appDataDirectory.toString()))
            assertContentEquals(initial.avatarContent, Files.readAllBytes(java.nio.file.Path.of(stored.avatarUri)))
            assertEquals(
                listOf("流萤", "三月七"),
                database.agentRoleDao().findAll().map { role -> role.name },
            )
            assertEquals(
                1_000L,
                storage.loadCharacters().first { it.id == initial.id }.createdAt,
            )

            val retrieved = storage.getCharacter(initial.id)
            kotlin.test.assertNotNull(retrieved)
            assertEquals("first", retrieved.prompt)

            storage.initializeDefaults(listOf(characterFiles("builtin:流萤", "流萤", "changed")))

            assertEquals("first", database.agentRoleDao().findById(initial.id)?.systemPrompt)
            assertContentEquals(initial.avatarContent, Files.readAllBytes(java.nio.file.Path.of(stored.avatarUri)))
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
            appDataDirectory.toFile().deleteRecursively()
            cacheDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun saveCharacterNewAssignsNextMaxSortOrderAndExistingRetainsSortOrder() = runTest {
        val databasePath = Files.createTempFile("starrail-agent-role-save", ".db")
        val appDataDirectory = Files.createTempDirectory("starrail-agent-appdata-save")
        val cacheDirectory = Files.createTempDirectory("starrail-agent-cache-save")
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        val storage = RoomCharacterStorage(
            dao = database.agentRoleDao(),
            fileManager = createMockFileManager(appDataDirectory, cacheDirectory),
            currentTimeMillis = { 1_000L },
        )

        try {
            val initial = characterFiles("builtin:流萤", "流萤", "first")
            val second = characterFiles("builtin:三月七", "三月七", "second")
            storage.initializeDefaults(listOf(initial, second))

            assertEquals(0, database.agentRoleDao().findById("builtin:流萤")?.sortOrder)
            assertEquals(1, database.agentRoleDao().findById("builtin:三月七")?.sortOrder)

            val newChar = characterFiles("custom:流萤2号", "流萤2号", "custom prompt")
            val avatarBytes = byteArrayOf(4, 5, 6)
            val tempAvatar = Files.createTempFile(cacheDirectory, "temp_avatar", ".webp")
            Files.write(tempAvatar, avatarBytes)

            storage.saveCharacter(
                CharacterFiles(
                    id = newChar.id,
                    name = newChar.name,
                    prompt = newChar.prompt,
                    openingMessage = newChar.openingMessage,
                    avatarUri = "",
                ),
                CharacterAvatarSource(tempAvatar.toString()),
            )

            val storedNew = requireNotNull(database.agentRoleDao().findById("custom:流萤2号"))
            assertEquals(2, storedNew.sortOrder)
            assertContentEquals(avatarBytes, Files.readAllBytes(java.nio.file.Path.of(storedNew.avatarUri)))
            assertFalse(Files.exists(tempAvatar), "Cache file should be moved")

            val updatedInitial = characterFiles("builtin:流萤", "流萤新版", "new prompt")
            storage.saveCharacter(
                CharacterFiles(
                    id = updatedInitial.id,
                    name = updatedInitial.name,
                    prompt = updatedInitial.prompt,
                    openingMessage = updatedInitial.openingMessage,
                    avatarUri = database.agentRoleDao().findById(updatedInitial.id)?.avatarUri.orEmpty(),
                ),
                null,
            )

            val storedUpdated = requireNotNull(database.agentRoleDao().findById("builtin:流萤"))
            assertEquals(0, storedUpdated.sortOrder)
            assertEquals("流萤新版", storedUpdated.name)
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
            appDataDirectory.toFile().deleteRecursively()
            cacheDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun replacingAvatarOverwritesOwnedAvatarAndDeleteCharacterRemovesAvatarFile() = runTest {
        val databasePath = Files.createTempFile("starrail-agent-role-delete", ".db")
        val appDataDirectory = Files.createTempDirectory("starrail-agent-appdata-delete")
        val cacheDirectory = Files.createTempDirectory("starrail-agent-cache-delete")
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        val storage = RoomCharacterStorage(
            dao = database.agentRoleDao(),
            fileManager = createMockFileManager(appDataDirectory, cacheDirectory),
            currentTimeMillis = { 1_000L },
        )

        try {
            val initial = characterFiles("builtin:流萤", "流萤", "first")
            storage.initializeDefaults(listOf(initial))
            val oldAvatarUri = requireNotNull(database.agentRoleDao().findById(initial.id)).avatarUri

            val avatarBytes = byteArrayOf(9, 8, 7)
            val tempAvatar = Files.createTempFile(cacheDirectory, "temp_avatar", ".webp")
            Files.write(tempAvatar, avatarBytes)

            val saved = storage.saveCharacter(
                CharacterFiles(
                    id = initial.id,
                    name = initial.name,
                    prompt = initial.prompt,
                    openingMessage = initial.openingMessage,
                    avatarUri = oldAvatarUri,
                ),
                CharacterAvatarSource(tempAvatar.toString()),
            )

            assertTrue(saved.avatarUri.contains(appDataDirectory.toString()))
            assertContentEquals(avatarBytes, Files.readAllBytes(java.nio.file.Path.of(saved.avatarUri)))
            assertFalse(Files.exists(java.nio.file.Path.of(oldAvatarUri)), "Old avatar should be deleted")

            storage.deleteCharacter(initial.id, deletedAt = 2_000L)

            assertEquals(null, database.agentRoleDao().findById(initial.id))
            assertEquals(false, Files.exists(java.nio.file.Path.of(saved.avatarUri)))
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
            appDataDirectory.toFile().deleteRecursively()
            cacheDirectory.toFile().deleteRecursively()
        }
    }
}

private fun characterFiles(
    id: String,
    name: String,
    prompt: String,
) = DefaultCharacterAsset(
    id = id,
    name = name,
    prompt = prompt,
    openingMessage = "今天要聊点什么呢？",
    avatarContent = byteArrayOf(1, 2, 3),
)

private fun createAvatarSource(bytes: ByteArray): String {
    val source = Files.createTempFile("starrail-avatar-source", ".webp")
    Files.write(source, bytes)
    return source.toString()
}
