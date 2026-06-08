package com.kaixuan.starrailchatbox.platform

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
    val tempFile = File(context.cacheDir, "temp_camera_image_${System.currentTimeMillis()}.jpg")
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, tempFile)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageCaptured(PickedImage(uri.toString()))
        } else {
            onImageCaptured(null)
        }
    }
    
    return { launcher.launch(uri) }
}
