package com.kaixuan.starrailchatbox.data.database

import androidx.room.useWriterConnection
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class RoomDatabaseManager(
    private val database: StarRailDatabase,
    private val databasePath: String
) : DatabaseManager {

    companion object {
        private const val BACKUP_FILE_NAME = "starrail_chat_box_backup.db"
    }

    override suspend fun exportDatabase(directory: PlatformFile): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            Napier.d { "RoomDatabaseManager exportDatabase directory=${directory.name}" }

            // App 私有目录下的真实数据库文件
            val dbFile = PlatformFile(databasePath)

            // SAF / 普通目录都通过 PlatformFile 子文件方式处理
            val destFile = directory / BACKUP_FILE_NAME

            // 关键：先 checkpoint，把 WAL 内容合并回主 db 文件
            checkpointDatabase()

            // 可选：如果目标已存在，先删除，避免部分平台不允许直接覆盖
            if (destFile.exists()) {
                destFile.delete(mustExist = false)
            }

            // FileKit 推荐用 PlatformFile -> PlatformFile 的复制方式，
            // 对 Android content:// 这类 provider-backed file 更稳
            destFile.write(dbFile)
            // 或者：dbFile.copyTo(destFile)

            Napier.d { "RoomDatabaseManager 导出数据成功 destFile=${destFile.name}" }
        }
    }

    private suspend fun checkpointDatabase() {
        database.useWriterConnection<Unit> { connection ->
            connection.usePrepared("PRAGMA wal_checkpoint(TRUNCATE)") { statement ->
                if (statement.step()) {
                    val busy = statement.getLong(0)
                    val log = statement.getLong(1)
                    val checkpointed = statement.getLong(2)

                    if (busy != 0L) {
                        throw IllegalStateException(
                            "Database checkpoint failed: busy=$busy, log=$log, checkpointed=$checkpointed"
                        )
                    }
                }
            }
        }
    }

    override suspend fun importDatabase(file: PlatformFile): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            Napier.d { "RoomDatabaseManager importDatabase file=${file.name}" }

            // 导入前必须关闭 Room，避免覆盖时数据库仍被占用
            database.close()

            val targetDbFile = PlatformFile(databasePath)

            // 覆盖主数据库文件
            targetDbFile.write(file)
            // 或者：file.copyTo(targetDbFile)

            // 删除旧 WAL/SHM，避免旧日志污染新导入的 db
            PlatformFile("$databasePath-wal").delete(mustExist = false)
            PlatformFile("$databasePath-shm").delete(mustExist = false)

            Napier.d { "RoomDatabaseManager 导入数据成功" }
        }
    }
}