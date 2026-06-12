package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.util.Base64
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

class JvmAudioPlayer : AudioPlayer {
    private var clip: Clip? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var onCompleteCallback: (() -> Unit)? = null

    override fun play(uri: String, onComplete: () -> Unit) {
        stop()
        onCompleteCallback = onComplete

        scope.launch {
            try {
                val audioInputStream = withContext(Dispatchers.IO) {
                    val rawStream = when {
                        uri.startsWith("data:") -> {
                            val base64Data = uri.substringAfter("base64,")
                            val bytes = Base64.getDecoder().decode(base64Data)
                            AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
                        }
                        uri.startsWith("file:") || uri.startsWith("/") || (uri.length > 2 && uri[1] == ':') -> {
                            val file = if (uri.startsWith("file:")) File(URI(uri)) else File(uri)
                            AudioSystem.getAudioInputStream(file)
                        }
                        uri.startsWith("http") -> {
                            AudioSystem.getAudioInputStream(URI(uri).toURL())
                        }
                        else -> null
                    }

                    rawStream?.let { stream ->
                        val baseFormat = stream.format
                        if (baseFormat.encoding == AudioFormat.Encoding.PCM_SIGNED || 
                            baseFormat.encoding == AudioFormat.Encoding.PCM_UNSIGNED) {
                            stream
                        } else {
                            // 尝试转换为 PCM 以支持 MP3 等格式
                            val decodedFormat = AudioFormat(
                                AudioFormat.Encoding.PCM_SIGNED,
                                baseFormat.sampleRate,
                                16,
                                baseFormat.channels,
                                baseFormat.channels * 2,
                                baseFormat.sampleRate,
                                false
                            )
                            AudioSystem.getAudioInputStream(decodedFormat, stream)
                        }
                    }
                }

                if (audioInputStream != null) {
                    val newClip = AudioSystem.getClip()
                    newClip.addLineListener { event ->
                        if (event.type == LineEvent.Type.STOP) {
                            if (newClip.framePosition == newClip.frameLength) {
                                scope.launch {
                                    stop()
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.IO) {
                        newClip.open(audioInputStream)
                    }
                    clip = newClip
                    newClip.start()
                } else {
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                stop()
            }
        }
    }

    override fun stop() {
        clip?.stop()
        clip?.close()
        clip = null
        
        val cb = onCompleteCallback
        onCompleteCallback = null
        cb?.invoke()
    }

    override fun isPlaying(): Boolean = clip?.isRunning ?: false

    override fun release() {
        stop()
    }

    override suspend fun getDuration(uri: String): Int? = withContext(Dispatchers.IO) {
        var tempFileForDuration: File? = null
        try {
            val file = when {
                uri.startsWith("file:") || uri.startsWith("/") || (uri.length > 2 && uri[1] == ':') ->
                    if (uri.startsWith("file:")) File(URI(uri)) else File(uri)
                uri.startsWith("data:") -> {
                    val base64Data = uri.substringAfter("base64,")
                    val bytes = Base64.getDecoder().decode(base64Data)
                    val temp = File.createTempFile("temp_duration_", ".tmp")
                    temp.writeBytes(bytes)
                    tempFileForDuration = temp
                    temp
                }
                else -> null
            }

            if (file != null) {
                val fileFormat = AudioSystem.getAudioFileFormat(file)
                // MP3 SPI usually provides duration in microseconds
                val durationFromProps = fileFormat.properties()["duration"] as? Long
                if (durationFromProps != null) {
                    return@withContext (durationFromProps / 1_000_000).toInt()
                }

                // Fallback for WAV etc.
                val frames = fileFormat.frameLength.toDouble()
                val format = fileFormat.format
                if (frames > 0 && format.frameRate > 0) {
                    return@withContext (frames / format.frameRate).toInt()
                }
            }

            // Fallback for http or others where we can't get FileFormat easily without downloading
            if (uri.startsWith("http")) {
                AudioSystem.getAudioInputStream(URI(uri).toURL()).use { stream ->
                    val format = stream.format
                    val frames = stream.frameLength.toDouble()
                    if (frames > 0 && format.frameRate > 0) {
                        return@withContext (frames / format.frameRate).toInt()
                    }
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            tempFileForDuration?.delete()
        }
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    return remember { JvmAudioPlayer() }
}
