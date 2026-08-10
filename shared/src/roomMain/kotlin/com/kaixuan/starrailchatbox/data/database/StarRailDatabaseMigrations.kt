package com.kaixuan.starrailchatbox.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `chat_summary` (
            `id` TEXT NOT NULL,
            `session_id` TEXT NOT NULL,
            `from_seq` INTEGER NOT NULL,
            `to_seq` INTEGER NOT NULL,
            `content` TEXT NOT NULL,
            `source_message_count` INTEGER NOT NULL,
            `model_config_id` TEXT,
            `model_name_snapshot` TEXT,
            `prompt_tokens` INTEGER NOT NULL,
            `completion_tokens` INTEGER NOT NULL,
            `total_tokens` INTEGER NOT NULL,
            `created_at` INTEGER NOT NULL,
            PRIMARY KEY(`id`),
            FOREIGN KEY(`session_id`) REFERENCES `chat_session`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`model_config_id`) REFERENCES `model_config`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent(),
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_chat_summary_session_id_to_seq` " +
                "ON `chat_summary` (`session_id`, `to_seq`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chat_summary_model_config_id` " +
                "ON `chat_summary` (`model_config_id`)",
        )
        connection.execSQL(
            "ALTER TABLE `chat_session` ADD COLUMN `summary_threshold_message_count` " +
                "INTEGER NOT NULL DEFAULT 20",
        )
        connection.execSQL(
            "ALTER TABLE `chat_session` ADD COLUMN `summary_retained_message_count` " +
                "INTEGER NOT NULL DEFAULT 8",
        )
        connection.execSQL("UPDATE `chat_session` SET `enable_summary` = 1")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `message_attachment` (
                `id` TEXT NOT NULL,
                `message_id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `size` INTEGER NOT NULL,
                `mime_type` TEXT NOT NULL,
                `uri` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`message_id`) REFERENCES `chat_message`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_message_attachment_message_id` ON `message_attachment` (`message_id`)"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `agent_role` ADD COLUMN `voice_sample_uri` TEXT DEFAULT NULL"
        )
        connection.execSQL(
            "ALTER TABLE `message_attachment` ADD COLUMN `duration_ms` INTEGER DEFAULT NULL"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `agent_role` ADD COLUMN `author` TEXT NOT NULL DEFAULT ''"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `chat_session` ADD COLUMN `parent_session_id` TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE `chat_session` ADD COLUMN `branched_from_message_id` TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE `chat_session` ADD COLUMN `branch_depth` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_session_segment` (
                `owner_session_id` TEXT NOT NULL,
                `segment_index` INTEGER NOT NULL,
                `source_session_id` TEXT NOT NULL,
                `from_seq` INTEGER NOT NULL,
                `to_seq` INTEGER,
                PRIMARY KEY(`owner_session_id`, `segment_index`),
                FOREIGN KEY(`owner_session_id`) REFERENCES `chat_session`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`source_session_id`) REFERENCES `chat_session`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chat_session_segment_owner_session_id_segment_index` " +
                "ON `chat_session_segment` (`owner_session_id`, `segment_index`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chat_session_segment_source_session_id` " +
                "ON `chat_session_segment` (`source_session_id`)",
        )
        connection.execSQL(
            """
            INSERT INTO `chat_session_segment` (
                `owner_session_id`, `segment_index`, `source_session_id`, `from_seq`, `to_seq`
            )
            SELECT `id`, 0, `id`, 1, NULL FROM `chat_session`
            """.trimIndent(),
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_session_hidden_message` (
                `owner_session_id` TEXT NOT NULL,
                `message_id` TEXT NOT NULL,
                `hidden_at` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                PRIMARY KEY(`owner_session_id`, `message_id`),
                FOREIGN KEY(`owner_session_id`) REFERENCES `chat_session`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`message_id`) REFERENCES `chat_message`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chat_session_hidden_message_owner_session_id_message_id` " +
                "ON `chat_session_hidden_message` (`owner_session_id`, `message_id`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chat_session_hidden_message_message_id` " +
                "ON `chat_session_hidden_message` (`message_id`)",
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `local_model` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `file_path` TEXT NOT NULL,
                `size_bytes` INTEGER NOT NULL,
                `sha256` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `source_url` TEXT,
                `license` TEXT NOT NULL,
                `context_window` INTEGER NOT NULL,
                `max_output_tokens` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}
