package com.kaixuan.starrailchatbox.data.character.importer

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character

actual fun getPngEncoder(): PngEncoder = JsPngEncoder()

actual fun getCharacterCardExporter(): CharacterCardExporter = object : CharacterCardExporter {
    override suspend fun exportToPng(
        character: Character,
        directory: io.github.vinceglb.filekit.PlatformFile
    ): ApiResult<Unit> {
        return ApiResult.UnexpectedError("Export is not supported on Web")
    }
}

class JsPngEncoder : PngEncoder {
    override suspend fun toPng(bytes: ByteArray): ByteArray {
        // Simple passthrough for JS/WasmJS for now as per requirements
        return bytes
    }
}
