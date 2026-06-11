package com.kaixuan.starrailchatbox.data.character.importer

import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString

object PngMetadataCodec {
    private val PNG_SIGNATURE = "89504e470d0a1a0a".decodeHex()

    fun readChunks(bytes: ByteArray): Map<String, String> {
        val chunks = mutableMapOf<String, String>()
        val buffer = Buffer().write(bytes)

        val signature = buffer.readByteString(8)
        if (signature != PNG_SIGNATURE) {
            return chunks
        }

        while (!buffer.exhausted()) {
            val length = buffer.readInt()
            val type = buffer.readUtf8(4)
            if (type == "tEXt") {
                val data = buffer.readByteString(length.toLong())
                val dataStr = data.utf8()
                val nullPos = dataStr.indexOf('\u0000')
                if (nullPos != -1) {
                    val key = dataStr.substring(0, nullPos)
                    val value = dataStr.substring(nullPos + 1)
                    chunks[key] = value
                }
            } else if (type == "iTXt") {
                val data = buffer.readByteString(length.toLong())
                val dataStr = data.utf8()
                val parts = dataStr.split('\u0000')
                if (parts.size >= 4) {
                    val key = parts[0]
                    val headerOffset = key.length + 1 + 2 + parts[1].length + 1 + parts[2].length + 1
                    if (dataStr.length > headerOffset) {
                        chunks[key] = dataStr.substring(headerOffset)
                    }
                }
            } else {
                buffer.skip(length.toLong())
            }
            buffer.skip(4) // CRC
        }
        return chunks
    }

    fun writeTextChunk(pngBytes: ByteArray, keyword: String, text: String): ByteArray {
        val input = Buffer().write(pngBytes)
        val output = Buffer()

        val signature = input.readByteString(8)
        if (signature != PNG_SIGNATURE) {
            throw IllegalArgumentException("Invalid PNG signature")
        }
        output.write(signature)

        val chunkData = Buffer()
        chunkData.writeUtf8(keyword)
        chunkData.writeByte(0)
        chunkData.writeUtf8(text)
        val tEXtBytes = chunkData.readByteString()

        while (!input.exhausted()) {
            val length = input.readInt()
            val type = input.readUtf8(4)
            
            if (type == "IEND") {
                // Insert our tEXt chunk before IEND
                output.writeInt(tEXtBytes.size)
                output.writeUtf8("tEXt")
                output.write(tEXtBytes)
                output.writeInt(calculateCrc("tEXt", tEXtBytes))
                
                // Then write IEND
                output.writeInt(length)
                output.writeUtf8(type)
                input.skip(length.toLong())
                output.writeInt(input.readInt()) // IEND CRC
                break
            } else {
                output.writeInt(length)
                output.writeUtf8(type)
                val data = input.readByteString(length.toLong())
                output.write(data)
                output.writeInt(input.readInt()) // CRC
            }
        }

        return output.readByteArray()
    }

    private fun calculateCrc(type: String, data: ByteString): Int {
        val crc = Crc32()
        crc.update(type.encodeToByteArray())
        crc.update(data.toByteArray())
        return crc.getValue()
    }

    private class Crc32 {
        private var crc = -1

        fun update(data: ByteArray) {
            for (b in data) {
                val byte = b.toInt() and 0xff
                crc = table[(crc xor byte) and 0xff] xor (crc ushr 8)
            }
        }

        fun getValue(): Int = crc.inv()

        companion object {
            private val table = IntArray(256) {
                var c = it
                for (k in 0..7) {
                    c = if (c and 1 != 0) 0xEDB88320.toInt() xor (c ushr 1) else c ushr 1
                }
                c
            }
        }
    }
}
