package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class WasmJsAudioRecorder : AudioRecorder {
    private var startTime = 0L

    override fun hasPermission(): Boolean = true

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        onResult(true)
    }

    override fun startRecording() {
        startTime = System.currentTimeMillis()
    }

    override fun stopRecording(cancel: Boolean): RecordResult? {
        val duration = System.currentTimeMillis() - startTime
        if (cancel || duration < 800) return null
        return RecordResult(
            uri = "builtin:voice_sample.m4a",
            durationMs = duration
        )
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    return remember { WasmJsAudioRecorder() }
}
