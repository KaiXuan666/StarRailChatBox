package com.kaixuan.starrailchatbox.data.character

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kaixuan.starrailchatbox.data.database.StarRailDatabase
import com.kaixuan.starrailchatbox.data.database.StarRailDatabaseConstructor
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomCharacterStorageTest {
    @Test
    fun importsDefaultsIntoAgentRoleWithoutOverwritingExistingRows() = runTest {
        val databasePath = Files.createTempFile("starrail-agent-role", ".db")
        val avatarDirectory = Files.createTempDirectory("starrail-agent-avatars")
        val voiceSampleDirectory = Files.createTempDirectory("starrail-agent-voice-samples")
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        val storage = RoomCharacterStorage(
            dao = database.agentRoleDao(),
            avatarStorage = FileCharacterAvatarStorage(avatarDirectory.toFile()),
            voiceSampleStorage = FileCharacterVoiceSampleStorage(voiceSampleDirectory.toFile()),
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
            assertTrue(stored.avatarUri.startsWith(avatarDirectory.toString()))
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
            avatarDirectory.toFile().deleteRecursively()
            voiceSampleDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun saveCharacterNewAssignsNextMaxSortOrderAndExistingRetainsSortOrder() = runTest {
        val databasePath = Files.createTempFile("starrail-agent-role-save", ".db")
        val avatarDirectory = Files.createTempDirectory("starrail-agent-avatars-save")
        val voiceSampleDirectory = Files.createTempDirectory("starrail-agent-voice-samples-save")
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        val storage = RoomCharacterStorage(
            dao = database.agentRoleDao(),
            avatarStorage = FileCharacterAvatarStorage(avatarDirectory.toFile()),
            voiceSampleStorage = FileCharacterVoiceSampleStorage(voiceSampleDirectory.toFile()),
            currentTimeMillis = { 1_000L },
        )

        try {
            val initial = characterFiles("builtin:流萤", "流萤", "first")
            val second = characterFiles("builtin:三月七", "三月七", "second")
            storage.initializeDefaults(listOf(initial, second))

            assertEquals(0, database.agentRoleDao().findById("builtin:流萤")?.sortOrder)
            assertEquals(1, database.agentRoleDao().findById("builtin:三月七")?.sortOrder)

            val newChar = characterFiles("custom:流萤2号", "流萤2号", "custom prompt")
            storage.saveCharacter(
                CharacterFiles(
                    id = newChar.id,
                    name = newChar.name,
                    prompt = newChar.prompt,
                    openingMessage = newChar.openingMessage,
                    avatarUri = "",
                ),
                CharacterAvatarSource(createAvatarSource(byteArrayOf(4, 5, 6))),
            )

            val storedNew = requireNotNull(database.agentRoleDao().findById("custom:流萤2号"))
            assertEquals(2, storedNew.sortOrder)
            assertContentEquals(byteArrayOf(4, 5, 6), Files.readAllBytes(java.nio.file.Path.of(storedNew.avatarUri)))

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
            avatarDirectory.toFile().deleteRecursively()
            voiceSampleDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun replacingAvatarOverwritesOwnedAvatarAndDeleteCharacterRemovesAvatarFile() = runTest {
        val databasePath = Files.createTempFile("starrail-agent-role-delete", ".db")
        val avatarDirectory = Files.createTempDirectory("starrail-agent-avatars-delete")
        val voiceSampleDirectory = Files.createTempDirectory("starrail-agent-voice-samples-delete")
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        val storage = RoomCharacterStorage(
            dao = database.agentRoleDao(),
            avatarStorage = FileCharacterAvatarStorage(avatarDirectory.toFile()),
            voiceSampleStorage = FileCharacterVoiceSampleStorage(voiceSampleDirectory.toFile()),
            currentTimeMillis = { 1_000L },
        )

        try {
            val initial = characterFiles("builtin:流萤", "流萤", "first")
            storage.initializeDefaults(listOf(initial))
            val oldAvatarUri = requireNotNull(database.agentRoleDao().findById(initial.id)).avatarUri

            val saved = storage.saveCharacter(
                CharacterFiles(
                    id = initial.id,
                    name = initial.name,
                    prompt = initial.prompt,
                    openingMessage = initial.openingMessage,
                    avatarUri = oldAvatarUri,
                ),
                CharacterAvatarSource(createAvatarSource(byteArrayOf(9, 8, 7))),
            )

            assertEquals(oldAvatarUri, saved.avatarUri)
            assertContentEquals(byteArrayOf(9, 8, 7), Files.readAllBytes(java.nio.file.Path.of(saved.avatarUri)))

            storage.deleteCharacter(initial.id, deletedAt = 2_000L)

            assertEquals(null, database.agentRoleDao().findById(initial.id))
            assertEquals(false, Files.exists(java.nio.file.Path.of(saved.avatarUri)))
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
            avatarDirectory.toFile().deleteRecursively()
            voiceSampleDirectory.toFile().deleteRecursively()
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
