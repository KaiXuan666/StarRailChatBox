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

actual fun installPackage(path: String) {
    try {
        val file = File(path)
        if (file.exists()) {
            val os = System.getProperty("os.name").lowercase()
            if (os.contains("win")) {
                ProcessBuilder(file.absolutePath).start()
                System.exit(0)
            } else {
                Desktop.getDesktop().open(file)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

