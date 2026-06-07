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
        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        val storage = RoomCharacterStorage(
            dao = database.agentRoleDao(),
            avatarStorage = FileCharacterAvatarStorage(avatarDirectory.toFile()),
            currentTimeMillis = { 1_000L },
        )
        val initial = characterFiles("builtin:流萤", "流萤", "first")
        val second = characterFiles("builtin:三月七", "三月七", "second")

        try {
            storage.initializeDefaults(listOf(initial, second))

            val stored = requireNotNull(database.agentRoleDao().findById(initial.id))
            assertTrue(stored.isBuiltin)
            assertEquals("first", stored.systemPrompt)
            assertTrue(stored.avatarUri.startsWith(avatarDirectory.toString()))
            assertContentEquals(initial.avatarBytes, Files.readAllBytes(java.nio.file.Path.of(stored.avatarUri)))
            assertEquals(
                listOf("流萤", "三月七"),
                database.agentRoleDao().findAll().map { role -> role.name },
            )

            storage.initializeDefaults(listOf(characterFiles("builtin:流萤", "流萤", "changed")))

            assertEquals("first", database.agentRoleDao().findById(initial.id)?.systemPrompt)
            assertContentEquals(initial.avatarBytes, Files.readAllBytes(java.nio.file.Path.of(stored.avatarUri)))
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
            avatarDirectory.toFile().deleteRecursively()
        }
    }
}

private fun characterFiles(
    id: String,
    name: String,
    prompt: String,
) = CharacterFiles(
    id = id,
    name = name,
    promptBytes = prompt.encodeToByteArray(),
    avatarBytes = byteArrayOf(1, 2, 3),
)
