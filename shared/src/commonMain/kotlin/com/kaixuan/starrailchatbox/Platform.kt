package com.kaixuan.starrailchatbox

interface Platform {
    val name: String
    val versionCode: Int
    val versionName: String
}

expect fun getPlatform(): Platform