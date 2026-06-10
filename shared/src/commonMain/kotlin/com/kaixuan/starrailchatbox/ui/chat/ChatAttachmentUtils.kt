package com.kaixuan.starrailchatbox.ui.chat

import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind

fun getMimeTypeFromName(name: String, isImage: Boolean = false, isVoice: Boolean = false): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when {
        isImage -> when (ext) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        isVoice -> "audio/m4a"
        else -> when (ext) {
            "txt", "kt", "java", "py", "js", "ts", "md" -> "text/plain"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "json" -> "application/json"
            "pdf" -> "application/pdf"
            "xml" -> "application/xml"
            "mp3" -> "audio/mpeg"
            "db" -> "application/x-sqlite3"
            "zip" -> "application/zip"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "rar" -> "application/vnd.rar"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}

fun getIconForMimeType(mimeType: String): StarRailIconKind {
    return when {
        mimeType.startsWith("image/") -> StarRailIconKind.GALLERY
        mimeType.startsWith("audio/") -> StarRailIconKind.MICROPHONE
        mimeType.startsWith("video/") -> StarRailIconKind.CAMERA
        mimeType.startsWith("text/plain") -> StarRailIconKind.EDIT
        mimeType.startsWith("text/html") || mimeType.startsWith("text/css") -> StarRailIconKind.GLOBE
        mimeType.contains("javascript") || mimeType.contains("json") || mimeType.contains("xml") -> StarRailIconKind.KEYBOARD
        mimeType.contains("sqlite") || mimeType.contains("database") -> StarRailIconKind.DATABASE
        mimeType.contains("zip") || mimeType.contains("archive") || mimeType.contains("tar") || mimeType.contains("7z") || mimeType.contains("rar") -> StarRailIconKind.CUBE
        mimeType.contains("pdf") -> StarRailIconKind.INFO
        else -> StarRailIconKind.FILE
    }
}
