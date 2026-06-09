package com.kaixuan.starrailchatbox.data.database

import io.github.vinceglb.filekit.PlatformFile

class InMemoryDatabaseManager : DatabaseManager {
    override suspend fun exportDatabase(directoryPath: PlatformFile): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun importDatabase(filePath: PlatformFile): Result<Unit> {
        return Result.success(Unit)
    }
}
