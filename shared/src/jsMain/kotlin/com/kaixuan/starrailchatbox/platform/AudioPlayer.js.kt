package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.w3c.dom.Audio

class JsAudioPlayer : AudioPlayer {
    private var audio: Audio? = null
    private var onCompleteCallback: (() -> Unit)? = null

    override fun play(uri: String, onComplete: () -> Unit) {
        stop()
        onCompleteCallback = onComplete
        try {
            audio = Audio(uri).apply {
                addEventListener("ended", { stop() })
                addEventListener("error", { stop() })
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    override fun stop() {
        audio?.pause()
        audio = null
        val cb = onCompleteCallback
        onCompleteCallback = null
        cb?.invoke()
    }

    override fun isPlaying(): Boolean {
        return audio?.let { !it.paused } ?: false
    }

    override fun release() {
        stop()
    }

    override suspend fun getDuration(uri: String): Int? {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            try {
                val tempAudio = Audio(uri)
                val onLoadedMetadata = {
                    val duration = tempAudio.duration
                    if (duration.isNaN() || duration.isInfinite()) {
                        continuation.resume(null) { }
                    } else {
                        continuation.resume(duration.toInt()) { }
                    }
                }
                val onError = {
                    continuation.resume(null) { }
                }
                tempAudio.addEventListener("loadedmetadata", onLoadedMetadata)
                tempAudio.addEventListener("error", onError)
                
                continuation.invokeOnCancellation {
                    tempAudio.removeEventListener("loadedmetadata", onLoadedMetadata)
                    tempAudio.removeEventListener("error", onError)
                }
            } catch (e: Exception) {
                continuation.resume(null) { }
            }
        }
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    return remember { JsAudioPlayer() }
}
