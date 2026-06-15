package com.kaixuan.starrailchatbox.platform

/**
 * 跨平台打开 URI。
 * 在 Android 平台，如果 URI 是 file:// 协议，会使用 FileProvider 转换为 content:// 协议以避免 FileUriExposedException。
 */
expect fun openUri(uri: String, mimeType: String? = null)

/**
 * 跨平台安装更新包。
 * 在 Android 平台调起系统安装程序，在 Windows (JVM) 平台启动 EXE 并退出当前进程。
 */
expect fun installPackage(path: String)

