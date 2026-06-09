package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.time.TimeSource
import kotlin.time.TimeMark

class IosAudioRecorder : AudioRecorder {
    private var startTimeMark: TimeMark? = null

    override fun startRecording() {
        startTimeMark = TimeSource.Monotonic.markNow()
    }

    override fun stopRecording(cancel: Boolean): RecordResult? {
        val start = startTimeMark ?: return null
        val duration = start.elapsedNow().inWholeMilliseconds
        if (cancel || duration < 800) return null
        return RecordResult(
            uri = "builtin:voice_sample.m4a",
            durationMs = duration
        )
    }
}

@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    return remember { IosAudioRecorder() }
}
