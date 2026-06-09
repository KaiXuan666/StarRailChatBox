package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

interface RecordAudioPermissionController {
    suspend fun isPermissionGranted(): Boolean

    suspend fun providePermission(): Boolean
}

@Composable
expect fun rememberRecordAudioPermissionController(): RecordAudioPermissionController
