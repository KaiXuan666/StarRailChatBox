package com.kaixuan.starrailchatbox.data.character.importer

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import io.github.vinceglb.filekit.PlatformFile

interface CharacterCardExporter {
    suspend fun exportToPng(
        character: Character,
        directory: PlatformFile
    ): ApiResult<Unit>
}

expect fun getCharacterCardExporter(): CharacterCardExporter

expect fun getPngEncoder(): PngEncoder

interface PngEncoder {
    suspend fun toPng(bytes: ByteArray): ByteArray
}
