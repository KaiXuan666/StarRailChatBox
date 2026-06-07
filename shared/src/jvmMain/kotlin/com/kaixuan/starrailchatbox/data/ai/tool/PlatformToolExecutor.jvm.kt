package com.kaixuan.starrailchatbox.data.ai.tool

actual fun createPlatformToolExecutor(): PlatformToolExecutor {
    return UnsupportedPlatformToolExecutor
}
