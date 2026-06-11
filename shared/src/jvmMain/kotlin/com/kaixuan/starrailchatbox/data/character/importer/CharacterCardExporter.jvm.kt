package com.kaixuan.starrailchatbox.data.character.importer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual fun getPngEncoder(): PngEncoder = JvmPngEncoder()

actual fun getCharacterCardExporter(): CharacterCardExporter = DefaultCharacterCardExporter()

class JvmPngEncoder : PngEncoder {
    override suspend fun toPng(bytes: ByteArray): ByteArray {
        val image = ImageIO.read(ByteArrayInputStream(bytes))
            ?: throw IllegalArgumentException("Failed to decode image")
        
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        return outputStream.toByteArray()
    }
}
