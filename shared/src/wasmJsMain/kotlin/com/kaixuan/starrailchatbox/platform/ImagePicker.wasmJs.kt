package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePicker(onImagePicked: (PickedImage?) -> Unit): () -> Unit {
    return {
        onImagePicked(null)
    }
}
