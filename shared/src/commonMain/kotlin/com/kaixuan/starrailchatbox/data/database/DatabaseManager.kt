package com.kaixuan.starrailchatbox.data.database

import io.github.vinceglb.filekit.PlatformFile

interface DatabaseManager {
    suspend fun exportDatabase(directoryPath: PlatformFile): Result<Unit>
    suspend fun importDatabase(filePath: PlatformFile): Result<Unit>
}
