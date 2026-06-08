package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberImagePicker(onImagePicked: (PickedImage?) -> Unit): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    return {
        coroutineScope.launch(Dispatchers.IO) {
            val fileChooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
                fileFilter = FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "webp")
            }
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                onImagePicked(PickedImage(fileChooser.selectedFile.absolutePath))
            } else {
                onImagePicked(null)
            }
        }
    }
}
