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

    private val scope = CoroutineScope(Dispatchers.Main)
    private var fallbackJob: Job? = null
    private var isFallbackPlaying = false

    // 增加一个标记位，防止回调被重复触发或置空错乱
    private var isCompleting = false

    override fun play(uri: String, onComplete: () -> Unit) {
        // 确保开始新播放前彻底重置状态
        stop()
        isCompleting = false
        onCompleteCallback = onComplete

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
                    // 播放完成，在主线程直接处理回调，避免协程延迟
                    scope.launch(Dispatchers.Main) {
                        handlePlaybackComplete()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    scope.launch(Dispatchers.Main) {
                        handlePlaybackComplete() // 出错时也当做完成处理，保证业务闭环
                    }
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            startFallbackPlay(3000L)
        }
    }

    private fun startFallbackPlay(durationMs: Long) {
        isFallbackPlaying = true
        fallbackJob = scope.launch {
            delay(durationMs)
            handlePlaybackComplete() // Mock 结束也走统一的完成处理
        }
    }

    // 新增：专门处理播放完成的函数
    private fun handlePlaybackComplete() {
        if (isCompleting) return
        isCompleting = true

        // 1. 先把回调单独取出来
        val cb = onCompleteCallback
        onCompleteCallback = null

        // 2. 释放资源（但不调用会触发状态冲突的重置）
        releaseMediaPlayer()
        cleanTempFile()

        fallbackJob?.cancel()
        fallbackJob = null
        isFallbackPlaying = false

        // 3. 最后安全触发回调
        cb?.invoke()
    }

    override fun stop() {
        // 主动停止时不应该当作“正常播放完成”，但需要清理回调
        fallbackJob?.cancel()
        fallbackJob = null
        isFallbackPlaying = false

        releaseMediaPlayer()
        cleanTempFile()

        // 主动 stop 时直接清空回调，不触发 invoke
        onCompleteCallback = null
        isCompleting = false
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                } catch (_: Exception) {}
                mp.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }

    private fun cleanTempFile() {
        tempFile?.let {
            if (it.exists()) {
                it.delete()
            }
            tempFile = null
        }
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

    override suspend fun getDuration(uri: String): Int? {
        if (uri.startsWith("builtin:")) {
            return 3
        }
        
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            if (uri.startsWith("content://") || uri.startsWith("file://") || uri.startsWith("http://") || uri.startsWith("https://")) {
                retriever.setDataSource(context, Uri.parse(uri))
            } else if (uri.startsWith("data:")) {
                val base64Data = uri.substringAfter("base64,")
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                val temp = File.createTempFile("temp_duration_", ".m4a", context.cacheDir)
                temp.writeBytes(bytes)
                retriever.setDataSource(temp.absolutePath)
                temp.delete()
            } else {
                retriever.setDataSource(uri)
            }
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLong()?.div(1000)?.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current
    return remember(context) { AndroidAudioPlayer(context) }
}
