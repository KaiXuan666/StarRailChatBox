package com.kaixuan.starrailchatbox.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePicker(onImagePicked: (PickedImage?) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        onImagePicked(uri?.let { PickedImage(it.toString()) })
    }
    return { launcher.launch("image/*") }
}
