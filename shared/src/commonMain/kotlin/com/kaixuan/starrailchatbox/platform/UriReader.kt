package com.kaixuan.starrailchatbox.platform

/**
 * 跨平台读取 URI/本地文件路径的数据。
 *
 * 在 Android 平台支持读取 content:// 和 file:// 协议；
 * 在 JVM/Desktop 平台支持读取本地文件路径。
 */
expect suspend fun readUriAsBytes(uri: String): ByteArray
