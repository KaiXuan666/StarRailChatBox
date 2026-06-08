package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.JFileChooser

@Composable
actual fun rememberFilePicker(onFilePicked: (PickedFile?) -> Unit): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    return {
        coroutineScope.launch(Dispatchers.IO) {
            val fileChooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
            }
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                onFilePicked(PickedFile(file.absolutePath, file.name))
            } else {
                onFilePicked(null)
            }
        }
    }
}

@Composable
actual fun rememberCameraLauncher(onImageCaptured: (PickedImage?) -> Unit): () -> Unit {
    // 桌面端暂时不支持拍照，后续可接入本地摄像头库
    return {
        onImageCaptured(null)
    }
}
