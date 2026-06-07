package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePicker(onImagePicked: (ByteArray?) -> Unit): () -> Unit {
    return {
        onImagePicked(null)
    }
}
