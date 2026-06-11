package com.kaixuan.starrailchatbox.data.character.importer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

actual fun getPngEncoder(): PngEncoder = AndroidPngEncoder()

actual fun getCharacterCardExporter(): CharacterCardExporter = DefaultCharacterCardExporter()

class AndroidPngEncoder : PngEncoder {
    override suspend fun toPng(bytes: ByteArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("Failed to decode bitmap")
        
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
}
