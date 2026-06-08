package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

data class PickedFile(
    val uri: String,
    val name: String,
)

@Composable
expect fun rememberFilePicker(onFilePicked: (PickedFile?) -> Unit): () -> Unit

@Composable
expect fun rememberCameraLauncher(onImageCaptured: (PickedImage?) -> Unit): () -> Unit
