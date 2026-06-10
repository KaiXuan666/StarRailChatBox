package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

interface AudioPlayer {
    /**
     * 播放指定 URI 的音频。
     * @param uri 音频源的 URI（支持本地路径、Content URI 或 Data URL Base64）
     * @param onComplete 播放结束或发生错误时的回调
     */
    fun play(uri: String, onComplete: () -> Unit)

    /**
     * 停止当前播放。
     */
    fun stop()

    /**
     * 当前是否正在播放。
     */
    fun isPlaying(): Boolean

    /**
     * 释放播放器资源。
     */
    fun release()

    /**
     * 获取音频时长（单位为秒）。
     */
    suspend fun getDuration(uri: String): Int?
}

@Composable
expect fun rememberAudioPlayer(): AudioPlayer
