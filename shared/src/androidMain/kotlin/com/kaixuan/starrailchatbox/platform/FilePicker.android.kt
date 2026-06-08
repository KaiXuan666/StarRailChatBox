package com.kaixuan.starrailchatbox.platform

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberFilePicker(onFilePicked: (PickedFile?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            onFilePicked(null)
            return@rememberLauncherForActivityResult
        }
        
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        
        onFilePicked(PickedFile(uri.toString(), name))
    }
    return { launcher.launch("*/*") }
}

@Composable
actual fun rememberCameraLauncher(onImageCaptured: (PickedImage?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    
    // 使用 remember 避免在重组时重复创建文件和生成 URI
    // 在预览模式下跳过 FileProvider 调用，因为它依赖于 Manifest 中的配置，预览环境可能无法识别
    val uri = remember(isPreview) {
        if (isPreview) {
            null
        } else {
            val tempFile = File(context.cacheDir, "temp_camera_image_${System.currentTimeMillis()}.jpg")
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, tempFile)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageCaptured(PickedImage(uri?.toString() ?: ""))
        } else {
            onImageCaptured(null)
        }
    }
    
    return { 
        uri?.let { launcher.launch(it) }
    }
}
