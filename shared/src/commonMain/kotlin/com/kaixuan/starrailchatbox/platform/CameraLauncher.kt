package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

data class PickedImage(
    val uri: String,
    val name: String? = null,
    val extension: String? = null,
)

@Composable
expect fun rememberCameraLauncher(onImageCaptured: (PickedImage?) -> Unit): () -> Unit
