package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCameraLauncher(onImageCaptured: (PickedImage?) -> Unit): () -> Unit {
    return {
        onImageCaptured(null)
    }
}
