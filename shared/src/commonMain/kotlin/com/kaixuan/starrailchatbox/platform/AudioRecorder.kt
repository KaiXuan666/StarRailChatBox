package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

data class RecordResult(
    val uri: String,
    val durationMs: Long
)

interface AudioRecorder {
    /**
     * 检测是否已被授予麦克风权限。
     */
    fun hasPermission(): Boolean

    /**
     * 请求麦克风权限。
     */
    fun requestPermission(onResult: (Boolean) -> Unit)

    /**
     * 开始录制音频。
     */
    fun startRecording()

    /**
     * 停止或取消录制音频。
     * @param cancel 是否是取消（若是取消，则内部需清理已录制的文件，并返回 null）
     * @return 录音结果（包含文件路径和时长），如果取消或出错则返回 null
     */
    fun stopRecording(cancel: Boolean): RecordResult?
}

/**
 * 跨平台记住 AudioRecorder 实例的 Composable 工具。
 */
@Composable
expect fun rememberAudioRecorder(): AudioRecorder
