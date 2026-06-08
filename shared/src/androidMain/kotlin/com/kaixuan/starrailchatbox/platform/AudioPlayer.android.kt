package com.kaixuan.starrailchatbox.platform

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var onCompleteCallback: (() -> Unit)? = null
    private var tempFile: File? = null
    
    // Fallback Mock 播放机制
    private val scope = CoroutineScope(Dispatchers.Main)
    private var fallbackJob: Job? = null
    private var isFallbackPlaying = false

    override fun play(uri: String, onComplete: () -> Unit) {
        stop()
        onCompleteCallback = onComplete
        
        // 如果是 mock 的内置音频，直接触发模拟播放
        if (uri.startsWith("builtin:")) {
            startFallbackPlay(3000L)
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                if (uri.startsWith("content://") || uri.startsWith("file://") || uri.startsWith("http://") || uri.startsWith("https://")) {
                    setDataSource(context, Uri.parse(uri))
                } else if (uri.startsWith("data:")) {
                    val base64Data = uri.substringAfter("base64,")
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    val temp = File.createTempFile("temp_voice_", ".m4a", context.cacheDir)
                    temp.writeBytes(bytes)
                    tempFile = temp
                    setDataSource(temp.absolutePath)
                } else {
                    setDataSource(uri)
                }
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 真实播放失败时，为了保证业务闭环，采用 mock 播放
            startFallbackPlay(3000L)
        }
    }

    private fun startFallbackPlay(durationMs: Long) {
        isFallbackPlaying = true
        fallbackJob = scope.launch {
            delay(durationMs)
            stop()
        }
    }

    override fun stop() {
        fallbackJob?.cancel()
        fallbackJob = null
        isFallbackPlaying = false

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
        
        tempFile?.let {
            if (it.exists()) {
                it.delete()
            }
            tempFile = null
        }

        val cb = onCompleteCallback
        onCompleteCallback = null
        cb?.invoke()
    }

    override fun isPlaying(): Boolean {
        return isFallbackPlaying || (try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            false
        })
    }

    override fun release() {
        stop()
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current
    return remember(context) { AndroidAudioPlayer(context) }
}
