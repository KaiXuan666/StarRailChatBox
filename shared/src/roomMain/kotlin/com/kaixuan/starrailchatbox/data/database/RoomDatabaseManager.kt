package com.kaixuan.starrailchatbox.data.database

import androidx.room.useWriterConnection
import androidx.sqlite.execSQL
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import no.synth.kmpzip.io.ByteArrayOutputStream
import no.synth.kmpzip.zip.ZipEntry
import no.synth.kmpzip.zip.ZipOutputStream
import no.synth.kmpzip.zip.ZipInputStream
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import com.kaixuan.starrailchatbox.platform.getFormattedDateTime
import okio.Path
import okio.Path.Companion.toPath

class RoomDatabaseManager(
    private val database: StarRailDatabase,
    private val databasePath: String
) : DatabaseManager {

    override suspend fun exportDatabase(directoryPath: PlatformFile, userNickname: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            Napier.d { "RoomDatabaseManager exportDatabase directory=${directoryPath.name} nickname=$userNickname" }

            // 1. 关键：先 checkpoint，把 WAL 内容合并回主 db 文件
            checkpointDatabase()

            // 2. 读取数据库主文件数据
            val dbPath = databasePath.toPath()
            val dbBytes = KmpFileManager.Default.readBytes(dbPath)

            // 3. 递归遍历 KmpFileManager.Default.appDataDir 路径下的所有文件，过滤不需要打包的项
            val appDataDir = KmpFileManager.Default.appDataDir
            val cacheDir = KmpFileManager.Default.cacheDir
            val fileList = mutableListOf<Path>()

            fun collectFiles(dir: Path) {
                val list = try { KmpFileManager.Default.list(dir) } catch (e: Exception) { emptyList() }
                for (path in list) {
                    val metadata = try { KmpFileManager.Default.fileSystem.metadata(path) } catch (e: Exception) { null }
                    if (metadata?.isDirectory == true) {
                        // 排除临时缓存目录
                        if (path != cacheDir) {
                            collectFiles(path)
                        }
                    } else if (metadata?.isRegularFile == true) {
                        val filename = path.name
                        // 排除数据库锁、WAL、SHM、Journal 缓存及写日志文件本身，这些会单独或不予处理
                        val isDbRelated = filename == "starrail_chat_box.db" ||
                                filename == "starrail_chat_box.db-wal" ||
                                filename == "starrail_chat_box.db-shm" ||
                                filename == "starrail_chat_box.db-journal"
                        
                        val isInCacheDir = path.toString().startsWith(cacheDir.toString())

                        if (!isDbRelated && !isInCacheDir) {
                            fileList.add(path)
                        }
                    }
                }
            }

            collectFiles(appDataDir)

            // 4. 利用 kmp-zip 将它们打包为 zip 字节流
            val bos = ByteArrayOutputStream()
            ZipOutputStream(bos).use { zos ->
                // A. 写入数据库主文件
                zos.putNextEntry(ZipEntry("starrail_chat_box.db"))
                zos.write(dbBytes)
                zos.closeEntry()

                // B. 写入私有目录的其他持久化文件
                for (path in fileList) {
                    val relativePath = path.toString().removePrefix(appDataDir.toString())
                        .replace('\\', '/')
                        .trimStart('/')
                    if (relativePath.isNotEmpty()) {
                        val fileBytes = try {
                            KmpFileManager.Default.readBytes(path)
                        } catch (e: Exception) {
                            null
                        }
                        if (fileBytes != null) {
                            zos.putNextEntry(ZipEntry(relativePath))
                            zos.write(fileBytes)
                            zos.closeEntry()
                        }
                    }
                }
            }

            // 5. 写入目标 zip 文件，名称规则为：用户昵称+应用名称+年月日时分.zip
            val cleanNickname = userNickname.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val appName = "崩铁ChatBox"
            val dateTimeStr = getFormattedDateTime()
            val destFileName = "${cleanNickname}-${appName}-${dateTimeStr}.zip"
            val destFile = directoryPath / destFileName

            if (destFile.exists()) {
                destFile.delete(mustExist = false)
            }

            destFile.write(bos.toByteArray())
            Napier.d { "RoomDatabaseManager exportDatabase 成功 destFile=${destFile.name}" }
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

    override suspend fun importDatabase(filePath: PlatformFile): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            Napier.d { "RoomDatabaseManager importDatabase file=${filePath.name}" }

            val bytes = filePath.readBytes()

            // 检查前 4 字节魔数是否为 ZIP 幻数 0x04034B50
            val isZip = if (bytes.size >= 4) {
                bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
            } else {
                false
            }

            if (isZip || filePath.name.lowercase().endsWith(".zip")) {
                Napier.d { "检测为 ZIP 压缩包备份，开始导入并解压还原" }

                // 导入前必须关闭 Room，避免覆盖时数据库仍被占用
                database.close()

                // 前置删除已存在的 -wal、-shm、-journal 文件，避免旧日志污染覆盖后的新 db 导致故障恢复崩溃
                KmpFileManager.Default.delete(("$databasePath-wal").toPath())
                KmpFileManager.Default.delete(("$databasePath-shm").toPath())
                KmpFileManager.Default.delete(("$databasePath-journal").toPath())

                val appDataDir = KmpFileManager.Default.appDataDir
                val cacheDir = KmpFileManager.Default.cacheDir

                ZipInputStream(bytes).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory) {
                            val entryBytes = zip.readBytes()
                            if (name == "starrail_chat_box.db") {
                                // 写入主数据库文件
                                KmpFileManager.Default.writeBytes(databasePath.toPath(), entryBytes)
                            } else {
                                // 还原到私有目录中的相对位置（排除 cacheDir 下的文件）
                                val targetPath = appDataDir / name.toPath()
                                val isInCacheDir = targetPath.toString().startsWith(cacheDir.toString())
                                if (!isInCacheDir) {
                                    KmpFileManager.Default.writeBytes(targetPath, entryBytes)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                // 统一修整数据库中的跨平台绝对路径
                fixDatabasePaths()

                Napier.d { "RoomDatabaseManager ZIP 导入数据成功" }
            } else {
                Napier.d { "检测为普通 DB 文件备份，执行普通覆盖导入" }

                // 导入前必须关闭 Room，避免覆盖时数据库仍被占用
                database.close()

                // 前置删除已存在的 -wal、-shm、-journal 文件，避免旧日志污染覆盖后的新 db 导致故障恢复崩溃
                KmpFileManager.Default.delete(("$databasePath-wal").toPath())
                KmpFileManager.Default.delete(("$databasePath-shm").toPath())
                KmpFileManager.Default.delete(("$databasePath-journal").toPath())

                // 覆盖主数据库文件
                KmpFileManager.Default.writeBytes(databasePath.toPath(), bytes)

                // 统一修整数据库中的跨平台绝对路径
                fixDatabasePaths()

                Napier.d { "RoomDatabaseManager DB 导入数据成功" }
            }
        }
    }

    private fun fixDatabasePaths() {
        try {
            val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
            val connection = driver.open(databasePath)
            try {
                // 强制将修改模式设置为 TRUNCATE，确保所有的路径修改直接在主 db 文件中生效，绝不产生 WAL
                connection.execSQL("PRAGMA journal_mode = TRUNCATE")

                val newBaseDir = KmpFileManager.Default.appDataDir.toString().replace('\\', '/')

                val keyDirs = listOf("character_avatars/", "chat_attachments/", "character_voice_samples/")
                val targets = listOf(
                    "agent_role" to "avatar_uri",
                    "agent_role" to "voice_sample_uri",
                    "message_attachment" to "uri"
                )

                val sqls = mutableListOf<String>()
                for (keyDir in keyDirs) {
                    for ((table, column) in targets) {
                        sqls.add(
                            """
                            UPDATE $table 
                            SET $column = '$newBaseDir' || '/' || SUBSTR(REPLACE($column, '\', '/'), INSTR(REPLACE($column, '\', '/'), '$keyDir'))
                            WHERE REPLACE($column, '\', '/') LIKE '%$keyDir%'
                            """.trimIndent()
                        )
                    }
                }

                for (sql in sqls) {
                    connection.prepare(sql).use { statement ->
                        statement.step()
                    }
                }

                // 强制执行一次 checkpoint 确保彻底落盘
                connection.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")

                Napier.d { "RoomDatabaseManager: 成功执行了跨平台备份数据库路径修正" }
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            Napier.e("RoomDatabaseManager: 执行跨平台备份数据库路径修正失败", e)
        }
    }
}