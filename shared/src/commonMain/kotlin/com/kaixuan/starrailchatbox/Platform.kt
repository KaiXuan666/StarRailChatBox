package com.kaixuan.starrailchatbox

enum class PlatformType {
    Android, Ios, Windows, Web
}

interface Platform {
    val name: String
    val type: PlatformType
    val versionCode: Int
    val versionName: String
}

expect fun getPlatform(): Platform