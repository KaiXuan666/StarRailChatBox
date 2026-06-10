package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IosAudioPlayer : AudioPlayer {
    private var isPlaying = false
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var onCompleteCallback: (() -> Unit)? = null

    override fun play(uri: String, onComplete: () -> Unit) {
        stop()
        onCompleteCallback = onComplete
        isPlaying = true
        playbackJob = scope.launch {
            delay(3000L)
            stop()
        }
    }

    override fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        isPlaying = false
        val cb = onCompleteCallback
        onCompleteCallback = null
        cb?.invoke()
    }

    override fun isPlaying(): Boolean = isPlaying

    override fun release() {
        stop()
    }

    override suspend fun getDuration(uri: String): Int? {
        return 3
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    return remember { IosAudioPlayer() }
}
