package com.kaixuan.starrailchatbox.data.character.importer

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

actual fun getPngEncoder(): PngEncoder = IosPngEncoder()

actual fun getCharacterCardExporter(): CharacterCardExporter = DefaultCharacterCardExporter()

class IosPngEncoder : PngEncoder {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun toPng(bytes: ByteArray): ByteArray {
        val nsData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        val image = UIImage.imageWithData(nsData)
            ?: throw IllegalArgumentException("Failed to decode image")
        
        val pngData = UIImagePNGRepresentation(image)
            ?: throw IllegalArgumentException("Failed to encode to PNG")
        
        val result = ByteArray(pngData.length.toInt())
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), pngData.bytes, pngData.length)
        }
        return result
    }
}
