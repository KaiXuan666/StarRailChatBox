package com.kaixuan.starrailchatbox.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StarRailDatabaseMigrationTest {
    @Test
    fun migratesVersionOneWithoutLosingSessions() = runTest {
        val databasePath = Files.createTempFile("starrail-migration", ".db")
        createVersionOneDatabase(databasePath.toString())

        val database = Room.databaseBuilder<StarRailDatabase>(
            name = databasePath.toString(),
            factory = StarRailDatabaseConstructor::initialize,
        )
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build()

        try {
            val session = requireNotNull(database.chatSessionDao().findById("session"))
            assertEquals(true, session.enableSummary)
            assertEquals(20, session.summaryThresholdMessageCount)
            assertEquals(8, session.summaryRetainedMessageCount)
            assertNull(database.chatSummaryDao().findActive("session"))
        } finally {
            database.close()
            Files.deleteIfExists(databasePath)
        }
    }
}

private fun createVersionOneDatabase(path: String) {
    BundledSQLiteDriver().open(path).use { connection ->
        versionOneSchema.forEach(connection::execSQL)
        connection.execSQL(
            """
            INSERT INTO chat_session (
                id, title, agent_id, model_config_id, system_prompt_snapshot,
                custom_system_prompt, max_context_message_count, enable_summary,
                summary_threshold_tokens, active_summary_id, compaction_seq,
                last_message_id, last_message_at, pinned, archived, created_at,
                updated_at, deleted_at
            ) VALUES (
                'session', 'title', NULL, NULL, 'prompt',
                NULL, 2147483647, 0,
                0, NULL, 0,
                NULL, 1000, 0, 0, 1000,
                1000, NULL
            )
            """.trimIndent(),
        )
        connection.execSQL("PRAGMA user_version = 1")
    }
}

private val versionOneSchema = listOf(
    """
    CREATE TABLE IF NOT EXISTS `agent_role` (
        `id` TEXT NOT NULL, `name` TEXT NOT NULL, `avatar_uri` TEXT NOT NULL,
        `description` TEXT NOT NULL, `system_prompt` TEXT NOT NULL,
        `opening_message` TEXT NOT NULL, `temperature` REAL NOT NULL,
        `top_p` REAL NOT NULL, `sort_order` INTEGER NOT NULL,
        `is_builtin` INTEGER NOT NULL, `created_at` INTEGER NOT NULL,
        `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, PRIMARY KEY(`id`)
    )
    """.trimIndent(),
    "CREATE INDEX IF NOT EXISTS `index_agent_role_name` ON `agent_role` (`name`)",
    "CREATE INDEX IF NOT EXISTS `index_agent_role_sort_order_created_at` ON `agent_role` (`sort_order`, `created_at`)",
    "CREATE INDEX IF NOT EXISTS `index_agent_role_deleted_at` ON `agent_role` (`deleted_at`)",
    """
    CREATE TABLE IF NOT EXISTS `model_config` (
        `id` TEXT NOT NULL, `provider` TEXT NOT NULL, `name` TEXT NOT NULL,
        `base_url` TEXT NOT NULL, `api_key_encrypted` TEXT,
        `model_name` TEXT NOT NULL, `context_window` INTEGER NOT NULL,
        `max_output_tokens` INTEGER NOT NULL, `support_vision` INTEGER NOT NULL,
        `support_tool_call` INTEGER NOT NULL, `support_reasoning` INTEGER NOT NULL,
        `temperature` REAL NOT NULL, `top_p` REAL NOT NULL, `enabled` INTEGER NOT NULL,
        `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL,
        `deleted_at` INTEGER, PRIMARY KEY(`id`)
    )
    """.trimIndent(),
    "CREATE INDEX IF NOT EXISTS `index_model_config_provider_model_name` ON `model_config` (`provider`, `model_name`)",
    "CREATE INDEX IF NOT EXISTS `index_model_config_enabled_deleted_at` ON `model_config` (`enabled`, `deleted_at`)",
    """
    CREATE TABLE IF NOT EXISTS `chat_session` (
        `id` TEXT NOT NULL, `title` TEXT NOT NULL, `agent_id` TEXT,
        `model_config_id` TEXT, `system_prompt_snapshot` TEXT NOT NULL,
        `custom_system_prompt` TEXT, `max_context_message_count` INTEGER NOT NULL,
        `enable_summary` INTEGER NOT NULL, `summary_threshold_tokens` INTEGER NOT NULL,
        `active_summary_id` TEXT, `compaction_seq` INTEGER NOT NULL,
        `last_message_id` TEXT, `last_message_at` INTEGER NOT NULL,
        `pinned` INTEGER NOT NULL, `archived` INTEGER NOT NULL,
        `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL,
        `deleted_at` INTEGER, PRIMARY KEY(`id`),
        FOREIGN KEY(`agent_id`) REFERENCES `agent_role`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
        FOREIGN KEY(`model_config_id`) REFERENCES `model_config`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
    )
    """.trimIndent(),
    "CREATE INDEX IF NOT EXISTS `index_chat_session_agent_id` ON `chat_session` (`agent_id`)",
    "CREATE INDEX IF NOT EXISTS `index_chat_session_model_config_id` ON `chat_session` (`model_config_id`)",
    "CREATE INDEX IF NOT EXISTS `index_chat_session_pinned_archived_deleted_at_last_message_at` ON `chat_session` (`pinned`, `archived`, `deleted_at`, `last_message_at`)",
    """
    CREATE TABLE IF NOT EXISTS `chat_message` (
        `id` TEXT NOT NULL, `session_id` TEXT NOT NULL, `parent_message_id` TEXT,
        `seq` INTEGER NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL,
        `reasoning_content` TEXT, `status` TEXT NOT NULL, `error_code` TEXT,
        `error_message` TEXT, `model_config_id` TEXT, `model_name_snapshot` TEXT,
        `prompt_tokens` INTEGER NOT NULL, `completion_tokens` INTEGER NOT NULL,
        `total_tokens` INTEGER NOT NULL, `estimated_tokens` INTEGER NOT NULL,
        `is_context_excluded` INTEGER NOT NULL, `created_at` INTEGER NOT NULL,
        `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, `suggestions_json` TEXT,
        PRIMARY KEY(`id`),
        FOREIGN KEY(`session_id`) REFERENCES `chat_session`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
        FOREIGN KEY(`parent_message_id`) REFERENCES `chat_message`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
        FOREIGN KEY(`model_config_id`) REFERENCES `model_config`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
    )
    """.trimIndent(),
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_chat_message_session_id_seq` ON `chat_message` (`session_id`, `seq`)",
    "CREATE INDEX IF NOT EXISTS `index_chat_message_parent_message_id` ON `chat_message` (`parent_message_id`)",
    "CREATE INDEX IF NOT EXISTS `index_chat_message_model_config_id` ON `chat_message` (`model_config_id`)",
    "CREATE INDEX IF NOT EXISTS `index_chat_message_session_id_deleted_at_seq` ON `chat_message` (`session_id`, `deleted_at`, `seq`)",
)
