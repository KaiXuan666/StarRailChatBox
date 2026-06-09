package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberRecordAudioPermissionController(): RecordAudioPermissionController {
    return remember {
        object : RecordAudioPermissionController {
            override suspend fun isPermissionGranted(): Boolean = true

            override suspend fun providePermission(): Boolean = true
        }
    }
}
