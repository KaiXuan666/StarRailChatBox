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
