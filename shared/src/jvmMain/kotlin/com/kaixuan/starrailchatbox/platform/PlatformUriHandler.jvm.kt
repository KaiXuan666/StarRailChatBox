package com.kaixuan.starrailchatbox.platform

import java.awt.Desktop
import java.net.URI
import java.io.File

actual fun openUri(uri: String, mimeType: String?) {
    try {
        if (uri.startsWith("file://") || uri.startsWith("/") || uri.contains(":\\") || uri.contains(":/")) {
            val filePath = if (uri.startsWith("file://")) uri.substring(7) else uri
            val file = File(filePath)
            if (file.exists()) {
                Desktop.getDesktop().open(file)
            }
        } else {
            Desktop.getDesktop().browse(URI(uri))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
