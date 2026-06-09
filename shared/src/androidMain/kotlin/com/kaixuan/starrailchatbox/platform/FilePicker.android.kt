package com.kaixuan.starrailchatbox.platform

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    
    // 使用 remember 保存当前的 URI，以便在回调中访问
    var currentUri by remember { mutableStateOf<Uri?>(null) }
    var currentName by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageCaptured(PickedImage(currentUri?.toString() ?: "", currentName))
        } else {
            onImageCaptured(null)
        }
    }
    
    return { 
        if (isPreview) {
            onImageCaptured(null)
        } else {
            val name = "camera_${System.currentTimeMillis()}.jpg"
            val tempFile = File(context.cacheDir, name)
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            currentUri = uri
            currentName = name
            launcher.launch(uri)
        }
    }
}
